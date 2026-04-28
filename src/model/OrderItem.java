package model;

import org.json.JSONObject;

/**
 * Line held in cart or persisted order snapshots.
 */
public class OrderItem {
    private MenuItem menuItem;
    private int quantity;
    private double subtotal;

    public OrderItem(MenuItem menuItem, int quantity, double subtotal) {
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public void recalculateSubtotal() {
        subtotal = menuItem.getPrice() * quantity;
    }

    public static OrderItem fromJson(JSONObject o) {
        MenuItem mi = MenuItem.fromJson(o.getJSONObject("menuItem"));
        int qty = o.optInt("quantity", 1);
        double sub = o.optDouble("subtotal", mi.getPrice() * qty);
        return new OrderItem(mi, qty, sub);
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("menuItem", menuItem.toJson());
        o.put("quantity", quantity);
        o.put("subtotal", subtotal);
        return o;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
