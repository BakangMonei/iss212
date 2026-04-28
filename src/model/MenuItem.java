package model;

import org.json.JSONObject;

/**
 * A single café item with stock and categorisation used in {@code menu.json}.
 */
public class MenuItem {
    private String id;
    private String name;
    private double price;
    private String category;
    private int quantity;
    private boolean available;

    public MenuItem() {
    }

    public MenuItem(String id, String name, double price, String category,
                    int quantity, boolean available) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.quantity = quantity;
        this.available = available;
    }

    /** Deep copy — useful when editing snapshots in carts and orders. */
    public MenuItem copy() {
        return new MenuItem(id, name, price, category, quantity, available);
    }

    public static MenuItem fromJson(JSONObject o) {
        MenuItem m = new MenuItem();
        m.id = o.optString("id", "");
        m.name = o.optString("name", "");
        m.price = o.optDouble("price", 0);
        m.category = o.optString("category", "");
        m.quantity = o.optInt("quantity", 0);
        m.available = o.optBoolean("available", true);
        return m;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("price", price);
        o.put("category", category);
        o.put("quantity", quantity);
        o.put("available", available);
        return o;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
