package com.pos.model;

/**
 * Lifecycle state of an order used for report grouping.
 */
public enum OrderStatus {
    /**
     * Order has been created but not yet completed/printed.
     */
    PENDING,

    /**
     * Order fully processed, receipt printed and cash drawer released.
     */
    COMPLETED
}
