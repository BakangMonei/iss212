package manager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import model.MenuItem;

/**
 * Singleton persistence for {@code data/menu.json} with atomic overwrite.
 */
public final class MenuManager {
    private static final MenuManager INSTANCE = new MenuManager();

    private final List<MenuItem> items = new ArrayList<>();
    private final Path dataDir = Paths.get(System.getProperty("user.dir"), "data");
    private final Path menuFile = dataDir.resolve("menu.json");
    private final Path menuTmp = dataDir.resolve("menu_tmp.json");
    private final AtomicLong idCounter = new AtomicLong(1);

    private MenuManager() {
    }

    public static MenuManager getInstance() {
        return INSTANCE;
    }

    public synchronized void loadMenu() {
        ensureDataDirExists();
        if (!Files.exists(menuFile)) {
            seedDefaultMenu();
            persistMenuUnchecked(true);
            System.out.println("Created default menu at " + menuFile.toAbsolutePath());
            return;
        }
        String text;
        try {
            byte[] bytes = Files.readAllBytes(menuFile);
            text = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            warnBlocking("Could not load menu. Starting with defaults.");
            seedDefaultMenu();
            return;
        }
        JSONObject root;
        try {
            root = new JSONObject(text);
        } catch (JSONException e) {
            warnBlocking("Corrupt menu JSON. Starting with defaults.");
            seedDefaultMenu();
            return;
        }
        JSONArray arr = root.optJSONArray("items");
        items.clear();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MenuItem m = MenuItem.fromJson(o);
                if (!m.getId().isEmpty()) {
                    items.add(m);
                }
            }
        }
        if (items.isEmpty()) {
            warnBlocking("Corrupt menu JSON. Starting with defaults.");
            seedDefaultMenu();
        }
    }

    private void warnBlocking(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(null, message, "University of Botswana — Canteen",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            try {
                SwingUtilities.invokeAndWait(() ->
                        JOptionPane.showMessageDialog(null, message,
                                "University of Botswana — Canteen", JOptionPane.WARNING_MESSAGE));
            } catch (Exception ignored) {
                JOptionPane.showMessageDialog(null, message, "University of Botswana — Canteen",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void ensureDataDirExists() {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not create data directory. Please check permissions.",
                    "University of Botswana — Canteen", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void seedDefaultMenu() {
        items.clear();
        addItem("Pap & Eggs", 18.0, "Breakfast", 50);
        addItem("Toast & Beans", 15.0, "Breakfast", 40);
        addItem("Vetkoek", 8.0, "Breakfast", 60);
        addItem("Fruit Salad", 12.0, "Breakfast", 30);
        addItem("Beef Stew & Rice", 35.0, "Mains", 40);
        addItem("Chicken & Dumplings", 38.0, "Mains", 35);
        addItem("Vegetable Curry & Rice", 28.0, "Mains", 45);
        addItem("Grilled Fish & Chips", 42.0, "Mains", 20);
        addItem("Chicken Pie", 14.0, "Snacks", 80);
        addItem("Sausage Roll", 10.0, "Snacks", 100);
        addItem("Samosa x3", 12.0, "Snacks", 70);
        addItem("Maheu", 8.0, "Drinks", 90);
        addItem("Mineral Water", 5.0, "Drinks", 120);
        addItem("Juice Box", 10.0, "Drinks", 85);
    }

    public synchronized List<MenuItem> getAllItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public synchronized MenuItem findById(String id) {
        if (id == null) {
            return null;
        }
        for (MenuItem m : items) {
            if (id.equals(m.getId())) {
                return m;
            }
        }
        return null;
    }

    public synchronized void addItem(String name, double price, String category, int quantity) {
        String id = nextId(name, category);
        MenuItem m = new MenuItem(id, name, price, category, quantity, true);
        items.add(m);
    }

    public synchronized void addItem(MenuItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(nextId(item.getName(), item.getCategory()));
        }
        items.add(item);
    }

    /** Called from admin bulk replace after table edit. */
    public synchronized void replaceAll(List<MenuItem> newList) {
        items.clear();
        for (MenuItem m : newList) {
            items.add(copy(m));
        }
    }

    public synchronized MenuItem removeItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < items.size()) {
            return items.remove(rowIndex);
        }
        return null;
    }

    public synchronized boolean saveMenuWithDialog() {
        return persistMenuChecked("Could not save changes. Please check disk space.");
    }

    public synchronized boolean persistMenuUnchecked(boolean overwriteEmptyOk) {
        try {
            doAtomicWrite(items);
            return true;
        } catch (IOException e) {
            if (!overwriteEmptyOk) {
                System.err.println("Menu save failed: " + e.getMessage());
            }
            return false;
        }
    }

    private boolean persistMenuChecked(String errorMessage) {
        try {
            doAtomicWrite(items);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, errorMessage, "University of Botswana — Canteen",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void doAtomicWrite(List<MenuItem> list) throws IOException {
        JSONArray arr = new JSONArray();
        for (MenuItem m : list) {
            arr.put(m.toJson());
        }
        JSONObject root = new JSONObject();
        root.put("items", arr);

        Files.createDirectories(dataDir);
        try (BufferedWriter w = Files.newBufferedWriter(menuTmp, StandardCharsets.UTF_8)) {
            w.write(root.toString(2));
        }
        Files.move(menuTmp, menuFile, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    private static MenuItem copy(MenuItem src) {
        return new MenuItem(src.getId(), src.getName(), src.getPrice(),
                src.getCategory(), src.getQuantity(), src.isAvailable());
    }

    private String nextId(String name, String category) {
        String base = asciiSlug(category + "_" + name).replace('.', '_');
        if (!containsId(base)) {
            return base;
        }
        return base + "_" + idCounter.getAndIncrement();
    }

    private boolean containsId(String id) {
        for (MenuItem m : items) {
            if (id.equals(m.getId())) {
                return true;
            }
        }
        return false;
    }

    private static String asciiSlug(String s) {
        String lower = s.toLowerCase(Locale.ROOT).trim();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                b.append(c);
            } else if (c == ' ' || c == '-' || c == '/') {
                b.append('_');
            }
        }
        String slug = b.toString().replaceAll("_+", "_");
        if (slug.isEmpty()) {
            return "item_" + UUID.randomUUID().toString().substring(0, 8);
        }
        return slug;
    }

    /** Category order for UI tabs. */
    public static List<String> orderedCategories() {
        List<String> order = new ArrayList<>();
        order.add("Breakfast");
        order.add("Mains");
        order.add("Snacks");
        order.add("Drinks");
        return order;
    }

    public static Map<String, List<MenuItem>> groupByCategory(List<MenuItem> all) {
        Map<String, List<MenuItem>> map = new LinkedHashMap<>();
        for (String c : orderedCategories()) {
            map.put(c, new ArrayList<>());
        }
        for (MenuItem m : all) {
            String cat = m.getCategory() != null ? m.getCategory() : "Other";
            map.computeIfAbsent(cat, k -> new ArrayList<>()).add(m);
        }
        return map;
    }
}
