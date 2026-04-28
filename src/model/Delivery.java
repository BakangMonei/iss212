package model;

/**
 * Delivery tracking record produced by {@link manager.DeliveryManager}.
 */
public class Delivery {
    private final String deliveryId;
    private final Order order;
    private volatile long estimatedTimeEpochMs;
    private volatile OrderStatus status;

    public Delivery(String deliveryId, Order order, long estimatedTimeEpochMs, OrderStatus status) {
        this.deliveryId = deliveryId;
        this.order = order;
        this.estimatedTimeEpochMs = estimatedTimeEpochMs;
        this.status = status != null ? status : OrderStatus.PENDING;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public Order getOrder() {
        return order;
    }

    public long getEstimatedTimeEpochMs() {
        return estimatedTimeEpochMs;
    }

    public void setEstimatedTimeEpochMs(long estimatedTimeEpochMs) {
        this.estimatedTimeEpochMs = estimatedTimeEpochMs;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status != null ? status : OrderStatus.PENDING;
    }

    public static String label(OrderStatus status) {
        if (status == null) {
            return "Unknown";
        }
        switch (status) {
            case PENDING:
                return "Pending";
            case IN_PREPARATION:
                return "In Preparation";
            case OUT_FOR_DELIVERY:
                return "Out for Delivery";
            case DELIVERED:
                return "Delivered";
            case CANCELLED:
                return "Cancelled";
            default:
                return status.name();
        }
    }
}
