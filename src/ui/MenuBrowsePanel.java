package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;

import manager.MenuManager;
import model.MenuItem;
import model.UserSession;

/** Category tabs listing {@link MenuItemCard} rows plus search and cart shortcuts. */
final class MenuBrowsePanel extends JPanel {

    private final MainFrame frame;

    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField search = new JTextField(42);
    private final JLabel cartBadge = new JLabel("Cart: 0 items");
    private final List<MenuItemCard> cards = new ArrayList<>();

    MenuBrowsePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(18, 18));
        setOpaque(false);

        JPanel north = UITheme.pad(new JPanel(new BorderLayout(12, 14)));
        north.setOpaque(false);
        north.add(UITheme.centredTitle("Browse today's menu",
                UITheme.toolRow(cartBadge, myOrdersButton(), viewCartButton())), BorderLayout.NORTH);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchRow.setOpaque(false);
        searchRow.add(UITheme.body("Search"));
        search.setFont(UITheme.fontBody());
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void bump() {
                rebuildTabs();
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                bump();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                bump();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                bump();
            }
        });
        searchRow.add(search);
        north.add(searchRow, BorderLayout.SOUTH);

        tabs.setFont(UITheme.fontHeading());

        JPanel centre = UITheme.pad(new JPanel(new BorderLayout()));
        centre.setOpaque(false);
        centre.add(new JScrollPane(tabs), BorderLayout.CENTER);

        JPanel southStrip = UITheme.pad(new JPanel(new FlowLayout(FlowLayout.RIGHT)));
        southStrip.setOpaque(false);
        JButton logout = UITheme.secondaryButton("Log out");
        logout.addActionListener(e -> logOut());
        southStrip.add(logout);

        add(north, BorderLayout.NORTH);
        add(centre, BorderLayout.CENTER);
        add(southStrip, BorderLayout.SOUTH);
    }

    private JButton myOrdersButton() {
        JButton b = UITheme.secondaryButton("My orders");
        b.addActionListener(e -> frame.switchPanel(MainFrame.STAFF_ORDERS));
        return b;
    }

    private JButton viewCartButton() {
        JButton b = UITheme.primaryButton("View Cart");
        b.addActionListener(e -> {
            frame.getCartPanel().onShow();
            frame.switchPanel(MainFrame.CART);
        });
        return b;
    }

    private void logOut() {
        UserSession.getInstance().setCurrentUser(null);
        UserSession.getInstance().clearCart();
        frame.switchPanel(MainFrame.LOGIN);
    }

    void onShow() {
        refreshCartBadge();
        rebuildTabs();
    }

    void refreshCartBadge() {
        int n = UserSession.getInstance().cartItemCount();
        cartBadge.setFont(UITheme.fontHeading());
        cartBadge.setForeground(UITheme.TEXT_PRIMARY);
        cartBadge.setText("Cart: " + n + (n == 1 ? " item" : " items"));
    }

    void rebuildTabs() {
        tabs.removeAll();
        cards.clear();
        MenuManager mgr = MenuManager.getInstance();
        List<MenuItem> all = mgr.getAllItems();
        Map<String, List<MenuItem>> grouped = filteredGroup(all);

        for (String cat : MenuManager.orderedCategories()) {
            List<MenuItem> group = grouped.get(cat);
            if (group == null || group.isEmpty()) {
                continue;
            }
            JPanel grid = new JPanel(new GridLayout(0, 2, 20, 20));
            grid.setOpaque(false);
            for (MenuItem mi : group) {
                MenuItem live = mgr.findById(mi.getId());
                MenuItem snapshot = live != null ? copy(live) : copy(mi);
                Runnable r = () -> {
                    refreshCartBadge();
                    frame.repaint();
                };
                MenuItemCard card = new MenuItemCard(frame, snapshot, r);
                cards.add(card);
                grid.add(card);
            }
            JPanel wrap = UITheme.pad(new JPanel(new BorderLayout()));
            wrap.setOpaque(false);
            JScrollPane sp = new JScrollPane(grid);
            UITheme.comfortScroll(sp);
            wrap.add(sp, BorderLayout.CENTER);
            tabs.addTab(cat, wrap);
        }
        tabs.revalidate();
        tabs.repaint();
    }

    private Map<String, List<MenuItem>> filteredGroup(List<MenuItem> all) {
        String q = search.getText() != null ? search.getText().trim().toLowerCase(Locale.ROOT) : "";
        Map<String, List<MenuItem>> map = new LinkedHashMap<>();
        for (String c : MenuManager.orderedCategories()) {
            map.put(c, new ArrayList<>());
        }
        for (MenuItem m : all) {
            if (!q.isEmpty()) {
                String blob = (m.getName() + " " + m.getCategory()).toLowerCase(Locale.ROOT);
                if (!blob.contains(q)) {
                    continue;
                }
            }
            String cat = m.getCategory() != null ? m.getCategory() : "Other";
            map.computeIfAbsent(cat, k -> new ArrayList<>()).add(m);
        }
        return map;
    }

    private static MenuItem copy(MenuItem m) {
        return new MenuItem(m.getId(), m.getName(), m.getPrice(), m.getCategory(),
                m.getQuantity(), m.isAvailable());
    }
}
