package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * UB Canteen root window — {@link java.awt.CardLayout} switches staff and admin flows.
 */
public class MainFrame extends JFrame {

    public static final String LOGIN = "LOGIN";
    public static final String MENU = "MENU";
    public static final String STAFF_ORDERS = "STAFF_ORDERS";
    public static final String CART = "CART";
    public static final String SUMMARY = "SUMMARY";
    public static final String STATUS = "STATUS";
    public static final String ADMIN = "ADMIN";

    private final java.awt.CardLayout cards = new java.awt.CardLayout();
    private final JPanel deck = new JPanel(cards);

    private LoginPanel loginPanel;
    private MenuBrowsePanel menuBrowsePanel;
    private CartPanel cartPanel;
    private OrderSummaryPanel orderSummaryPanel;
    private OrderStatusPanel orderStatusPanel;
    private AdminPanel adminPanel;
    private StaffOrdersPanel staffOrdersPanel;

    public MainFrame() {
        super();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = UITheme.pad(new JPanel(new BorderLayout(20, 20)));
        JPanel banner = UITheme.banner("University of Botswana — Staff Canteen");
        banner.setOpaque(true);

        loginPanel = new LoginPanel(this);
        menuBrowsePanel = new MenuBrowsePanel(this);
        cartPanel = new CartPanel(this);
        orderSummaryPanel = new OrderSummaryPanel(this);
        orderStatusPanel = new OrderStatusPanel(this);
        adminPanel = new AdminPanel(this);
        staffOrdersPanel = new StaffOrdersPanel(this);

        deck.setOpaque(false);
        deck.add(loginPanel, LOGIN);
        deck.add(menuBrowsePanel, MENU);
        deck.add(cartPanel, CART);
        deck.add(orderSummaryPanel, SUMMARY);
        deck.add(orderStatusPanel, STATUS);
        deck.add(adminPanel, ADMIN);
        deck.add(staffOrdersPanel, STAFF_ORDERS);

        root.add(banner, BorderLayout.NORTH);
        root.add(deck, BorderLayout.CENTER);
        root.setBackground(UITheme.BG_PANEL);
        root.setOpaque(true);

        setContentPane(UITheme.pad(root));

        setTitle("University of Botswana — Staff Canteen");
        switchPanel(LOGIN);
        pack();
        setMinimumSize(new Dimension(1024, 700));
        setSize(new Dimension(1320, 860));
        setLocationRelativeTo(null);
    }

    /** Switch deck — key must match constants on this frame. */
    public void switchPanel(String name) {
        cards.show(deck, name);
        revalidate();
        repaint();
        if (MENU.equals(name)) {
            menuBrowsePanel.onShow();
        } else if (CART.equals(name)) {
            cartPanel.onShow();
        } else if (SUMMARY.equals(name)) {
            orderSummaryPanel.onShow();
        } else if (STATUS.equals(name)) {
            orderStatusPanel.onShow();
        } else if (ADMIN.equals(name)) {
            adminPanel.onShow();
        } else if (STAFF_ORDERS.equals(name)) {
            staffOrdersPanel.onShow();
        }
    }

    public JPanel getDeck() {
        return deck;
    }

    public MenuBrowsePanel getMenuBrowsePanel() {
        return menuBrowsePanel;
    }

    public CartPanel getCartPanel() {
        return cartPanel;
    }

    public OrderSummaryPanel getOrderSummaryPanel() {
        return orderSummaryPanel;
    }

    public OrderStatusPanel getOrderStatusPanel() {
        return orderStatusPanel;
    }

    public StaffOrdersPanel getStaffOrdersPanel() {
        return staffOrdersPanel;
    }

    public AdminPanel getAdminPanel() {
        return adminPanel;
    }
}
