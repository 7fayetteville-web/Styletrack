
import java.sql.*;
import java.util.*;

public class ProductVariantDAO {

    public Map<Integer, String> getProducts() throws SQLException {
        Map<Integer, String> products = new LinkedHashMap<>();

        String sql =
            "SELECT product_id, product_name FROM products ORDER BY product_name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.put(
                    rs.getInt("product_id"),
                    rs.getString("product_name")
                );
            }
        }
        return products;
    }

    public List<Object[]> getAllVariants() throws SQLException {
        List<Object[]> list = new ArrayList<>();

        String sql = """
            SELECT v.variant_id, p.product_name,
                   v.product_id, v.size, v.color, v.stock_quantity
            FROM product_variants v
            JOIN products p ON v.product_id = p.product_id
            ORDER BY p.product_name, v.size, v.color
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("variant_id"),
                    rs.getString("product_name"),
                    rs.getInt("product_id"),
                    rs.getString("size"),
                    rs.getString("color"),
                    rs.getInt("stock_quantity")
                });
            }
        }
        return list;
    }

    public void addVariant(int productId, String size,
                           String color, int stock)
            throws SQLException {

        String sql = """
            INSERT INTO product_variants
                (product_id, size, color, stock_quantity)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, size);
            ps.setString(3, color);
            ps.setInt(4, stock);
            ps.executeUpdate();
        }
    }

    public void updateVariant(int id, int productId,
                              String size, String color,
                              int stock) throws SQLException {

        String sql = """
            UPDATE product_variants
            SET product_id = ?, size = ?,
                color = ?, stock_quantity = ?
            WHERE variant_id = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, size);
            ps.setString(3, color);
            ps.setInt(4, stock);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void deleteVariant(int id) throws SQLException {
        String sql =
            "DELETE FROM product_variants WHERE variant_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
