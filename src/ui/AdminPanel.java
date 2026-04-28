package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import manager.MenuManager;
import manager.OrderManager;
import model.Delivery;
import model.MenuItem;
import model.Order;
import model.OrderStatus;
import model.Role;
import model.StaffUser;
import model.UserSession;

/** Administrator maintenance for menu catalogue and persisted orders overlay. */
final class AdminPanel extends JPanel {

    private final MainFrame frame;

    private final DefaultTableModel menuModel =
            new DefaultTableModel(new Object[]{"Name", "Price (BWP)", "Category", "Quantity", "Available"}, 0);
    private final JTable menuTable;
    private final DefaultTableModel historyModel =
            new DefaultTableModel(new Object[]{"Order ID", "Staff", "Total (BWP)", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable historyTable;

    AdminPanel(MainFrame frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(16, 16));

        menuTable = new JTable(menuModel);
        menuTable.setAutoCreateRowSorter(true);
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.comfortTable(menuTable);

        historyTable = new JTable(historyModel);
        UITheme.comfortTable(historyTable);

        JPanel menuMgmt = UITheme.pad(new JPanel(new BorderLayout(10, 12)));
        menuMgmt.setOpaque(false);
        menuMgmt.add(UITheme.body("Editable cells update the working copy until you press Save."), BorderLayout.NORTH);
        javax.swing.JScrollPane menuScroll = new javax.swing.JScrollPane(menuTable);
        UITheme.comfortScroll(menuScroll);
        menuMgmt.add(menuScroll, BorderLayout.CENTER);

        JPanel menuButtons = UITheme.pad(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        menuButtons.setOpaque(false);
        JButton addBtn = UITheme.primaryButton("Add Item");
        JButton rmBtn = UITheme.primaryButton("Remove");
        JButton toggleBtn = UITheme.primaryButton("Toggle Available");
        JButton saveBtn = UITheme.primaryButton("Save Changes");
        addBtn.addActionListener(e -> showAddDialog());
        rmBtn.addActionListener(e -> removeSelectedMenuRow());
        toggleBtn.addActionListener(e -> toggleAvailable());
        saveBtn.addActionListener(e -> saveMenu());
        menuButtons.add(addBtn);
        menuButtons.add(rmBtn);
        menuButtons.add(toggleBtn);
        menuButtons.add(saveBtn);
        menuMgmt.add(menuButtons, BorderLayout.SOUTH);

        JPanel historyWrap = UITheme.pad(new JPanel(new BorderLayout()));
        historyWrap.setOpaque(false);
        javax.swing.JScrollPane histScroll = new javax.swing.JScrollPane(historyTable);
        UITheme.comfortScroll(histScroll);
        historyWrap.add(histScroll, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.fontBody());
        tabs.addTab("Menu Management", menuMgmt);
        tabs.addTab("Order History", historyWrap);
        tabs.addChangeListener(e -> maybeRefreshOrders(tabs));

        JPanel south = UITheme.pad(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        south.setOpaque(false);
        JButton logout = UITheme.secondaryButton("Log out");
        logout.addActionListener(ev -> logout());
        south.add(logout);

        add(UITheme.centredTitle("Canteen administration", null), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void maybeRefreshOrders(JTabbedPane tabs) {
        if (tabs.getSelectedIndex() == 1) {
            refreshOrders();
        }
    }

    void onShow() {
        StaffUser admin = UserSession.getInstance().getCurrentUser();
        if (admin == null || admin.getRole() != Role.ADMIN) {
            frame.switchPanel(MainFrame.LOGIN);
            return;
        }
        populateMenuGrid();
        refreshOrders();
    }

    private void logout() {
        UserSession.getInstance().setCurrentUser(null);
        frame.switchPanel(MainFrame.LOGIN);
    }

    private void populateMenuGrid() {
        menuModel.setRowCount(0);
        for (MenuItem m : MenuManager.getInstance().getAllItems()) {
            menuModel.addRow(new Object[]{
                    m.getName(), m.getPrice(), m.getCategory(),
                    m.getQuantity(), m.isAvailable()
            });
        }
    }

    private void refreshOrders() {
        historyModel.setRowCount(0);
        for (Order o : OrderManager.getInstance().getAllOrders()) {
            String staff = "";
            if (o.getStaffUser() != null) {
                staff = o.getStaffUser().getStaffId()
                        + " — " + o.getStaffUser().getName();
            }
            historyModel.addRow(new Object[]{
                    o.getOrderId(),
                    staff,
                    String.format("%.2f", o.calculateGrandTotal()),
                    statusCaption(o.getStatus())
            });
        }
    }

    private static String statusCaption(OrderStatus s) {
        if (s == null) {
            return "";
        }
        return Delivery.label(s);
    }

    private void removeSelectedMenuRow() {
        int r = menuTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this,
                    "Choose a menu row.",
                    "University of Botswana — Canteen", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = menuTable.convertRowIndexToModel(r);
        List<MenuItem> current = rebuildMenuFromUi();
        if (modelRow >= 0 && modelRow < current.size()) {
            current.remove(modelRow);
            reloadMenuModel(current);
        }
    }

    private void toggleAvailable() {
        int r = menuTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this,
                    "Choose a menu row.",
                    "University of Botswana — Canteen", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = menuTable.convertRowIndexToModel(r);
        MenuItem mi = rebuildMenuFromUi().get(modelRow);
        mi.setAvailable(!mi.isAvailable());
        menuModel.setValueAt(mi.isAvailable(), modelRow, 4);
        menuTable.clearSelection();
    }

    private void saveMenu() {
        MenuManager mgr = MenuManager.getInstance();
        mgr.replaceAll(rebuildMenuFromUi());
        if (mgr.saveMenuWithDialog()) {
            JOptionPane.showMessageDialog(this,
                    "Menu saved to disk.",
                    "University of Botswana — Canteen", JOptionPane.INFORMATION_MESSAGE);
            populateMenuGrid();
        }
    }

    private List<MenuItem> rebuildMenuFromUi() {
        List<MenuItem> original = mgrSnapshot();
        List<MenuItem> next = new ArrayList<>();
        for (int row = 0; row < menuModel.getRowCount(); row++) {
            MenuItem base = original.get(row);
            String name = String.valueOf(menuModel.getValueAt(row, 0));
            double price = parsePrice(menuModel.getValueAt(row, 1));
            String cat = String.valueOf(menuModel.getValueAt(row, 2));
            int qty = parseInt(menuModel.getValueAt(row, 3), base.getQuantity());
            boolean avail = parseBool(menuModel.getValueAt(row, 4));
            MenuItem copy = base.copy();
            copy.setName(name);
            copy.setPrice(price);
            copy.setCategory(cat);
            copy.setQuantity(qty);
            copy.setAvailable(avail);
            next.add(copy);
        }
        return next;
    }

    private List<MenuItem> mgrSnapshot() {
        return new ArrayList<>(MenuManager.getInstance().getAllItems());
    }

    private void reloadMenuModel(List<MenuItem> rows) {
        menuModel.setRowCount(0);
        for (MenuItem m : rows) {
            menuModel.addRow(new Object[]{m.getName(), m.getPrice(),
                    m.getCategory(), m.getQuantity(), m.isAvailable()});
        }
    }

    private double parsePrice(Object cell) {
        if (cell instanceof Number) {
            return ((Number) cell).doubleValue();
        }
        try {
            return Double.parseDouble(cell != null ? cell.toString().trim() : "0");
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int parseInt(Object cell, int def) {
        if (cell instanceof Number) {
            return ((Number) cell).intValue();
        }
        try {
            return Integer.parseInt(cell.toString().trim());
        } catch (Exception ex) {
            return def;
        }
    }

    private boolean parseBool(Object cell) {
        if (cell instanceof Boolean) {
            return (Boolean) cell;
        }
        return Boolean.parseBoolean(String.valueOf(cell));
    }

    private void showAddDialog() {
        JDialog dlg = new JDialog(frame, true);
        dlg.setTitle("New menu article");
        JPanel form = UITheme.pad(new JPanel(new GridLayout(0, 2, 8, 8)));
        form.setOpaque(false);
        JTextField name = new JTextField();
        JTextField priceField = new JTextField("15.00");
        JTextField category = new JTextField("Mains");
        SpinnerNumberModel spin = new SpinnerNumberModel(30, 0, 9999, 1);
        JSpinner qty = new JSpinner(spin);
        name.setFont(UITheme.fontBody());
        priceField.setFont(UITheme.fontBody());
        category.setFont(UITheme.fontBody());
        qty.setFont(UITheme.fontBody());
        form.add(UITheme.body("Name"));
        form.add(name);
        form.add(UITheme.body("Price"));
        form.add(priceField);
        form.add(UITheme.body("Category"));
        form.add(category);
        form.add(UITheme.body("Opening stock"));
        form.add(qty);

        JButton ok = UITheme.primaryButton("Save item");
        JButton cancel = UITheme.secondaryButton("Cancel");
        ok.addActionListener(e -> {
            String nm = name.getText() != null ? name.getText().trim() : "";
            if (nm.isEmpty()) {
                JOptionPane.showMessageDialog(dlg,
                        "Name cannot be blank.",
                        "University of Botswana — Canteen",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            double px = parsePrice(priceField.getText());
            String cg = category.getText() != null ? category.getText().trim() : "";
            int stock = ((Number) qty.getValue()).intValue();
            MenuManager mgr = MenuManager.getInstance();
            mgr.addItem(nm, px, cg, stock);
            populateMenuGrid();
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());

        JPanel footer = UITheme.pad(new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)));
        footer.setOpaque(false);
        footer.add(cancel);
        footer.add(ok);

        dlg.setLayout(new BorderLayout());
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(frame);
        dlg.setMinimumSize(new Dimension(480, dlg.getPreferredSize().height));
        dlg.setVisible(true);
    }
}
