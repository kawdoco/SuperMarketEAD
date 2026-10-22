package com.pos.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Product / inventory line item.
 * <p>
 * Identified by a numeric database id plus a user-supplied barcode SKU so the
 * POS terminal can scan or type a barcode and hit the exact product.
 */

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_barcode", columnList = "barcode"),
        @Index(name = "idx_products_category", columnList = "category")
})

public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Barcode / SKU read from a scanner or typed at the POS. Must be unique. */
    @Column(name = "barcode", nullable = false, unique = true, length = 64)
    private String barcode;

    /** Human readable name, e.g. "Cola 500ml". */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** Category used for grid filtering. */
    @Column(name = "category", nullable = false, length = 64)
    private String category;

    /** Selling price in the POS currency (e.g. 1500). */
    @Column(nullable = false)
    private BigDecimal price;

    /** Current on-hand stock. */
    @Column(nullable = false)
    private Integer stock;

    /** Low stock threshold shown in the UI when restocking. */
    @Column(nullable = false)
    private Integer lowStockThreshold;

    /** Blanket size. */
    @Column(name = "size", length = 32)
    private String size;

    /** Simple textual description. */
    @Column(name = "description", length = 512)
    private String description;

    /** 0 = inactive, 1 = active. */
    @Column(nullable = false)
    private Boolean active = true;

    /** Last time this product was sold (used by the activity report). */
    @Column(name = "last_sold_at")
    private java.time.Instant lastSoldAt;

    /** Insert / audit timestamps. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    @PrePersist
    void onCreate() {
        java.time.Instant now = java.time.Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }
    
}
