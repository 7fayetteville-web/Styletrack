
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class POSDAO {

    public static class VariantOption {
        public int id;
        public String label;
        public double price;
        public int stock;

        public VariantOption(int id, String label, double price, int stock) {
            this.id = id;
            this.label = label;
            this.price = price;
            this.stock = stock;
        }

        @Override
        public String toString() {
            return label + " | PHP "
                    + String.format("%.2f", price)
                    + " | Stock: " + stock;
        }
    }

    public static class SaleItem {
        public int variantId;
        public int quantity;
        public double price;

        public SaleItem(int variantId, int quantity, double price) {
            this.variantId = variantId;
            this.quantity = quantity;
            this.price = price;
        }
    }

    public List<VariantOption> getAvailableVariants() throws SQLException {
        List<VariantOption> list = new ArrayList<>();

        String sql =
            "SELECT v.variant_id, p.product_name, v.size, v.color, " +
            "p.price, v.stock_quantity " +
            "FROM product_variants v " +
            "JOIN products p ON v.product_id = p.product_id " +
            "WHERE v.stock_quantity > 0 " +
            "ORDER BY p.product_name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String label =
                    rs.getString("product_name") + " - " +
                    rs.getString("size") + " - " +
                    rs.getString("color");

                list.add(new VariantOption(
                    rs.getInt("variant_id"),
                    label,
                    rs.getDouble("price"),
                    rs.getInt("stock_quantity")
                ));
            }
        }

        return list;
    }

    public void checkout(
            List<SaleItem> cart,
            double discount,
            double cashReceived) throws SQLException {

        if (cart == null || cart.isEmpty()) {
            throw new SQLException("The cart is empty.");
        }

        if (!Double.isFinite(discount) ||
                !Double.isFinite(cashReceived) ||
                discount < 0 || cashReceived < 0) {
            throw new SQLException("Invalid payment amount.");
        }

        double subtotal = 0;

        for (SaleItem item : cart) {
            if (item.quantity <= 0 || item.price < 0 ||
                    !Double.isFinite(item.price)) {
                throw new SQLException("Invalid item quantity or price.");
            }

            subtotal += item.price * item.quantity;
        }

        double total = subtotal - discount;

        if (discount > subtotal) {
            throw new SQLException("Discount cannot exceed subtotal.");
        }

        if (cashReceived < total) {
            throw new SQLException("Cash received is insufficient.");
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                String orderSql =
                    "INSERT INTO orders " +
                    "(customer_id, order_type, status, subtotal, discount, total) " +
                    "VALUES (NULL, 'Walk-in', 'Completed', ?, ?, ?)";

                int orderId;

                try (PreparedStatement ps = con.prepareStatement(
                        orderSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setDouble(1, subtotal);
                    ps.setDouble(2, discount);
                    ps.setDouble(3, total);

                    int inserted = ps.executeUpdate();

                    if (inserted != 1) {
                        throw new SQLException("Order was not saved.");
                    }

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Could not get order ID.");
                        }

                        orderId = keys.getInt(1);
                    }
                }

                String stockSql =
                    "UPDATE product_variants " +
                    "SET stock_quantity = stock_quantity - ? " +
                    "WHERE variant_id = ? AND stock_quantity >= ?";

                String itemSql =
                    "INSERT INTO order_items " +
                    "(order_id, variant_id, quantity, unit_price) " +
                    "VALUES (?, ?, ?, ?)";

                for (SaleItem item : cart) {
                    try (PreparedStatement ps =
                            con.prepareStatement(stockSql)) {

                        ps.setInt(1, item.quantity);
                        ps.setInt(2, item.variantId);
                        ps.setInt(3, item.quantity);

                        if (ps.executeUpdate() != 1) {
                            throw new SQLException(
                                "Not enough stock for variant ID " +
                                item.variantId +
                                ". Refresh inventory and try again."
                            );
                        }
                    }

                    try (PreparedStatement ps =
                            con.prepareStatement(itemSql)) {

                        ps.setInt(1, orderId);
                        ps.setInt(2, item.variantId);
                        ps.setInt(3, item.quantity);
                        ps.setDouble(4, item.price);

                        ps.executeUpdate();
                    }
                }

                con.commit();

            } catch (SQLException ex) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }
                throw ex;

            } catch (RuntimeException ex) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }
                throw ex;

            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}
