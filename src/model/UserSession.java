package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Session-scoped login and shopping cart for the UB Canteen desktop app.
 */
public final class UserSession {
    private static final UserSession INSTANCE = new UserSession();

    private StaffUser currentUser;
    private final List<OrderItem> cart = new ArrayList<>();
    private volatile Order activeOrderTracking;
    private volatile Order lastPlacedOrder;

    private UserSession() {
    }

    public static UserSession getInstance() {
        return INSTANCE;
    }

    public StaffUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(StaffUser currentUser) {
        this.currentUser = currentUser;
    }

    public void clearCart() {
        cart.clear();
    }

    /** Total unit count across cart lines — shown next to basket icon in browse UI. */
    public int cartItemCount() {
        int n = 0;
        for (OrderItem oi : cart) {
            n += oi.getQuantity();
        }
        return n;
    }

    public List<OrderItem> getCartCopy() {
        List<OrderItem> copy = new ArrayList<>();
        for (OrderItem oi : cart) {
            MenuItem mc = Objects.requireNonNull(oi.getMenuItem()).copy();
            OrderItem noc = new OrderItem(mc, oi.getQuantity(), oi.getSubtotal());
            copy.add(noc);
        }
        return Collections.unmodifiableList(copy);
    }

    public List<OrderItem> getMutableCart() {
        return cart;
    }

    public Order getActiveOrderTracking() {
        return activeOrderTracking;
    }

    public void setActiveOrderTracking(Order activeOrderTracking) {
        this.activeOrderTracking = activeOrderTracking;
    }

    public Order getLastPlacedOrder() {
        return lastPlacedOrder;
    }

    public void setLastPlacedOrder(Order lastPlacedOrder) {
        this.lastPlacedOrder = lastPlacedOrder;
    }

    /** Adds or merges by menu-item id — stock checked at checkout time elsewhere. */
    public void addToCart(MenuItem item, int quantity) {
        if (!item.isAvailable()) {
            return;
        }
        for (OrderItem oi : cart) {
            if (oi.getMenuItem().getId().equals(item.getId())) {
                int next = clampQty(oi.getQuantity() + quantity);
                oi.setQuantity(next);
                oi.recalculateSubtotal();
                return;
            }
        }
        MenuItem mc = item.copy();
        OrderItem nov = new OrderItem(mc, clampQty(quantity), mc.getPrice() * clampQty(quantity));
        cart.add(nov);
    }

    public static int clampQty(int q) {
        if (q < 1) {
            return 1;
        }
        if (q > 10) {
            return 10;
        }
        return q;
    }
}
