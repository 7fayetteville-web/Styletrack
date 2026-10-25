
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OnlineOrderDAO {

    public static class OnlineOrder {
        public int orderId;
        public String customerName;
        public Timestamp orderDate;
        public String orderType;
        public String status;
        public double total;

        public OnlineOrder(
                int orderId,
                String customerName,
                Timestamp orderDate,
                String orderType,
                String status,
                double total) {

            this.orderId = orderId;
            this.customerName = customerName;
            this.orderDate = orderDate;
            this.orderType = orderType;
            this.status = status;
            this.total = total;
        }
    }

    // Retrieve all orders
    public List<OnlineOrder> getAllOrders() throws SQLException {
        List<OnlineOrder> orders = new ArrayList<>();

        String sql =
                "SELECT o.order_id, " +
                "COALESCE(c.customer_name, 'Walk-in Customer') AS customer_name, " +
                "o.order_date, o.order_type, o.status, o.total " +
                "FROM orders o " +
                "LEFT JOIN customers c ON o.customer_id = c.customer_id " +
                "ORDER BY o.order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                orders.add(new OnlineOrder(
                        rs.getInt("order_id"),
                        rs.getString("customer_name"),
                        rs.getTimestamp("order_date"),
                        rs.getString("order_type"),
                        rs.getString("status"),
                        rs.getDouble("total")
                ));
            }
        }

        return orders;
    }

    // Update order status
    public void updateStatus(int orderId, String status)
            throws SQLException {

        String sql =
                "UPDATE orders SET status = ? WHERE order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, orderId);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated == 0) {
                throw new SQLException("Order not found.");
            }
        }
    }
}
