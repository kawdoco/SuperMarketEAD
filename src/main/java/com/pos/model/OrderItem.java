package com.pos.model;

import java.math.BigDecimal;

/**
 * Line item snapshot embedded inside the parent {@link Order}.
 * <p>
 * Serialised to JSON into {@code Order.itemsJson} at checkout so every
 * order stays a single self-contained record — SQLite reads and the report
 * queries stay trivial. Not a JPA entity by design.
 */
public class OrderItem {

    private String barcode;
    private String name;
    private String category;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;

    public OrderItem() {
    }

    public OrderItem(String barcode, String name, String category,
                     BigDecimal unitPrice, Integer quantity, BigDecimal lineTotal) {
        this.barcode = barcode;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    @Override
    public String toString() {
        return quantity + " x " + name + " @ " + unitPrice;
    }
}
