package model;

/**
 * Lifecycle states for cafeteria orders placed by UB staff.
 */
public enum OrderStatus {
    PENDING,
    IN_PREPARATION,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
