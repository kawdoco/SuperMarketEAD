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
    
}
