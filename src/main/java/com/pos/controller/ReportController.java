package com.pos.controller;

import com.pos.model.Order;
import com.pos.model.OrderStatus;
import com.pos.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Transaction history and daily sales reports (the "C" in MVC).
 * <p>
 * All aggregates run as SQL over the local SQLite file, so even months of
 * history stay fast on low-RAM terminal hardware. The view gets a single
 * {@link LocalDate} window; everything is computed between its start and
 * end instants in the machine's local zone.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final OrderRepository orderRepository;

    public ReportController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Daily dashboard: totals for the selected day plus the recent order list.
     *
     * @param date optional ISO date (yyyy-MM-dd); defaults to today
     */
    @GetMapping
    public String daily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        LocalDate day = date != null ? date : LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant start = day.atStartOfDay(zone).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();

        // ---- Aggregates (single SQLite pass each, no in-memory accumulation) ----
        BigDecimal total = nvl(orderRepository.sumTotalByDay(start, end));
        BigDecimal cash = nvl(orderRepository.sumCashByDay(start, end));
        BigDecimal card = nvl(orderRepository.sumCardByDay(start, end));
        Long count = orderRepository.countCompletedByDay(start, end);
        Long cashCount = orderRepository.countCashByDay(start, end);

        // ---- Order list for the selected day (newest first) ----
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        model.addAttribute("day", day);
        model.addAttribute("orders", orders);
        model.addAttribute("totalSales", total);
        model.addAttribute("cashSales", cash);
        model.addAttribute("cardSales", card);
        model.addAttribute("orderCount", count != null ? count : 0L);
        model.addAttribute("cashCount", cashCount != null ? cashCount : 0L);
        model.addAttribute("avgSale", count != null && count > 0
                ? total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        return "reports";
    }

    /** Full detail of one order, including the embedded line items. */
    @GetMapping("/order/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        