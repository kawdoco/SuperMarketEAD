package com.pos.model;

public package com.pos.controller;

import com.pos.model.Product;
import com.pos.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inventory management (the "C" in MVC).
 * <p>
 * Server-rendered CRUD over {@link Product}: create, edit, quick stock
 * adjustment and delete. Every mutation uses the Post/Redirect/Get pattern
 * so a browser refresh never double-submits a form.
 */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final ProductRepository productRepository;

    public InventoryController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ---------------- Views ----------------

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        List<Product> products = (q != null && !q.isBlank())
                ? productRepository.searchBy(q.trim())
                : productRepository.findActiveOrdered();

        model.addAttribute("products", products);
        model.addAttribute("categories", productRepository.findDistinctCategories());
        model.addAttribute("query", q);
        model.addAttribute("form", new Product()); // blank form bean for create
        return "inventory";
    }

    /** Load one product into the edit form. */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("error", "Product #" + id + " not found");
            return "redirect:/inventory";
        }
        model.addAttribute("form", product);
        model.addAttribute("products", productRepository.findActiveOrdered());
        model.addAttribute("categories", productRepository.findDistinctCategories());
        return "inventory";
    