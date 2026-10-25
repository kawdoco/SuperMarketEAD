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
public class OrderRequest {

    private List<ItemDto> items;
    private String customerName;
    private PaymentMethod paymentMethod;
    private BigDecimal cashTendered;
    private String cardLastFour;
    private String terminal;

    public List<ItemDto> getItems() {
        return items;
    }

    public void setItems(List<ItemDto> items) {
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }
}
