package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Persisted cafeteria order belonging to UB staff with delivery meta-data.
 */
public class Order {
    private String orderId;
    private StaffUser staffUser;
    private final List<OrderItem> items;
    private OrderStatus status;
    private String deliveryLocation;
    private long timestamp;

    public Order() {
        this.items = new ArrayList<>();
    }

    public Order(String orderId, StaffUser staffUser, List<OrderItem> items,
                 OrderStatus status, String deliveryLocation, long timestamp) {
        this.orderId = orderId;
        this.staffUser = staffUser;
        this.items = new ArrayList<>(items);
        this.status = status;
        this.deliveryLocation = deliveryLocation;
        this.timestamp = timestamp;
    }

    public JSONObject toJson() {
        JSONObject root = new JSONObject();
        root.put("orderId", orderId);

        JSONObject su = new JSONObject();
        su.put("staffId", staffUser != null ? staffUser.getStaffId() : "");
        su.put("name", staffUser != null ? staffUser.getName() : "");
        su.put("department", staffUser != null ? staffUser.getDepartment() : "");
        su.put("role", staffUser != null ? staffUser.getRole().name() : Role.STAFF.name());
        root.put("staffUser", su);

        JSONArray arr = new JSONArray();
        for (OrderItem oi : items) {
            arr.put(oi.toJson());
        }
        root.put("items", arr);

        root.put("status", status != null ? status.name() : OrderStatus.PENDING.name());
        root.put("deliveryLocation", deliveryLocation != null ? deliveryLocation : "");
        root.put("timestamp", timestamp);

        return root;
    }

    public static Order fromJson(JSONObject root) throws IllegalArgumentException {
        Order order = new Order();
        order.setOrderId(root.optString("orderId", ""));
        JSONObject su = root.optJSONObject("staffUser");
        if (su != null) {
            Role r = Role.STAFF;
            try {
                r = Role.valueOf(su.optString("role", Role.STAFF.name()));
            } catch (IllegalArgumentException ignored) {
                r = Role.STAFF;
            }
            order.setStaffUser(new StaffUser(
                    su.optString("staffId", ""),
                    su.optString("name", ""),
                    su.optString("department", ""),
                    r));
        }
        JSONArray items = root.optJSONArray("items");
        order.items.clear();
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                order.items.add(OrderItem.fromJson(items.getJSONObject(i)));
            }
        }
        try {
            order.setStatus(OrderStatus.valueOf(
                    root.optString("status", OrderStatus.PENDING.name())));
        } catch (IllegalArgumentException ex) {
            order.setStatus(OrderStatus.PENDING);
        }
        order.setDeliveryLocation(root.optString("deliveryLocation", ""));
        order.setTimestamp(root.optLong("timestamp", System.currentTimeMillis()));
        return order;
    }

    public double calculateGrandTotal() {
        double t = 0;
        for (OrderItem oi : items) {
            t += oi.getSubtotal();
        }
        return t;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public StaffUser getStaffUser() {
        return staffUser;
    }

    public void setStaffUser(StaffUser staffUser) {
        this.staffUser = staffUser;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<OrderItem> getMutableItems() {
        return items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
