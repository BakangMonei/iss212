package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import manager.DeliveryManager;
import manager.OrderManager;
import model.Delivery;
import model.Order;
import model.OrderStatus;
import model.UserSession;

/** Live courier-style progress simulated with {@link javax.swing.Timer}. */
final class OrderStatusPanel extends JPanel implements PropertyChangeListener {

    private static final SimpleDateFormat ETA_FMT =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault(Locale.Category.FORMAT));

    private final MainFrame frame;

    private final JProgressBar progress = new JProgressBar(1, 4);
    private final JLabel statusText = new JLabel(" ");
    private final JLabel etaText = new JLabel(" ");
    private final JLabel orderIdText = new JLabel(" ");

    private Order tracking;

    OrderStatusPanel(MainFrame frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));

        JPanel north = UITheme.centredTitle("Delivery progress", null);
        north.setOpaque(false);

        JPanel centre = UITheme.pad(new JPanel(new BorderLayout(16, 16)));
        centre.setOpaque(false);

        progress.setFont(UITheme.fontBody());
        progress.setStringPainted(true);
        progress.setString("Pending | In Preparation | Out for Delivery | Delivered");
        progress.setPreferredSize(new Dimension(960, 48));

        statusText.setFont(UITheme.fontHeading());
        statusText.setForeground(UITheme.TEXT_PRIMARY);
        etaText.setFont(UITheme.fontBody());
        etaText.setForeground(UITheme.TEXT_PRIMARY);
        orderIdText.setFont(UITheme.fontBody());
        orderIdText.setForeground(UITheme.TEXT_PRIMARY);

        JPanel texts = UITheme.pad(new JPanel(new BorderLayout(8, 8)));
        texts.setOpaque(false);
        texts.add(orderIdText, BorderLayout.NORTH);
        texts.add(statusText, BorderLayout.CENTER);
        texts.add(etaText, BorderLayout.SOUTH);

        centre.add(progress, BorderLayout.NORTH);
        centre.add(texts, BorderLayout.CENTER);

        JButton newOrderBtn = UITheme.primaryButton("Place New Order");
        newOrderBtn.addActionListener(e -> startNewOrder());

        JPanel south = UITheme.pad(new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)));
        south.setOpaque(false);
        south.add(newOrderBtn);

        add(north, BorderLayout.NORTH);
        add(centre, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        DeliveryManager.getInstance().addPropertyChangeListener(this);
    }

    void setTrackingOrder(Order order) {
        this.tracking = order;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!DeliveryManager.PROPERTY_DELIVERY.equals(evt.getPropertyName())) {
            return;
        }
        Object nv = evt.getNewValue();
        if (!(nv instanceof Delivery)) {
            return;
        }
        Delivery d = (Delivery) nv;
        SwingUtilities.invokeLater(() -> maybeApply(d));
    }

    void onShow() {
        DeliveryManager.getInstance().refreshListeners();
        if (tracking == null) {
            return;
        }
        Delivery d = DeliveryManager.getInstance().getCurrentDelivery();
        if (d != null && d.getOrder() != null && d.getOrder().getOrderId().equals(tracking.getOrderId())) {
            maybeApply(d);
            return;
        }
        Order live = OrderManager.getInstance().findByOrderId(tracking.getOrderId());
        OrderStatus st = live != null && live.getStatus() != null ? live.getStatus() : tracking.getStatus();
        if (st == null) {
            st = OrderStatus.PENDING;
        }
        applyStatuses(st, tracking.getOrderId(), System.currentTimeMillis() + 8L * 1000 * 4L);
    }

    private void maybeApply(Delivery d) {
        if (tracking == null || d.getOrder() == null) {
            return;
        }
        if (!d.getOrder().getOrderId().equals(tracking.getOrderId())) {
            return;
        }
        applyDeliveryPayload(d.getOrder(), d.getStatus(), d.getEstimatedTimeEpochMs());
        tracking.setStatus(d.getOrder().getStatus());
    }

    private void applyDeliveryPayload(Order persisted, OrderStatus st, long etaEpoch) {
        orderIdText.setText("Order ID: " + persisted.getOrderId());
        statusText.setText("Status: " + Delivery.label(st));
        applyStatuses(st != null ? st : persisted.getStatus(), persisted.getOrderId(), etaEpoch);
    }

    private void applyStatuses(OrderStatus st, String orderId, long etaEpoch) {
        progress.setIndeterminate(false);
        progress.setValue(uiStep(st));
        orderIdText.setText("Order ID: " + orderId);
        statusText.setText("Status: " + Delivery.label(st));
        etaText.setText(st == OrderStatus.DELIVERED
                ? "Delivered — " + ETA_FMT.format(new Date(etaEpoch))
                : ("ETA completion window ends around " + ETA_FMT.format(new Date(etaEpoch))));
    }

    private static int uiStep(OrderStatus st) {
        if (st == null) {
            return 1;
        }
        switch (st) {
            case PENDING:
                return 1;
            case IN_PREPARATION:
                return 2;
            case OUT_FOR_DELIVERY:
                return 3;
            case DELIVERED:
                return 4;
            default:
                return 1;
        }
    }

    private void startNewOrder() {
        DeliveryManager.getInstance().stopSimulation();
        UserSession.getInstance().clearCart();
        tracking = null;
        UserSession.getInstance().setActiveOrderTracking(null);
        frame.switchPanel(MainFrame.MENU);
        frame.getMenuBrowsePanel().onShow();
    }
}
