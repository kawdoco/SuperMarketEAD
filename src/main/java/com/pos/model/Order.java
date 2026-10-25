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

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_customer", columnList = "customer_name"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
})

public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Terminal operator / display name of the person at the drawer. */
    @Column(name = "terminal", length = 64)
    private String terminal;

    /** Free-text customer name (optional on a retail counter). */
    @Column(name = "customer_name", length = 128)
    private String customerName;

    /** Status determines report grouping. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    /** How the sale was paid for. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 16)
    private PaymentMethod paymentMethod;

    /** Cash amount tendered; null for card sales. */
    @Column(precision = 12, scale = 2)
    private BigDecimal cashTendered;

    /** Card result code / last four digits supplied at checkout. */
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    /** Total before tax. */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /** Tax amount. */
    @Column(name = "tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal tax;

    /** Total charged to the customer. */
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    /** Compact JSON array of the sales line items. */
    @Column(name = "items_json", length = 4096)
    private String itemsJson;

    /** ISO-8601 local timestamp when the order was completed. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getCashTendered() {
        return cashTendered;
    }

    public void setCashTendered(BigDecimal cashTendered) {
        this.cashTendered = cashTendered;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getItemsJson() {
        return itemsJson;
    }

    public void setItemsJson(String itemsJson) {
        this.itemsJson = itemsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        if (itemsJson == null || itemsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModules()
                    .readValue(itemsJson, new com.fasterxml.jackson.core.type.TypeReference<List<OrderItem>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException("Corrupt order items payload", e);
        }
    }

    /** Persist the current live line items as compact JSON. */
    public void setItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            this.itemsJson = null;
            return;
        }
        try {
            this.itemsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModules()
                    .writeValueAsString(items);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise order items", e);
        }
    }

    /** Builder-style construction for JPA-friendly object creation. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for constructing an {@link Order}. */
    public static class Builder {
        private Long id;
        private String terminal;
        private String customerName;
        private OrderStatus status;
        private PaymentMethod paymentMethod;
        private BigDecimal cashTendered;
        private String cardLastFour;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal total;
        private String itemsJson;
        private Instant createdAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder terminal(String terminal) {
            this.terminal = terminal;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder cashTendered(BigDecimal cashTendered) {
            this.cashTendered = cashTendered;
            return this;
        }

        public Builder cardLastFour(String cardLastFour) {
            this.cardLastFour = cardLastFour;
            return this;
        }

        public Builder subtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder tax(BigDecimal tax) {
            this.tax = tax;
            return this;
        }

        public Builder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public Builder itemsJson(String itemsJson) {
            this.itemsJson = itemsJson;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder items(List<OrderItem> items) {
            if (items == null || items.isEmpty()) {
                this.itemsJson = null;
            } else {
                try {
                    this.itemsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                            .registerModules()
                            .writeValueAsString(items);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to serialise order items", e);
                }
            }
            return this;
        }