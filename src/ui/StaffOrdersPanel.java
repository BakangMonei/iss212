package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import manager.OrderManager;
import model.Delivery;
import model.Order;
import model.OrderStatus;
import model.Role;
import model.StaffUser;
import model.UserSession;

/**
 * Staff view: list of this user’s placed orders with quick access to live tracking.
 */
final class StaffOrdersPanel extends JPanel implements PropertyChangeListener {

    private static final SimpleDateFormat WHEN =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault(Locale.Category.FORMAT));

    private final MainFrame frame;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Order ID", "Status", "Total (BWP)", "Deliver to", "Placed"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);

    StaffOrdersPanel(MainFrame frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(16, 16));

        UITheme.comfortTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        JPanel north = UITheme.centredTitle("My orders — track deliveries", toolbar());
        north.setOpaque(false);

        JPanel mid = UITheme.pad(new JPanel(new BorderLayout()));
        mid.setOpaque(false);
        JScrollPane scroll = new JScrollPane(table);
        UITheme.comfortScroll(scroll);
        scroll.setPreferredSize(new Dimension(880, 420));
        mid.add(scroll, BorderLayout.CENTER);

        JButton trackBtn = UITheme.primaryButton("Track selected order");
        trackBtn.addActionListener(e -> trackSelected());

        JButton backMenu = UITheme.secondaryButton("Back to menu");
        backMenu.addActionListener(ev -> frame.switchPanel(MainFrame.MENU));

        JPanel south = UITheme.pad(new JPanel(new BorderLayout(8, 8)));
        south.setOpaque(false);
        JPanel tips = UITheme.pad(new JPanel(new BorderLayout()));
        tips.setOpaque(false);
        tips.add(UITheme.body("Double-click an order row to open tracking, or press Track selected."), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        actions.setOpaque(false);
        actions.add(backMenu);
        actions.add(trackBtn);
        south.add(tips, BorderLayout.NORTH);
        south.add(actions, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(mid, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = table.rowAtPoint(e.getPoint());
                    if (r >= 0) {
                        table.setRowSelectionInterval(r, r);
                        trackSelected();
                    }
                }
            }
        });

        OrderManager.getInstance().addPropertyChangeListener(this);
    }

    private JPanel toolbar() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        row.setOpaque(false);
        JButton refresh = UITheme.secondaryButton("Refresh");
        refresh.addActionListener(e -> populate());
        row.add(refresh);
        return row;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!OrderManager.PROPERTY_ORDER.equals(evt.getPropertyName())) {
            return;
        }
        javax.swing.SwingUtilities.invokeLater(this::populate);
    }

    void onShow() {
        StaffUser staff = UserSession.getInstance().getCurrentUser();
        if (staff == null || staff.getRole() != Role.STAFF) {
            frame.switchPanel(MainFrame.LOGIN);
            return;
        }
        populate();
    }

    private void populate() {
        model.setRowCount(0);
        StaffUser staff = UserSession.getInstance().getCurrentUser();
        if (staff == null) {
            return;
        }
        String sid = staff.getStaffId();
        List<Order> mine = new ArrayList<>();
        for (Order o : OrderManager.getInstance().getAllOrders()) {
            if (matchesStaff(o, sid)) {
                mine.add(o);
            }
        }
        mine.sort(Comparator.comparingLong(Order::getTimestamp).reversed());
        for (Order o : mine) {
            OrderStatus st = o.getStatus() != null ? o.getStatus() : OrderStatus.PENDING;
            model.addRow(new Object[]{
                    o.getOrderId(),
                    Delivery.label(st),
                    String.format("%.2f", o.calculateGrandTotal()),
                    o.getDeliveryLocation() != null ? o.getDeliveryLocation() : "",
                    WHEN.format(new Date(o.getTimestamp()))
            });
        }
    }

    private static boolean matchesStaff(Order o, String sessionStaffId) {
        if (o.getStaffUser() == null || sessionStaffId == null) {
            return false;
        }
        return sessionStaffId.equalsIgnoreCase(o.getStaffUser().getStaffId());
    }

    private void trackSelected() {
        StaffUser staff = UserSession.getInstance().getCurrentUser();
        if (staff == null || staff.getRole() != Role.STAFF) {
            frame.switchPanel(MainFrame.LOGIN);
            return;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Select an order to track.",
                    "University of Botswana — Canteen", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int mr = table.convertRowIndexToModel(viewRow);
        String orderId = String.valueOf(model.getValueAt(mr, 0));
        Order live = OrderManager.getInstance().findByOrderId(orderId);
        if (live == null || !matchesStaff(live, staff.getStaffId())) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "That order could not be loaded.",
                    "University of Botswana — Canteen", javax.swing.JOptionPane.WARNING_MESSAGE);
            populate();
            return;
        }
        UserSession.getInstance().setActiveOrderTracking(live);
        frame.getOrderStatusPanel().setTrackingOrder(live);
        frame.switchPanel(MainFrame.STATUS);
    }
}
