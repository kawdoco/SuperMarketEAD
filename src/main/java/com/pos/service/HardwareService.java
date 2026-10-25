package com.pos.service;

import com.pos.model.Order;
import com.pos.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Hardware driver for the legacy POS terminal (FEC MP-3435 class machines).
 * <p>
 * Sends raw <a href="https://en.wikipedia.org/wiki/ESC/P">ESC/POS</a> byte
 * streams to the system default thermal receipt printer through
 * {@code javax.print}. The RJ11 cash drawer is daisy-chained off the
 * printer's drawer-kick connector, so opening the drawer is done with the
 * ESC/POS drawer-kick pulse command ({@code ESC p}).
 * <p>
 * <h2>ESC/POS commands used</h2>
 * <pre>
 *   ESC @        1B 40              initialize printer (clears buffers)
 *   ESC a n      1B 61 n            justification (0=left, 1=center)
 *   ESC E n      1B 45 n            bold on/off
 *   ESC d n      1B 64 n            feed n lines
 *   GS V 0       1D 56 00           full paper cut
 *   ESC p m t1 t2  1B 70 00 19 FA   drawer-kick pulse, pin 2, 25 ms on
 * </pre>
 * <p>
 * <h2>Fault tolerance</h2>
 * Every method degrades gracefully: if no printer is present or the spooler
 * rejects the job the failure is logged and the sale proceeds regardless.
 * Hardware can never block or roll back a completed checkout.
 */
@Service
@ConditionalOnProperty(name = "app.hardware.enabled", havingValue = "true", matchIfMissing = true)
public class HardwareService {

    private static final Logger log = LoggerFactory.getLogger(HardwareService.class);

    /** Byte-array document flavor — representation class "[B" (byte[]). */
    private static final DocFlavor RAW_BYTES = new DocFlavor("application/octet-stream", "[B");

    // ----- ESC/POS command bytes -----
    private static final byte[] ESC_INIT   = {0x1B, 0x40};              // ESC @
    private static final byte[] ALIGN_LEFT = {0x1B, 0x61, 0x00};        // ESC a 0
    private static final byte[] ALIGN_CENT = {0x1B, 0x61, 0x01};        // ESC a 1
    private static final byte[] BOLD_ON    = {0x1B, 0x45, 0x01};        // ESC E 1
    private static final byte[] BOLD_OFF   = {0x1B, 0x45, 0x00};        // ESC E 0
    private static final byte[] FEED_3     = {0x1B, 0x64, 0x03};        // ESC d 3
    private static final byte[] CUT_FULL   = {0x1D, 0x56, 0x00};        // GS V 0
    private static final byte[] DRAWER_PULSE = {0x1B, 0x70, 0x00, 0x19, (byte) 0xFA}; // ESC p 0 25 250

    /** Printable width in characters for an 80 mm roll (safe default). */
    private static final int WIDTH = 42;

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    /**
     * Print the receipt for a completed order.
     * <p>
     * One spool job carries the whole receipt: init + header + line items +
     * totals + footer + feed + paper cut, so the printer never splits a
     * ticket across queues.
     */
    public void printReceipt(Order order) {
        if (order == null) {
            log.warn("printReceipt: order is null, nothing to print");
            return;
        }

        byte[] payload = buildReceiptBytes(order);

        try {
            PrintService printer = findReceiptPrinter();
            if (printer == null) {
                log.warn("No receipt printer found — skipping print for order #{}", order.getId());
                return;
            }

            DocPrintJob job = printer.createPrintJob();
            Doc doc = new SimpleDoc(payload, RAW_BYTES, null);
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            job.print(doc, attrs);

            log.info("Receipt for order #{} sent to printer '{}'", order.getId(), printer.getName());
        } catch (PrintException e) {
            // Never fail a sale because the printer is offline / out of paper.
            log.warn("Failed to print receipt for order #{}", order.getId(), e);
        }
    }

