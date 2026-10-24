package com.pos.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout contract carried from the POS UI on every AJAX sale.
 * <p>
 * View-layer contract: the terminal JavaScript posts this object to
 * {@code POST /api/checkout}; the controller converts it into a persisted
 * {@link Order}.
 */