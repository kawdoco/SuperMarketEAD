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
