package com.pos.controller;

import com.pos.model.ItemDto;
import com.pos.model.Order;
import com.pos.model.OrderItem;
import com.pos.model.OrderRequest;
import com.pos.model.OrderStatus;
import com.pos.model.PaymentMethod;
import com.pos.repository.OrderRepository;
import com.pos.repository.ProductRepository;
import com.pos.service.HardwareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AJAX checkout endpoint of the MVC stack (the "C" in MVC).
 * <p>
 * Receives the cart as an {@link OrderRequest}, converts it into model
 * objects, persists an {@link Order} (line items serialise into
 * {@code Order.itemsJson}), triggers receipt printing and the cash drawer
 * through the {@link HardwareService}, and returns the totals the UI needs
 * to close the sale.
 * <p>
 * Hardware faults are logged and swallowed — a broken printer can never
 * lose a completed sale.
 */
@RestController
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    /** Flat 10% sales tax; override per site by changing this constant. */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TransactionTemplate tx;
    /**
     * Optional: {@link HardwareService} is a {@code @ConditionalOnProperty} bean
     * (app.hardware.enabled). When hardware is disabled the provider resolves
     * to null and checkout proceeds without printing / drawer pulses.
     */
    private final ObjectProvider<HardwareService> hardwareServiceProvider;

    /** ALL = open the drawer on every finalized bill; CASH = cash sales only. */
    private final String drawerOpenOn;

    public CheckoutController(OrderRepository orderRepository,
                              ProductRepository productRepository,
                              PlatformTransactionManager txManager,
                              ObjectProvider<HardwareService> hardwareServiceProvider,
                              @Value("${app.hardware.drawer-open-on:ALL}") String drawerOpenOn) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.tx = new TransactionTemplate(txManager);
        this.hardwareServiceProvider = hardwareServiceProvider;
        this.drawerOpenOn = drawerOpenOn == null ? "ALL" : drawerOpenOn.trim().toUpperCase();
    }

    @PostMapping("/api/checkout")
    public ResponseEntity<Map<String, Object>> completeOrder(@RequestBody OrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Cart is empty");
            return ResponseEntity.badRequest().body(error);
        }

        // ---- Build the model line items. ----
        List<OrderItem> items = request.getItems().stream()
                .map(this::toOrderItem)
                .toList();

        // ---- Server-side totals: never trust client arithmetic. ----
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getLineTotal)
                .filter(i -> i != null && i.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, java.math.RoundingMode.HALF_UP);

        // ---- Persist. ----
        Order order = Order.builder()
                .terminal(request.getTerminal() != null ? request.getTerminal() : "POS-1")
                .customerName(request.getCustomerName())
                .status(OrderStatus.COMPLETED)
                .paymentMethod(request.getPaymentMethod())
                .cashTendered(request.getPaymentMethod() == PaymentMethod.CASH
                        ? request.getCashTendered()
                        : null)
                .cardLastFour(request.getPaymentMethod() == PaymentMethod.CARD
                        ? request.getCardLastFour()
                        : null)
                .subtotal(subtotal)
                .tax(tax)
                .total(total)
                .items(items)
                .createdAt(Instant.now())
                .build();

        // Order + stock deduction commit together (single SQLite transaction).
        Order saved = tx.execute(status -> {
            Order o = orderRepository.save(order);
            deductStock(items);
            return o;
        });

        