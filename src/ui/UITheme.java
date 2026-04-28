package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * Shared colours, fonts, and panel chrome for the UB Canteen desktop UI.
 */
public final class UITheme {

    public static final Color UB_GREEN = Color.decode("#006747");
    public static final Color UB_GOLD = Color.decode("#FFB81C");
    public static final Color WHITE = Color.WHITE;
    public static final Color BG_PANEL = Color.decode("#F5F5F5");
    public static final Color TEXT_PRIMARY = Color.decode("#1A1A1A");
    public static final Color ERROR_RED = Color.decode("#C0392B");
    public static final Color SUCCESS_GREEN = Color.decode("#27AE60");

    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font HEADING = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    private UITheme() {
    }

    public static Font fontBody() {
        return BODY;
    }

    public static Font fontHeading() {
        return HEADING;
    }

    public static Font fontSmall() {
        return SMALL;
    }

    public static Border paddedBorder() {
        return BorderFactory.createEmptyBorder(16, 16, 16, 16);
    }

    public static JPanel pad(JPanel p) {
        p.setBorder(paddedBorder());
        p.setBackground(BG_PANEL);
        return p;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(fontHeading());
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    public static JLabel body(String text) {
        JLabel l = new JLabel(text);
        l.setFont(fontBody());
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(fontBody());
        b.setBackground(UB_GREEN);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        return b;
    }

    /** Gold outline / text for secondary actions (e.g. Back). */
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(fontBody());
        b.setBackground(UB_GOLD);
        b.setForeground(TEXT_PRIMARY);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        return b;
    }

    /** Top UB banner spanning the window width. */
    public static JPanel banner(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UB_GREEN);
        JLabel t = new JLabel(title, JLabel.CENTER);
        t.setFont(fontHeading());
        t.setForeground(WHITE);
        t.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        p.add(t, BorderLayout.CENTER);
        return p;
    }

    /** Right-aligned toolbar row inside a bordered panel segment. */
    public static JPanel toolRow(javax.swing.JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        row.setOpaque(false);
        for (javax.swing.JComponent c : components) {
            row.add(c);
        }
        return row;
    }

    public static JPanel centredTitle(String text, javax.swing.JComponent east) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = heading(text);
        p.add(t, BorderLayout.WEST);
        if (east != null) {
            p.add(east, BorderLayout.EAST);
        }
        return p;
    }
}
