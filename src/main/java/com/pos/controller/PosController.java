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

    /**
     * AJAX endpoint that returns matching products on a keystroke. The UI
     * renders them directly so the terminal stays responsive on a modest
     * CPU with a modest (2-4GB) footprint.
     */
    @GetMapping("/api/products")
    public String products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "") String q,
            Model model) {
        List<Product> products;

        if (q != null && !q.isBlank()) {
            // Search across barcode, name and category.
            products = productRepository.searchBy(q);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.searchByCategoryAndNameLike(category, q);
        } else {
            products = productRepository.findActiveOrdered();
        }

        model.addAttribute("products", products);
        return "fragments/products :: productGrid";
    }

    /**
     * Exact barcode lookup used by the scanner. Returns 200 + product JSON,
     * or 404 when the code is unknown / inactive.
     */
    @GetMapping("/api/products/barcode/{code}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> byBarcode(@PathVariable String code) {
        return productRepository.findByBarcode(code.trim())
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(p -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("id", p.getId());
                    body.put("barcode", p.getBarcode());
                    body.put("name", p.getName());
                    body.put("category", p.getCategory());
                    body.put("price", p.getPrice());
                    body.put("stock", p.getStock());
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    