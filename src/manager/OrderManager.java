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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import model.MenuItem;
import model.Order;
import model.OrderItem;
import model.OrderStatus;
import model.StaffUser;

/**
 * Singleton persistence for {@code data/orders.json}.
 */
public final class OrderManager {
    private static final OrderManager INSTANCE = new OrderManager();

    private final List<Order> orders = new ArrayList<>();
    private final Path dataDir = Paths.get(System.getProperty("user.dir"), "data");
    private final Path ordersFile = dataDir.resolve("orders.json");
    private final Path ordersTmp = dataDir.resolve("orders_tmp.json");
    private final AtomicLong orderSeq = new AtomicLong(System.currentTimeMillis() % 100000);

    private OrderManager() {
    }

    public static OrderManager getInstance() {
        return INSTANCE;
    }

    public synchronized void loadOrders() {
        ensureDataDirExists();
        if (!Files.exists(ordersFile)) {
            orders.clear();
            persistOrdersUnchecked(true);
            System.out.println("Created empty orders file at " + ordersFile.toAbsolutePath());
            return;
        }
        String text;
        try {
            text = new String(Files.readAllBytes(ordersFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            warnBlocking("Could not load orders. Starting with defaults.");
            orders.clear();
            return;
        }
        JSONArray arr;
        try {
            arr = new JSONArray(text);
        } catch (JSONException e) {
            warnBlocking("Corrupt orders JSON. Starting with defaults.");
            orders.clear();
            return;
        }
        orders.clear();
        for (int i = 0; i < arr.length(); i++) {
            try {
                Order o = Order.fromJson(arr.getJSONObject(i));
                if (o.getOrderId() != null && !o.getOrderId().isEmpty()) {
                    orders.add(o);
                }
            } catch (Exception ex) {
                // skip bad row
            }
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

    public synchronized List<Order> getAllOrders() {
        return Collections.unmodifiableList(new ArrayList<>(orders));
    }

    public synchronized Order findByOrderId(String id) {
        if (id == null) {
            return null;
        }
        for (Order o : orders) {
            if (id.equals(o.getOrderId())) {
                return o;
            }
        }
        return null;
    }

    public synchronized Order createOrder(StaffUser user, List<OrderItem> items,
                                          String deliveryLocation) {
        String oid = "UB-ORD-" + orderSeq.incrementAndGet();
        Order o = new Order();
        o.setOrderId(oid);
        o.setStaffUser(user);
        o.getMutableItems().clear();
        for (OrderItem oi : items) {
            MenuItem snap = oi.getMenuItem().copy();
            OrderItem line = new OrderItem(snap, oi.getQuantity(), oi.getSubtotal());
            line.recalculateSubtotal();
            o.getMutableItems().add(line);
        }
        o.setStatus(OrderStatus.PENDING);
        o.setDeliveryLocation(deliveryLocation);
        o.setTimestamp(System.currentTimeMillis());
        orders.add(o);
        saveOrdersImmediate("Could not save changes. Please check disk space.");
        return o;
    }

    public synchronized void updateStatus(String orderId, OrderStatus status) {
        Order o = findByOrderId(orderId);
        if (o != null) {
            o.setStatus(status);
            saveOrdersImmediate("Could not save changes. Please check disk space.");
        }
    }

    public synchronized void saveOrdersWithDialog(String errorMessage) {
        saveOrdersImmediate(errorMessage);
    }

    private void saveOrdersImmediate(String errorMessage) {
        try {
            doAtomicWrite(orders);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, errorMessage, "University of Botswana — Canteen",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Used when corrupt file scenario — suppress dialog if bootstrap. */
    public synchronized boolean persistOrdersUnchecked(boolean quiet) {
        try {
            doAtomicWrite(orders);
            return true;
        } catch (IOException e) {
            if (!quiet) {
                JOptionPane.showMessageDialog(null,
                        "Could not save changes. Please check disk space.",
                        "University of Botswana — Canteen", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }

    private void doAtomicWrite(List<Order> list) throws IOException {
        JSONArray arr = new JSONArray();
        for (Order o : list) {
            arr.put(o.toJson());
        }
        Files.createDirectories(dataDir);
        try (BufferedWriter w = Files.newBufferedWriter(ordersTmp, StandardCharsets.UTF_8)) {
            w.write(arr.toString(2));
        }
        Files.move(ordersTmp, ordersFile, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }
}
