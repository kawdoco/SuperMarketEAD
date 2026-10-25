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
