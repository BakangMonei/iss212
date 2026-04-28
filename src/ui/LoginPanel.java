package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import model.Role;
import model.StaffUser;
import model.UserSession;

/** Staff credential form with UB-branded styling. */
final class LoginPanel extends JPanel {

    private final MainFrame frame;

    /** Normal staff directory number / reference — any non-empty value is accepted alongside {@code staff123}. */
    private final JTextField staffIdField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    LoginPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        centre.add(UITheme.heading("UB Staff Portal"), g);
        g.gridy++;
        centre.add(UITheme.body("Staff ID"), g);
        g.gridy++;
        g.gridwidth = 2;
        staffIdField.setFont(UITheme.fontBody());
        centre.add(staffIdField, g);
        g.gridy++;
        g.gridwidth = 1;
        centre.add(UITheme.body("Password"), g);
        g.gridy++;
        g.gridwidth = 2;
        passwordField.setFont(UITheme.fontBody());
        centre.add(passwordField, g);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);

        JButton adminModeBtn = UITheme.secondaryButton("Admin Mode");
        JButton loginBtn = UITheme.primaryButton("Sign In");
        JButton clearBtn = UITheme.secondaryButton("Clear");

        adminModeBtn.addActionListener(e -> {
            staffIdField.setText("admin");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        });

        loginBtn.addActionListener(e -> attemptLogin());
        clearBtn.addActionListener(e -> {
            staffIdField.setText("");
            passwordField.setText("");
        });

        actions.add(adminModeBtn);
        actions.add(loginBtn);
        actions.add(clearBtn);

        g.gridy++;
        g.gridwidth = 2;
        centre.add(actions, g);

        add(UITheme.centredTitle("Sign in to order from the UB canteen", null), BorderLayout.NORTH);
        add(centre, BorderLayout.CENTER);

        staffIdField.setPreferredSize(new Dimension(320, 32));
        passwordField.setPreferredSize(new Dimension(320, 32));
    }

    private void attemptLogin() {
        String id = staffIdField.getText() != null ? staffIdField.getText().trim() : "";
        char[] pw = passwordField.getPassword();
        String pass = new String(pw);
        java.util.Arrays.fill(pw, '\0');

        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your staff ID.",
                    "University of Botswana — Canteen", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if ("admin".equalsIgnoreCase(id) && "admin123".equals(pass)) {
            UserSession.getInstance().setCurrentUser(
                    new StaffUser("admin", "System Administrator",
                            "UB IT Services — Canteen", Role.ADMIN));
            staffIdField.setText("");
            passwordField.setText("");
            frame.switchPanel(MainFrame.ADMIN);
            return;
        }

        if ("staff123".equals(pass)) {
            UserSession.getInstance().setCurrentUser(
                    new StaffUser(id, "Staff — " + id, "University of Botswana", Role.STAFF));
            staffIdField.setText("");
            passwordField.setText("");
            frame.switchPanel(MainFrame.MENU);
            frame.getMenuBrowsePanel().onShow();
            return;
        }

        JOptionPane.showMessageDialog(this, "Invalid staff ID or password.",
                "University of Botswana — Canteen", JOptionPane.ERROR_MESSAGE);
    }
}
