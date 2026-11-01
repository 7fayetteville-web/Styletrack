
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.Map;

public class InventoryGUI extends JFrame {

    private final ProductVariantDAO dao = new ProductVariantDAO();

    private final JComboBox<String> productBox = new JComboBox<>();
    private final JTextField sizeField = new JTextField();
    private final JTextField colorField = new JTextField();
    private final JTextField stockField = new JTextField();

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[]{"ID", "Product", "Product ID",
                         "Size", "Color", "Stock", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

    private final JTable table = new JTable(model);
    private Map<Integer, String> products;

    public InventoryGUI() {
        setTitle("StyleTrack - Inventory Management");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        loadProducts();
        loadVariants();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectVariant();
            }
        });
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder(
            "Variant Details"
        ));

        form.add(new JLabel("Product:"));
        form.add(productBox);
        form.add(new JLabel("Size:"));
        form.add(sizeField);
        form.add(new JLabel("Color:"));
        form.add(colorField);
        form.add(new JLabel("Stock Quantity:"));
        form.add(stockField);

        JButton add = new JButton("Add");
        JButton update = new JButton("Update");
        JButton delete = new JButton("Delete");
        JButton clear = new JButton("Clear");

        add.addActionListener(e -> addVariant());
        update.addActionListener(e -> updateVariant());
        delete.addActionListener(e -> deleteVariant());
        clear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel();
        buttons.add(add);
        buttons.add(update);
        buttons.add(delete);
        buttons.add(clear);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadProducts() {
        try {
            products = dao.getProducts();
            productBox.removeAllItems();

            for (String name : products.values()) {
                productBox.addItem(name);
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private int selectedProductId() {
        String selected = (String) productBox.getSelectedItem();

        if (selected == null) {
            throw new IllegalArgumentException(
                "Add a product before creating a variant."
            );
        }

        for (Map.Entry<Integer, String> entry : products.entrySet()) {
            if (entry.getValue().equals(selected)) {
                return entry.getKey();
            }
        }

        throw new IllegalArgumentException("Select a valid product.");
    }

    private int readStock() {
        try {
            int stock = Integer.parseInt(stockField.getText().trim());

            if (stock < 0) {
                throw new IllegalArgumentException(
                    "Stock cannot be negative."
                );
            }
            return stock;

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                "Enter a valid whole number for stock."
            );
        }
    }

    private void validateFields() {
        if (sizeField.getText().trim().isEmpty()
                || colorField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Enter both size and color."
            );
        }
    }

    private void loadVariants() {
        try {
            model.setRowCount(0);

            for (Object[] v : dao.getAllVariants()) {
                int stock = (int) v[5];
                String status = stock <= 5 ? "LOW STOCK" : "In Stock";

                model.addRow(new Object[]{
                    v[0], v[1], v[2], v[3], v[4], stock, status
                });
            }
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void addVariant() {
        try {
            validateFields();

            dao.addVariant(
                selectedProductId(),
                sizeField.getText().trim(),
                colorField.getText().trim(),
                readStock()
            );

            JOptionPane.showMessageDialog(this, "Variant added!");
            clearForm();
            loadVariants();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void updateVariant() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Select a variant to update."
            );
            return;
        }

        try {
            validateFields();
            int id = (int) model.getValueAt(row, 0);

            dao.updateVariant(
                id,
                selectedProductId(),
                sizeField.getText().trim(),
                colorField.getText().trim(),
                readStock()
            );

            JOptionPane.showMessageDialog(this, "Variant updated!");
            clearForm();
            loadVariants();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void deleteVariant() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Select a variant to delete."
            );
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
            this, "Delete this variant?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int id = (int) model.getValueAt(row, 0);
            dao.deleteVariant(id);

            JOptionPane.showMessageDialog(this, "Variant deleted!");
            clearForm();
            loadVariants();

        } catch (SQLException ex) {
            error(ex);
        }
    }

    private void selectVariant() {
        int row = table.getSelectedRow();

        if (row == -1) {
            return;
        }

        productBox.setSelectedItem(model.getValueAt(row, 1));
        sizeField.setText(String.valueOf(model.getValueAt(row, 3)));
        colorField.setText(String.valueOf(model.getValueAt(row, 4)));
        stockField.setText(String.valueOf(model.getValueAt(row, 5)));
    }

    private void clearForm() {
        table.clearSelection();
        sizeField.setText("");
        colorField.setText("");
        stockField.setText("");

        if (productBox.getItemCount() > 0) {
            productBox.setSelectedIndex(0);
        }
    }

    private void error(SQLException ex) {
        JOptionPane.showMessageDialog(
            this, "Database error: " + ex.getMessage(),
            "Error", JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new InventoryGUI().setVisible(true)
        );
    }
}
