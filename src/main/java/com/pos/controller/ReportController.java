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
