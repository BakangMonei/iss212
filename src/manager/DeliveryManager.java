package manager;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import model.Delivery;
import model.Order;
import model.OrderStatus;

/**
 * Simulates delivery stages with a Swing timer and notifies UI via property events.
 */
public final class DeliveryManager {
    public static final String PROPERTY_DELIVERY = "delivery";

    private static final DeliveryManager INSTANCE = new DeliveryManager();

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final AtomicReference<Timer> activeTimer = new AtomicReference<>();
    private volatile Delivery current;

    private DeliveryManager() {
    }

    public static DeliveryManager getInstance() {
        return INSTANCE;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    public Delivery getCurrentDelivery() {
        return current;
    }

    /** Stop any previous simulation; start 8-second steps through live order statuses. */
    public void simulateDelivery(Order order) {
        Objects.requireNonNull(order, "order");
        stopSimulation();

        String did = "DLV-" + UUID.randomUUID().toString().substring(0, 8);
        long eta = System.currentTimeMillis() + 8L * 4 * 1000;
        Delivery first = new Delivery(did, order, eta, OrderStatus.PENDING);
        Delivery old = current;
        current = first;
        pcs.firePropertyChange(PROPERTY_DELIVERY, old, first);

        OrderManager.getInstance().updateStatus(order.getOrderId(), OrderStatus.PENDING);

        final int[] step = {0};
        Timer t = new Timer(8000, e -> {
            step[0]++;
            OrderStatus next;
            switch (step[0]) {
                case 1:
                    next = OrderStatus.IN_PREPARATION;
                    break;
                case 2:
                    next = OrderStatus.OUT_FOR_DELIVERY;
                    break;
                case 3:
                    next = OrderStatus.DELIVERED;
                    break;
                default:
                    ((Timer) e.getSource()).stop();
                    return;
            }
            order.setStatus(next);
            OrderManager.getInstance().updateStatus(order.getOrderId(), next);
            long newEta = System.currentTimeMillis() + (4L - step[0]) * 8L * 1000;
            if (next == OrderStatus.DELIVERED) {
                newEta = System.currentTimeMillis();
            }
            Delivery prev = current;
            current = new Delivery(did, order, newEta, next);
            pcs.firePropertyChange(PROPERTY_DELIVERY, prev, current);
            if (next == OrderStatus.DELIVERED) {
                ((Timer) e.getSource()).stop();
            }
        });
        t.setInitialDelay(8000);
        t.setRepeats(true);
        activeTimer.set(t);
        t.start();
    }

    public void stopSimulation() {
        Timer t = activeTimer.getAndSet(null);
        if (t != null) {
            t.stop();
        }
    }

    /**
     * Invoked after {@link OrderManager#updateStatus(String, OrderStatus)} from admin UI.
     * If the affected order is the one currently tracked for simulation, stops the timer and
     * refreshes {@link #current} so staff tracking matches disk. Other orders' simulations are left running.
     */
    public void externalOrderStatusEdited(String orderId) {
        if (orderId == null) {
            return;
        }
        Delivery prev = current;
        boolean sameTracked = prev != null && prev.getOrder() != null
                && orderId.equals(prev.getOrder().getOrderId());
        if (sameTracked) {
            stopSimulation();
            Order live = OrderManager.getInstance().findByOrderId(orderId);
            if (live != null) {
                Delivery neo = new Delivery(prev.getDeliveryId(), live,
                        System.currentTimeMillis(), live.getStatus());
                current = neo;
                pcs.firePropertyChange(PROPERTY_DELIVERY, prev, neo);
            }
        }
    }

    /** Fire an extra UI refresh on the EDT (used after panel attach). */
    public void refreshListeners() {
        SwingUtilities.invokeLater(() -> {
            Delivery d = current;
            pcs.firePropertyChange(PROPERTY_DELIVERY, null, d);
        });
    }
}
