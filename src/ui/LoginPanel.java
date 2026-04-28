package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import model.Role;
import model.StaffUser;
import model.UserSession;

/** Staff credential form with UB-branded styling and inline validation. */
final class LoginPanel extends JPanel {

    private final MainFrame frame;

    private final JTextField staffIdField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    /** Non-blocking credential / validation messaging (preferred over dialogs for routine errors). */
    private final JLabel feedback = new JLabel(" ");

    LoginPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 24));
        setOpaque(false);

        DocumentListener wipe = new DocumentListener() {
            private void clear() {
                clearFeedback();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                clear();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clear();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clear();
            }
        };
        staffIdField.getDocument().addDocumentListener(wipe);
        passwordField.getDocument().addDocumentListener(wipe);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        JLabel portal = new JLabel("UB Staff Portal");
        portal.setFont(UITheme.fontTitle());
        portal.setForeground(UITheme.TEXT_PRIMARY);
        centre.add(portal, g);
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
        g.gridy++;
        feedback.setForeground(UITheme.ERROR_RED);
        feedback.setFont(UITheme.fontBody());
        feedback.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 0, 8, 0));
        centre.add(feedback, g);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 0));
        actions.setOpaque(false);

        JButton adminModeBtn = UITheme.secondaryButton("Admin Mode");
        JButton loginBtn = UITheme.primaryButton("Sign In");
        JButton clearBtn = UITheme.secondaryButton("Clear");

        adminModeBtn.addActionListener(e -> {
            clearFeedback();
            staffIdField.setText("admin");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        });

        loginBtn.addActionListener(e -> attemptLogin());
        clearBtn.addActionListener(e -> {
            clearFeedback();
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
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(centre, BorderLayout.CENTER);
        add(wrap, BorderLayout.CENTER);

        staffIdField.setPreferredSize(new Dimension(480, 44));
        passwordField.setPreferredSize(new Dimension(480, 44));
        staffIdField.setMinimumSize(new Dimension(360, 44));
        passwordField.setMinimumSize(new Dimension(360, 44));
    }

    private void clearFeedback() {
        feedback.setText(" ");
        feedback.setVisible(true);
    }

    private void showError(String message) {
        feedback.setForeground(UITheme.ERROR_RED);
        feedback.setText(message);
    }

    private void attemptLogin() {
        String id = staffIdField.getText() != null ? staffIdField.getText().trim() : "";
        char[] pw = passwordField.getPassword();
        String pass = new String(pw);
        java.util.Arrays.fill(pw, '\0');

        if (id.isEmpty()) {
            showError("Please enter your staff ID.");
            staffIdField.requestFocusInWindow();
            return;
        }

        if (pass.isEmpty()) {
            showError("Please enter your password.");
            passwordField.requestFocusInWindow();
            return;
        }

        if ("admin".equalsIgnoreCase(id) && "admin123".equals(pass)) {
            clearFeedback();
            UserSession.getInstance().setCurrentUser(
                    new StaffUser("admin", "System Administrator",
                            "UB IT Services — Canteen", Role.ADMIN));
            staffIdField.setText("");
            passwordField.setText("");
            frame.switchPanel(MainFrame.ADMIN);
            return;
        }

        if ("staff123".equals(pass)) {
            if ("admin".equalsIgnoreCase(id)) {
                showError("Administrator sign-in requires password \"admin123\".");
                passwordField.selectAll();
                passwordField.requestFocusInWindow();
                return;
            }
            clearFeedback();
            UserSession.getInstance().setCurrentUser(
                    new StaffUser(id, "Staff — " + id, "University of Botswana", Role.STAFF));
            staffIdField.setText("");
            passwordField.setText("");
            frame.switchPanel(MainFrame.MENU);
            frame.getMenuBrowsePanel().onShow();
            return;
        }

        showError("Invalid staff ID or password.");
        passwordField.selectAll();
        passwordField.requestFocusInWindow();
    }
}
