
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ProductManagementGUI extends JFrame {

    private final ProductDAO dao = new ProductDAO();

    private final JTextField nameField = new JTextField(15);
    private final JComboBox<String> categoryBox =
        new JComboBox<>();
    private final JTextField brandField = new JTextField(15);
    private final JTextField priceField = new JTextField(15);
    private final JTextArea descriptionArea = new JTextArea(3, 15);

    private final DefaultTableModel tableModel =
        new DefaultTableModel(
            new String[]{"ID", "Name", "Category",
                         "Brand", "Price", "Description"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    private final JTable table = new JTable(tableModel);
    private Map<Integer, String> categories;

    public ProductManagementGUI() {
        setTitle("StyleTrack - Product Management");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildInterface();
        loadCategories();
        loadProducts();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedProduct();
            }
        });
    }

    private void buildInterface() {
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder(
            "Product Details"
        ));

        form.add(new JLabel("Product Name:"));
        form.add(nameField);

        form.add(new JLabel("Category:"));
        form.add(categoryBox);

        form.add(new JLabel("Brand:"));
        form.add(brandField);

        form.add(new JLabel("Price:"));
        form.add(priceField);

        form.add(new JLabel("Description:"));
        form.add(new JScrollPane(descriptionArea));

        JButton addButton = new JButton("Add Product");
        JButton updateButton = new JButton("Update Product");
        JButton deleteButton = new JButton("Delete Product");
        JButton clearButton = new JButton("Clear");

        addButton.addActionListener(e -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        clearButton.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        table.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        table.setAutoCreateRowSorter(true);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadCategories() {
        try {
            categories = dao.getCategories();
            categoryBox.removeAllItems();

            for (String category : categories.values()) {
                categoryBox.addItem(category);
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadProducts() {
        try {
            tableModel.setRowCount(0);

            List<Product> products = dao.getAllProducts();

            for (Product p : products) {
                tableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getProductName(),
                    categories.get(p.getCategoryId()),
                    p.getBrand(),
                    p.getPrice(),
                    p.getDescription()
                });
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private Product readForm(int id) {
        if (categoryBox.getSelectedItem() == null) {
            throw new IllegalArgumentException(
                "Please create a category first."
            );
        }

        String name = nameField.getText().trim();
        String brand = brandField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException(
                "Enter a product name."
            );
        }

        double price;
        try {
            price = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                "Enter a valid price, for example 250.00."
            );
        }

        String selectedCategory =
            (String) categoryBox.getSelectedItem();

        int categoryId = -1;
        for (Map.Entry<Integer, String> entry
                : categories.entrySet()) {
            if (entry.getValue().equals(selectedCategory)) {
                categoryId = entry.getKey();
                break;
            }
        }

        if (categoryId == -1) {
            throw new IllegalArgumentException(
                "Please select a valid category."
            );
        }

        return new Product(
            id, name, categoryId, brand, price, description
        );
    }

    private void addProduct() {
        try {
            Product p = readForm(0);
            dao.addProduct(p);

            JOptionPane.showMessageDialog(
                this, "Product added successfully!"
            );

            clearForm();
            loadProducts();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateProduct() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Select a product to update."
            );
            return;
        }

        try {
            int modelRow = table.convertRowIndexToModel(row);
            int id = (int) tableModel.getValueAt(modelRow, 0);

            Product p = readForm(id);
            dao.updateProduct(p);

            JOptionPane.showMessageDialog(
                this, "Product updated successfully!"
            );

            clearForm();
            loadProducts();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteProduct() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Select a product to delete."
            );
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this product?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int modelRow = table.convertRowIndexToModel(row);
            int id = (int) tableModel.getValueAt(modelRow, 0);

            dao.deleteProduct(id);

            JOptionPane.showMessageDialog(
                this, "Product deleted successfully!"
            );

            clearForm();
            loadProducts();

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showSelectedProduct() {
        int row = table.getSelectedRow();

        if (row == -1) {
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);

        nameField.setText(
            String.valueOf(tableModel.getValueAt(modelRow, 1))
        );

        categoryBox.setSelectedItem(
            tableModel.getValueAt(modelRow, 2)
        );

        brandField.setText(
            String.valueOf(tableModel.getValueAt(modelRow, 3))
        );

        priceField.setText(
            String.valueOf(tableModel.getValueAt(modelRow, 4))
        );

        Object description =
            tableModel.getValueAt(modelRow, 5);

        descriptionArea.setText(
            description == null ? "" : description.toString()
        );
    }

    private void clearForm() {
        table.clearSelection();
        nameField.setText("");
        brandField.setText("");
        priceField.setText("");
        descriptionArea.setText("");
        if (categoryBox.getItemCount() > 0) {
            categoryBox.setSelectedIndex(0);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(
            this,
            "Database error: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ProductManagementGUI gui =
                new ProductManagementGUI();
            gui.setVisible(true);
        });
    }
}
