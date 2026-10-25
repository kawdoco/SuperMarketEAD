package com.pos.model;

import java.math.BigDecimal;

/**
 * One line of the checkout cart sent from the POS UI.
 * <p>
 * View-layer contract: the terminal JavaScript serialises each cart row into
 * this shape when posting to {@code POST /api/checkout}.
 */
public class ItemDto {

    private String barcode;
    private String name;
    private String category;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;

    public ItemDto() {
    }

    public ItemDto(String barcode, String name, String category,
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
}
