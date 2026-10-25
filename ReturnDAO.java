
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReturnDAO {

    public static class ReturnableItem {
        public int orderItemId;
        public int orderId;
        public String productName;
        public String size;
        public String color;
        public int purchasedQuantity;
        public int alreadyReturned;
        public double unitPrice;

        public ReturnableItem(
                int orderItemId,
                int orderId,
                String productName,
                String size,
                String color,
                int purchasedQuantity,
                int alreadyReturned,
                double unitPrice) {

            this.orderItemId = orderItemId;
            this.orderId = orderId;
            this.productName = productName;
            this.size = size;
            this.color = color;
            this.purchasedQuantity = purchasedQuantity;
            this.alreadyReturned = alreadyReturned;
            this.unitPrice = unitPrice;
        }

        public int getRemainingQuantity() {
            return purchasedQuantity - alreadyReturned;
        }

        @Override
        public String toString() {
            return "Order #" + orderId + " - " + productName
                    + " (" + size + ", " + color + ")"
                    + " - Remaining returnable: "
                    + getRemainingQuantity();
        }
    }

    // Load items that still have returnable quantity
    public List<ReturnableItem> getReturnableItems()
            throws SQLException {

        List<ReturnableItem> items = new ArrayList<>();

        String sql =
                "SELECT oi.order_item_id, oi.order_id, " +
                "p.product_name, pv.size, pv.color, " +
                "oi.quantity AS purchased_quantity, " +
                "oi.unit_price, " +
                "COALESCE(SUM(r.return_quantity), 0) AS returned_qty " +
                "FROM order_items oi " +
                "JOIN product_variants pv " +
                "ON oi.variant_id = pv.variant_id " +
                "JOIN products p ON pv.product_id = p.product_id " +
                "JOIN orders o ON oi.order_id = o.order_id " +
                "LEFT JOIN returns r " +
                "ON oi.order_item_id = r.order_item_id " +
                "AND r.return_type = 'Approved' " +
                "WHERE o.status <> 'Cancelled' " +
                "GROUP BY oi.order_item_id, oi.order_id, " +
                "p.product_name, pv.size, pv.color, " +
                "oi.quantity, oi.unit_price " +
                "HAVING oi.quantity > returned_qty " +
                "ORDER BY oi.order_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(new ReturnableItem(
                        rs.getInt("order_item_id"),
                        rs.getInt("order_id"),
                        rs.getString("product_name"),
                        rs.getString("size"),
                        rs.getString("color"),
                        rs.getInt("purchased_quantity"),
                        rs.getInt("returned_qty"),
                        rs.getDouble("unit_price")
                ));
            }
        }

        return items;
    }

    // Process a return, restore stock, and update order status
    public void processReturn(
            int orderItemId,
            int returnQuantity,
            String reason) throws SQLException {

        if (returnQuantity <= 0) {
            throw new SQLException(
                    "Return quantity must be greater than zero.");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new SQLException(
                    "Please provide a return reason.");
        }

        String lockSql =
                "SELECT oi.quantity, oi.variant_id, o.status, oi.order_id " +
                "FROM order_items oi " +
                "JOIN orders o ON oi.order_id = o.order_id " +
                "WHERE oi.order_item_id = ? FOR UPDATE";

        String returnedSql =
                "SELECT COALESCE(SUM(return_quantity), 0) " +
                "FROM returns " +
                "WHERE order_item_id = ? " +
                "AND return_type = 'Approved'";

        String insertSql =
                "INSERT INTO returns " +
                "(order_item_id, quantity, return_quantity, reason, return_type) " +
                "VALUES (?, ?, ?, ?, 'Approved')";

        String stockSql =
                "UPDATE product_variants " +
                "SET stock_quantity = stock_quantity + ? " +
                "WHERE variant_id = ?";

        String remainingItemsSql =
                "SELECT COUNT(*) " +
                "FROM order_items oi " +
                "LEFT JOIN (" +
                "    SELECT order_item_id, " +
                "    SUM(return_quantity) AS returned_qty " +
                "    FROM returns " +
                "    WHERE return_type = 'Approved' " +
                "    GROUP BY order_item_id " +
                ") r ON oi.order_item_id = r.order_item_id " +
                "WHERE oi.order_id = ? " +
                "AND oi.quantity > COALESCE(r.returned_qty, 0)";

        String updateOrderSql =
                "UPDATE orders SET status = 'Returned' " +
                "WHERE order_id = ? AND status <> 'Cancelled'";

        try (Connection conn = DBConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                int purchasedQuantity;
                int variantId;
                int orderId;
                String orderStatus;

                // Lock the order item while processing the return
                try (PreparedStatement stmt =
                             conn.prepareStatement(lockSql)) {

                    stmt.setInt(1, orderItemId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Order item not found.");
                        }

                        purchasedQuantity = rs.getInt("quantity");
                        variantId = rs.getInt("variant_id");
                        orderStatus = rs.getString("status");
                        orderId = rs.getInt("order_id");
                    }
                }

                if ("Cancelled".equalsIgnoreCase(orderStatus)) {
                    throw new SQLException(
                            "Items from cancelled orders cannot be returned.");
                }

                // Calculate previously approved returns
                int alreadyReturned;

                try (PreparedStatement stmt =
                             conn.prepareStatement(returnedSql)) {

                    stmt.setInt(1, orderItemId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        alreadyReturned = rs.getInt(1);
                    }
                }

                int remaining =
                        purchasedQuantity - alreadyReturned;

                if (returnQuantity > remaining) {
                    throw new SQLException(
                            "Return quantity exceeds the remaining " +
                            "returnable quantity of " + remaining + ".");
                }

                // Save the return record
                try (PreparedStatement stmt =
                             conn.prepareStatement(insertSql)) {

                    stmt.setInt(1, orderItemId);
                    stmt.setInt(2, returnQuantity);
                    stmt.setInt(3, returnQuantity);
                    stmt.setString(4, reason.trim());

                    stmt.executeUpdate();
                }

                // Restore stock
                try (PreparedStatement stmt =
                             conn.prepareStatement(stockSql)) {

                    stmt.setInt(1, returnQuantity);
                    stmt.setInt(2, variantId);

                    if (stmt.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not restore product stock.");
                    }
                }

                // Check whether any order items remain unreturned
                boolean allItemsReturned;

                try (PreparedStatement stmt =
                             conn.prepareStatement(remainingItemsSql)) {

                    stmt.setInt(1, orderId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        allItemsReturned = rs.getInt(1) == 0;
                    }
                }

                // Mark the order Returned only when every item is returned
                if (allItemsReturned) {
                    try (PreparedStatement stmt =
                                 conn.prepareStatement(updateOrderSql)) {

                        stmt.setInt(1, orderId);
                        stmt.executeUpdate();
                    }
                }

                conn.commit();

            } catch (SQLException | RuntimeException ex) {

                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }

                throw ex;

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
