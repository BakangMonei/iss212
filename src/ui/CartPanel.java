package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import manager.OrderManager;
import model.MenuItem;
import model.Order;
import model.OrderItem;
import model.Role;
import model.StaffUser;
import model.UserSession;

/** Review basket, choose delivery point, and submit to {@link OrderManager}. */
final class CartPanel extends JPanel {

    private final MainFrame frame;

    private final DefaultTableModel model;

    private final JTable table;
    private final JTextField location = new JTextField(32);
    private final JLabel totalLabel;

    CartPanel(MainFrame frame) {
        this.frame = frame;
        model = new DefaultTableModel(
                new Object[]{"Item", "Qty", "Unit Price", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Integer.class;
                }
                if (columnIndex == 2 || columnIndex == 3) {
                    return Double.class;
                }
                return String.class;
            }
        };

        table = new JTable(model);
        table.setFont(UITheme.fontBody());
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(UITheme.fontBody());
        table.setFillsViewportHeight(true);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 1) {
                    int r = e.getFirstRow();
                    int q = UserSession.clampQty((Integer) model.getValueAt(r, 1));
                    OrderItem oi = itemForRow(r);
                    if (oi != null) {
                        oi.setQuantity(q);
                        oi.recalculateSubtotal();
                        model.setValueAt(oi.getMenuItem().getPrice(), r, 2);
                        model.setValueAt(oi.getSubtotal(), r, 3);
                        syncMutableCartFromUi();
                        updateTotal();
                    }
                }
            }
        });

        totalLabel = new JLabel("Total: BWP 0.00");
        totalLabel.setFont(UITheme.fontHeading());
        totalLabel.setForeground(UITheme.TEXT_PRIMARY);

        setLayout(new BorderLayout(12, 12));
        setOpaque(false);

        JPanel north = UITheme.centredTitle("Checkout — review your cart", null);
        north.setOpaque(false);

        JPanel mid = UITheme.pad(new JPanel(new BorderLayout()));
        mid.setOpaque(false);
        mid.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel locRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        locRow.setOpaque(false);
        locRow.add(UITheme.body("Delivery location"));
        location.setFont(UITheme.fontBody());
        location.setColumns(42);
        locRow.add(location);

        mid.add(locRow, BorderLayout.SOUTH);

        JPanel bottom = UITheme.pad(new JPanel(new BorderLayout()));
        bottom.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JButton back = UITheme.secondaryButton("Back");
        back.addActionListener(e -> frame.switchPanel(MainFrame.MENU));
        JButton remove = UITheme.primaryButton("Remove");
        remove.addActionListener(e -> removeSelected());
        left.add(back);
        left.add(remove);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(totalLabel);
        JButton checkoutBtn = UITheme.primaryButton("Checkout");
        checkoutBtn.addActionListener(e -> doCheckout());
        right.add(checkoutBtn);

        bottom.add(left, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);

        add(north, BorderLayout.NORTH);
        add(mid, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    void onShow() {
        rebuildFromSession();
    }

    private void rebuildFromSession() {
        model.setRowCount(0);
        List<OrderItem> lines = UserSession.getInstance().getMutableCart();
        for (OrderItem oi : lines) {
            MenuItem m = oi.getMenuItem();
            model.addRow(new Object[]{m.getName(),
                    UserSession.clampQty(oi.getQuantity()),
                    m.getPrice(),
                    oi.getSubtotal()});
        }
        syncMutableCartFromUi();
        updateTotal();
    }

    private OrderItem itemForRow(int row) {
        List<OrderItem> lines = UserSession.getInstance().getMutableCart();
        if (row >= 0 && row < lines.size()) {
            return lines.get(row);
        }
        return null;
    }

    private void syncMutableCartFromUi() {
        int rows = Math.min(model.getRowCount(), UserSession.getInstance().getMutableCart().size());
        for (int i = 0; i < rows; i++) {
            OrderItem oi = UserSession.getInstance().getMutableCart().get(i);
            int q = UserSession.clampQty((Integer) model.getValueAt(i, 1));
            oi.setQuantity(q);
            oi.recalculateSubtotal();
        }
    }

    private void removeSelected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            return;
        }
        List<OrderItem> cart = UserSession.getInstance().getMutableCart();
        if (r < cart.size()) {
            cart.remove(r);
            model.removeRow(r);
            updateTotal();
        }
    }

    private double computeTotal() {
        double t = 0;
        List<OrderItem> cart = UserSession.getInstance().getMutableCart();
        for (OrderItem oi : cart) {
            t += oi.getSubtotal();
        }
        return t;
    }

    private void updateTotal() {
        totalLabel.setText(String.format("Total: BWP %.2f", computeTotal()));
        frame.getMenuBrowsePanel().refreshCartBadge();
    }

    private void doCheckout() {
        StaffUser staff = UserSession.getInstance().getCurrentUser();
        if (staff == null || staff.getRole() != Role.STAFF) {
            JOptionPane.showMessageDialog(this, "Staff session expired. Please log in again.",
                    "University of Botswana — Canteen", JOptionPane.WARNING_MESSAGE);
            frame.switchPanel(MainFrame.LOGIN);
            return;
        }
        List<OrderItem> cart = UserSession.getInstance().getMutableCart();
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty.",
                    "University of Botswana — Canteen", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String loc = location.getText() != null ? location.getText().trim() : "";
        if (loc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a delivery location.",
                    "University of Botswana — Canteen", JOptionPane.WARNING_MESSAGE);
            return;
        }
        syncMutableCartFromUi();
        Order order = OrderManager.getInstance().createOrder(staff, cart, loc);
        UserSession.getInstance().setLastPlacedOrder(order);
        UserSession.getInstance().clearCart();
        location.setText("");
        frame.getOrderSummaryPanel().bind(order);
        frame.switchPanel(MainFrame.SUMMARY);
    }
}
