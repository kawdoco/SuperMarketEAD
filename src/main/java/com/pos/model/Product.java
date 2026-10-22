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
    
}
