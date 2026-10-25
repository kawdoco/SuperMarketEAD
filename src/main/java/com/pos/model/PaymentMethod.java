package com.pos.model;

/**
 * Supported payment methods recorded against a completed order.
 */
public enum PaymentMethod {
    /**
     * Physical currency inserted/handed at the drawer.
     */
    CASH,

    /**
     * Swiped, dipped or tapped card (EMV / magnetic stripe / NFC).
     */
    CARD
}
