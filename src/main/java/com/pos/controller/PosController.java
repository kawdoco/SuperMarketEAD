package com.pos.controller;

import com.pos.model.Product;
import com.pos.repository.OrderRepository;
import com.pos.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the touch-friendly POS home, category filter, and product search.
 */
@Controller
public class PosController {

    private static final Logger log = LoggerFactory.getLogger(PosController.class);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public PosController(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Root of the POS UI. Pre-loads a small working set of products so the
     * terminal can boot and start scanning with no first-run DB scan.
     */
    @GetMapping("/")
    public String home(Model model) {
        loadProducts(model);
        return "pos";
    }

    @GetMapping("/pos")
    public String pos(Model model) {
        loadProducts(model);
        return "pos";
    }

    