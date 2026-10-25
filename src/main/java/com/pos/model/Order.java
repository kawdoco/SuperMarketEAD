package com.pos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Sales order header. One order = many order items + one payment method.
 * <p>
 * Line items are materialised at checkout and stored as a compact JSON
 * array in {@link #itemsJson}. The getter serialises it back into a live
 * {@link List} so report queries never need to look at the DB across
 * session boundaries.
 */