
import java.sql.*;
import java.util.*;

public class ProductDAO {

    public Map<Integer, String> getCategories()
            throws SQLException {
        Map<Integer, String> categories = new LinkedHashMap<>();

        String sql =
            "SELECT category_id, category_name FROM categories";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categories.put(
                    rs.getInt("category_id"),
                    rs.getString("category_name")
                );
            }
        }

        return categories;
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();

        String sql = """
            SELECT product_id, product_name, category_id,
                   brand, price, description
            FROM products
            ORDER BY product_id
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("category_id"),
                    rs.getString("brand"),
                    rs.getDouble("price"),
                    rs.getString("description")
                ));
            }
        }

        return products;
    }

    public void addProduct(Product p) throws SQLException {
        String sql = """
            INSERT INTO products
            (product_name, category_id, brand, price, description)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setProductParameters(ps, p);
            ps.executeUpdate();
        }
    }

    public void updateProduct(Product p) throws SQLException {
        String sql = """
            UPDATE products
            SET product_name = ?, category_id = ?,
                brand = ?, price = ?, description = ?
            WHERE product_id = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setProductParameters(ps, p);
            ps.setInt(6, p.getProductId());
            ps.executeUpdate();
        }
    }

    public void deleteProduct(int productId) throws SQLException {
        String sql =
            "DELETE FROM products WHERE product_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    private void setProductParameters(
            PreparedStatement ps, Product p)
            throws SQLException {
        ps.setString(1, p.getProductName());
        ps.setInt(2, p.getCategoryId());
        ps.setString(3, p.getBrand());
        ps.setDouble(4, p.getPrice());
        ps.setString(5, p.getDescription());
    }
}
