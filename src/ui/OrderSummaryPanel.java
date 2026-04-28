package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import manager.DeliveryManager;
import manager.OrderManager;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import model.UserSession;

/** Post-checkout receipt with shortcut into live tracking. */
final class OrderSummaryPanel extends JPanel {

    private final MainFrame frame;

    private final JLabel orderIdLabel = new JLabel(" ");
    private final JLabel staffLabel = new JLabel(" ");
    private final JLabel locationLabel = new JLabel(" ");
    private final JLabel totalLabel = new JLabel("Grand total: BWP 0.00");
    private final DefaultTableModel tableModel =
            new DefaultTableModel(new Object[]{"Item", "Qty", "Unit Price", "Subtotal"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable itemsTable;

    /** Order rendered on summary after checkout — may diverge once persisted copies update. */
    private Order snapshot;

    OrderSummaryPanel(MainFrame frame) {
        this.frame = frame;
        itemsTable = new JTable(tableModel);
        UITheme.comfortTable(itemsTable);
        setOpaque(false);
        setLayout(new BorderLayout(20, 20));

        JPanel header = UITheme.centredTitle("Order confirmed", null);
        header.setOpaque(false);

        JPanel meta = new JPanel(new java.awt.GridLayout(0, 1, 8, 8));
        meta.setOpaque(false);
        orderIdLabel.setFont(UITheme.fontBody());
        staffLabel.setFont(UITheme.fontBody());
        locationLabel.setFont(UITheme.fontBody());
        meta.add(orderIdLabel);
        meta.add(staffLabel);
        meta.add(locationLabel);

        JPanel centreArea = UITheme.pad(new JPanel(new BorderLayout(8, 8)));
        centreArea.setOpaque(false);
        JScrollPane tableScroll = new JScrollPane(itemsTable);
        UITheme.comfortScroll(tableScroll);
        tableScroll.setPreferredSize(new Dimension(500, 380));
        centreArea.add(tableScroll, BorderLayout.CENTER);

        totalLabel.setFont(UITheme.fontTitle());
        totalLabel.setForeground(UITheme.TEXT_PRIMARY);
        JPanel totalsRow = UITheme.pad(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        totalsRow.setOpaque(false);
        totalsRow.add(totalLabel);

        JButton track = UITheme.primaryButton("Track My Order");
        track.addActionListener(e -> trackOrder());
        JButton backMenu = UITheme.secondaryButton("Back to browse");
        backMenu.addActionListener(e -> {
            frame.switchPanel(MainFrame.MENU);
            frame.getMenuBrowsePanel().onShow();
        });

        JPanel actions = UITheme.pad(new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)));
        actions.setOpaque(false);
        actions.add(backMenu);
        actions.add(track);

        JPanel footer = UITheme.pad(new JPanel(new BorderLayout(8, 8)));
        footer.setOpaque(false);
        footer.add(totalsRow, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);

        JPanel topBlock = UITheme.pad(new JPanel(new BorderLayout(8, 8)));
        topBlock.setOpaque(false);
        topBlock.add(header, BorderLayout.NORTH);
        topBlock.add(meta, BorderLayout.SOUTH);

        add(topBlock, BorderLayout.NORTH);
        add(centreArea, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    void bind(Order order) {
        this.snapshot = order;
        UserSession.getInstance().setLastPlacedOrder(order);
    }

    void onShow() {
        Order o = snapshot;
        if (o == null) {
            o = UserSession.getInstance().getLastPlacedOrder();
        }
        if (o == null) {
            return;
        }
        Order fresh = OrderManager.getInstance().findByOrderId(o.getOrderId());
        if (fresh != null) {
            o = fresh;
        }
        orderIdLabel.setText("Order ID: " + o.getOrderId());
        staffLabel.setText("Staff: " + (o.getStaffUser() != null ? o.getStaffUser().getName() : ""));
        locationLabel.setText("Deliver to: " + o.getDeliveryLocation());
        tableModel.setRowCount(0);
        for (OrderItem oi : o.getItems()) {
            tableModel.addRow(new Object[]{
                    oi.getMenuItem().getName(),
                    oi.getQuantity(),
                    String.format("BWP %.2f", oi.getMenuItem().getPrice()),
                    String.format("BWP %.2f", oi.getSubtotal())});
        }
        totalLabel.setText(String.format("Grand total: BWP %.2f", o.calculateGrandTotal()));
    }

    private void trackOrder() {
        Order o = snapshot;
        if (o == null) {
            o = UserSession.getInstance().getLastPlacedOrder();
        }
        if (o == null) {
            return;
        }
        Order live = OrderManager.getInstance().findByOrderId(o.getOrderId());
        if (live != null) {
            o = live;
        }
        o.setStatus(o.getStatus() != null ? o.getStatus() : OrderStatus.PENDING);
        UserSession.getInstance().setActiveOrderTracking(o);
        DeliveryManager.getInstance().simulateDelivery(o);
        frame.getOrderStatusPanel().setTrackingOrder(o);
        frame.switchPanel(MainFrame.STATUS);
    }
}
