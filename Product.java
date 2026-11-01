
public class Product {
    private int productId;
    private String productName;
    private int categoryId;
    private String brand;
    private double price;
    private String description;

    public Product(int productId, String productName,
                   int categoryId, String brand,
                   double price, String description) {
        this.productId = productId;
        setProductName(productName);
        this.categoryId = categoryId;
        this.brand = brand;
        setPrice(price);
        this.description = description;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Product name is required."
            );
        }
        this.productName = productName.trim();
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException(
                "Price must be a valid non-negative number."
            );
        }
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
