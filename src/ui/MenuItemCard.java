package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import manager.MenuManager;
import model.MenuItem;
import model.UserSession;

/** Single card describing one {@link MenuItem} with quantity and add-to-cart control. */
final class MenuItemCard extends JPanel {

    private final MenuItem item;
    private final Runnable cartListener;
    private final JSpinner qtySpinner;

    MenuItemCard(MainFrame frame, MenuItem item, Runnable cartListener) {
        this.item = item;
        this.cartListener = cartListener;
        setLayout(new BorderLayout(8, 8));
        setBackground(UITheme.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.UB_GREEN, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel title = new JLabel(item.getName());
        title.setFont(UITheme.fontHeading().deriveFont(Font.BOLD));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel price = new JLabel(String.format("BWP %.2f", item.getPrice()));
        price.setFont(UITheme.fontBody());
        price.setForeground(UITheme.TEXT_PRIMARY);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgeRow.setOpaque(false);
        JLabel cat = new JLabel(" " + item.getCategory() + " ");
        cat.setOpaque(true);
        cat.setBackground(UITheme.UB_GOLD);
        cat.setForeground(UITheme.TEXT_PRIMARY);
        cat.setFont(UITheme.fontSmall());
        badgeRow.add(cat);

        JLabel avail = new JLabel(item.isAvailable() ? "Available" : "Out of Stock");
        avail.setFont(UITheme.fontBody());
        avail.setForeground(item.isAvailable() ? UITheme.SUCCESS_GREEN : UITheme.ERROR_RED);
        badgeRow.add(avail);

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.setOpaque(false);
        north.add(title, BorderLayout.NORTH);
        north.add(price, BorderLayout.CENTER);
        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        mid.add(north, BorderLayout.NORTH);
        mid.add(badgeRow, BorderLayout.CENTER);

        SpinnerNumberModel m = new SpinnerNumberModel(1, 1, 10, 1);
        qtySpinner = new JSpinner(m);
        qtySpinner.setFont(UITheme.fontBody());
        ChangeListener sync = e -> {
            int v = UserSession.clampQty((Integer) qtySpinner.getValue());
            if (!Integer.valueOf(v).equals(qtySpinner.getValue())) {
                qtySpinner.setValue(v);
            }
        };
        qtySpinner.addChangeListener(sync);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        south.setOpaque(false);
        south.add(UITheme.body("Qty"));
        south.add(qtySpinner);

        JButton addBtn = UITheme.primaryButton("Add to Cart");
        addBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        addBtn.setEnabled(item.isAvailable());
        addBtn.addActionListener(e -> addClicked());

        south.add(addBtn);

        add(mid, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void addClicked() {
        MenuItem live = MenuManager.getInstance().findById(item.getId());
        if (live == null || !live.isAvailable()) {
            JOptionPane.showMessageDialog(this,
                    "Sorry, this item is currently unavailable.",
                    "University of Botswana — Canteen",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int qty = UserSession.clampQty((Integer) qtySpinner.getValue());
        UserSession.getInstance().addToCart(live, qty);
        cartListener.run();
    }
}