    /**
     * Trigger the RJ11 cash drawer via the printer's drawer-kick connector.
     * Sends {@code ESC p 0 25 250}: pin 2 driven low for 25 ms — the standard
     * pulse every RJ11 drawer latch expects.
     */
    public boolean openCashDrawer() {
        try {
            PrintService printer = findReceiptPrinter();
            if (printer == null) {
                log.warn("No receipt printer found — cannot pulse cash drawer");
                return false;
            }

            byte[] payload = concat(ESC_INIT, DRAWER_PULSE);
            DocPrintJob job = printer.createPrintJob();
            job.print(new SimpleDoc(payload, RAW_BYTES, null), new HashPrintRequestAttributeSet());

            log.info("Cash drawer pulse sent via '{}'", printer.getName());
            return true;
        } catch (PrintException | RuntimeException e) {
            log.warn("Failed to open cash drawer", e);
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Receipt layout
    // ---------------------------------------------------------------------

    private byte[] buildReceiptBytes(Order order) {
        StringBuilder sb = new StringBuilder();

        sb.append(bytesToString(ALIGN_CENT)).append(bytesToString(BOLD_ON)).append('\n');
        sb.append(center("PURCHASE RECEIPT")).append('\n');
        sb.append(bytesToString(BOLD_OFF)).append('\n');
        sb.append(center("Terminal: " + safe(order.getTerminal()))).append('\n');
        sb.append(center(TS_FMT.format(order.getCreatedAt()))).append('\n');
        sb.append(line('-')).append('\n');

        if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
            sb.append(padRight("Customer: " + order.getCustomerName())).append('\n');
            sb.append(line('-')).append('\n');
        }

        sb.append(bytesToString(ALIGN_LEFT)).append('\n');

        // Line items: "name.............. qty x price = total"
        for (OrderItem item : order.getItems()) {
            String name = truncate(safe(item.getName()), WIDTH - 14);
            String qtyPart = " x" + item.getQuantity();
            String totalPart = fmt(item.getLineTotal());
            sb.append(padRight(name)).append(rightAlign(qtyPart + " " + totalPart, 14)).append('\n');
        }

        sb.append(line('-')).append('\n');
        sb.append(twoCol("Subtotal", fmt(order.getSubtotal()))).append('\n');
        sb.append(twoCol("Tax", fmt(order.getTax()))).append('\n');
        sb.append(bytesToString(BOLD_ON));
        sb.append(twoCol("TOTAL", fmt(order.getTotal()))).append('\n');
        sb.append(bytesToString(BOLD_OFF));
        sb.append(line('-')).append('\n');

        sb.append(twoCol("Payment", order.getPaymentMethod().name())).append('\n');
        if (order.getCashTendered() != null) {
            sb.append(twoCol("Tendered", fmt(order.getCashTendered()))).append('\n');
        }
        if (order.getCardLastFour() != null) {
            sb.append(twoCol("Card ****", order.getCardLastFour())).append('\n');
        }
        if (order.getId() != null) {
            sb.append(twoCol("Order #", String.valueOf(order.getId()))).append('\n');
        }

        sb.append('\n');
        sb.append(center("Thank you!")).append('\n');
        sb.append(center("*** Customer copy ***")).append('\n');

        // init already emitted above; append cut + feed tail
        sb.append(bytesToString(FEED_3)).append(bytesToString(CUT_FULL));

        // ESC_INIT + printable text
        return concat(ESC_INIT, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------------
    // Printer discovery
    // ---------------------------------------------------------------------

    private PrintService findReceiptPrinter() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(RAW_BYTES, null);
        if (services == null || services.length == 0) {
            // Fall back to the default service even if it doesn't advertise
            // the raw flavor — Windows drivers usually still accept byte docs.
            PrintService def = PrintServiceLookup.lookupDefaultPrintService();
            return def;
        }

        // Prefer a printer that looks like a thermal POS device.
        return Arrays.stream(services)
                .filter(s -> s.getName() != null)
                .filter(s -> {
                    String n = s.getName().toLowerCase();
                    return n.contains("thermal") || n.contains("pos") || n.contains("receipt")
                            || n.contains("escpos") || n.contains("tm-") || n.contains("rp-");
                })
                .findFirst()
                .orElse(services[0]);
    }

    // ---------------------------------------------------------------------
    // Text helpers
    // ---------------------------------------------------------------------

    private String twoCol(String left, String right) {
        String l = truncate(safe(left), WIDTH - 12);
        return padRight(l) + rightAlign(right, 12);
    }

    private String padRight(String s) {
        if (s.length() >= WIDTH) {
            return s;
        }
        return s + " ".repeat(WIDTH - s.length());
    }

    private String rightAlign(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return " ".repeat(width - s.length()) + s;
    }

    private String center(String s) {
        if (s.length() >= WIDTH) {
            return s;
        }
        int left = (WIDTH - s.length()) / 2;
        return " ".repeat(left) + s;
    }

    private String line(char ch) {
        return String.valueOf(ch).repeat(WIDTH);
    }

    private String truncate(String s, int limit) {
        if (s == null || s.length() <= limit) {
            return s;
        }
        return s.substring(0, Math.max(0, limit - 1)) + "…";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String fmt(BigDecimal v) {
        return v == null ? "0.00" : String.format("%.2f", v);
    }

    private String bytesToString(byte[] bytes) {
        // Command bytes ride along in the text buffer; they are opaque to the
        // printer and never rendered. Re-encode as ISO-8859-1 to preserve them.
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private byte[] concat(byte[]... arrays) {
        int len = Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, pos, a.length);
            pos += a.length;
        }
        return out;
    }
}
