
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class POSGUI extends JFrame {

    private final POSDAO dao = new POSDAO();

    private final JComboBox<POSDAO.VariantOption> productBox =
            new JComboBox<>();

    private final JLabel stockLabel =
            new JLabel("Available Stock: --");

    private final JTextField quantityField = new JTextField("1", 5);
    private final JTextField discountField = new JTextField("0.00", 8);
    private final JTextField cashField = new JTextField("0.00", 8);

    private final JLabel subtotalLabel =
            new JLabel("Subtotal: PHP 0.00");
    private final JLabel totalLabel =
            new JLabel("Total: PHP 0.00");
    private final JLabel changeLabel =
            new JLabel("Change: PHP 0.00");

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Variant ID", "Product", "Quantity",
                    "Price", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable cartTable = new JTable(model);
    private final List<POSDAO.SaleItem> cart = new ArrayList<>();

    private final JButton addButton = new JButton("Add to Cart");
    private final JButton removeButton = new JButton("Remove Selected");
    private final JButton checkoutButton = new JButton("Complete Sale");
    private final JButton refreshButton = new JButton("Refresh Products");

    public POSGUI() {
        setTitle("StyleTrack - Point of Sale");
        setSize(1100, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Product selection panel
        JPanel topPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 10));

        topPanel.add(new JLabel("Clothing Variant:"));

        productBox.setPreferredSize(new Dimension(400, 28));
        topPanel.add(productBox);

        topPanel.add(stockLabel);

        topPanel.add(new JLabel("Quantity:"));
        topPanel.add(quantityField);
        topPanel.add(addButton);
        topPanel.add(refreshButton);

        // Payment and total information
        JPanel paymentPanel = new JPanel(
                new GridLayout(3, 1, 5, 5));

        JPanel amountPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        amountPanel.add(new JLabel("Discount (PHP):"));
        amountPanel.add(discountField);
        amountPanel.add(new JLabel("Cash Received (PHP):"));
        amountPanel.add(cashField);

        JPanel totalsPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        totalsPanel.add(subtotalLabel);
        totalsPanel.add(totalLabel);
        totalsPanel.add(changeLabel);

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT));

        buttonPanel.add(removeButton);
        buttonPanel.add(checkoutButton);

        paymentPanel.add(amountPanel);
        paymentPanel.add(totalsPanel);
        paymentPanel.add(buttonPanel);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(cartTable), BorderLayout.CENTER);
        add(paymentPanel, BorderLayout.SOUTH);

        // Button actions
        addButton.addActionListener(e -> addToCart());
        removeButton.addActionListener(e -> removeSelected());
        refreshButton.addActionListener(e -> loadProducts());
        checkoutButton.addActionListener(e -> completeSale());

        // Update stock when a product is selected
        productBox.addActionListener(e -> updateStockLabel());

        // Recalculate when Enter is pressed
        discountField.addActionListener(e -> updateTotals());
        cashField.addActionListener(e -> updateTotals());

        // Recalculate when leaving the fields
        discountField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateTotals();
            }
        });

        cashField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateTotals();
            }
        });

        loadProducts();
        updateStockLabel();
        updateTotals();
    }

    // Display the stock of the selected product
    private void updateStockLabel() {
        POSDAO.VariantOption selected =
                (POSDAO.VariantOption) productBox.getSelectedItem();

        if (selected == null) {
            stockLabel.setText("Available Stock: --");
        } else {
            stockLabel.setText(
                    "Available Stock: " + selected.stock + " pieces");
        }
    }

    // Load available clothing variants from MySQL
    private void loadProducts() {
        try {
            productBox.removeAllItems();

            List<POSDAO.VariantOption> products =
                    dao.getAvailableVariants();

            for (POSDAO.VariantOption product : products) {
                productBox.addItem(product);
            }

            updateStockLabel();

            if (products.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No products in stock. Add products and variants first.",
                        "No Products",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException ex) {
            showError("Could not load products:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Add a selected product to the cart
    private void addToCart() {
        POSDAO.VariantOption selected =
                (POSDAO.VariantOption) productBox.getSelectedItem();

        if (selected == null) {
            showError("Please select a clothing item.");
            return;
        }

        try {
            int quantity = Integer.parseInt(
                    quantityField.getText().trim());

            if (quantity <= 0) {
                showError("Quantity must be greater than zero.");
                return;
            }

            POSDAO.SaleItem existing = null;
            int currentQuantity = 0;

            for (POSDAO.SaleItem item : cart) {
                if (item.variantId == selected.id) {
                    existing = item;
                    currentQuantity = item.quantity;
                    break;
                }
            }

            if (currentQuantity + quantity > selected.stock) {
                showError(
                        "Not enough stock available.\n" +
                        "Available stock: " + selected.stock);
                return;
            }

            if (existing != null) {
                existing.quantity += quantity;
            } else {
                cart.add(new POSDAO.SaleItem(
                        selected.id, quantity, selected.price));
            }

            refreshCart();
            quantityField.setText("1");

        } catch (NumberFormatException ex) {
            showError("Please enter a valid whole-number quantity.");
        }
    }

    // Display cart items in the table
    private void refreshCart() {
        model.setRowCount(0);

        for (POSDAO.SaleItem item : cart) {
            String productName = "Variant ID " + item.variantId;

            for (int i = 0; i < productBox.getItemCount(); i++) {
                POSDAO.VariantOption option =
                        productBox.getItemAt(i);

                if (option.id == item.variantId) {
                    productName = option.label;
                    break;
                }
            }

            double lineTotal = item.price * item.quantity;

            model.addRow(new Object[]{
                    item.variantId,
                    productName,
                    item.quantity,
                    String.format("%.2f", item.price),
                    String.format("%.2f", lineTotal)
            });
        }

        updateTotals();
    }

    // Calculate subtotal
    private double getSubtotal() {
        double subtotal = 0;

        for (POSDAO.SaleItem item : cart) {
            subtotal += item.price * item.quantity;
        }

        return subtotal;
    }

    // Calculate discount, total, and change
    private void updateTotals() {
        double subtotal = getSubtotal();

        subtotalLabel.setText(
                String.format("Subtotal: PHP %.2f", subtotal));

        try {
            double discount = Double.parseDouble(
                    discountField.getText().trim());

            double cash = Double.parseDouble(
                    cashField.getText().trim());

            if (!Double.isFinite(discount)
                    || !Double.isFinite(cash)
                    || discount < 0
                    || discount > subtotal
                    || cash < 0) {

                totalLabel.setText("Total: Check amounts");
                changeLabel.setText("Change: --");
                return;
            }

            double total = subtotal - discount;

            totalLabel.setText(
                    String.format("Total: PHP %.2f", total));

            if (cash >= total) {
                changeLabel.setText(
                        String.format("Change: PHP %.2f", cash - total));
            } else {
                changeLabel.setText("Change: Insufficient cash");
            }

        } catch (NumberFormatException ex) {
            totalLabel.setText("Total: Enter valid amounts");
            changeLabel.setText("Change: --");
        }
    }

    // Remove selected cart item
    private void removeSelected() {
        int row = cartTable.getSelectedRow();

        if (row < 0) {
            showError("Please select an item in the cart.");
            return;
        }

        cart.remove(row);
        refreshCart();
    }

    // Process payment and save the sale
    private void completeSale() {
        if (cart.isEmpty()) {
            showError("Your cart is empty. Add a product first.");
            return;
        }

        double discount;
        double cash;

        try {
            discount = Double.parseDouble(
                    discountField.getText().trim());

            cash = Double.parseDouble(
                    cashField.getText().trim());

        } catch (NumberFormatException ex) {
            showError("Enter valid numbers for discount and cash.");
            return;
        }

        double subtotal = getSubtotal();

        if (!Double.isFinite(discount)
                || !Double.isFinite(cash)
                || discount < 0
                || discount > subtotal
                || cash < 0) {

            showError("Please check the discount and cash amounts.");
            return;
        }

        double total = subtotal - discount;

        if (cash < total) {
            showError(String.format(
                    "Insufficient cash!%nTotal: PHP %.2f%nCash: PHP %.2f",
                    total, cash));
            return;
        }

        double change = cash - total;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format(
                        "Subtotal: PHP %.2f%n" +
                        "Discount: PHP %.2f%n" +
                        "Total: PHP %.2f%n" +
                        "Cash Received: PHP %.2f%n" +
                        "Change: PHP %.2f%n%n" +
                        "Do you want to complete this sale?",
                        subtotal, discount, total, cash, change),
                "Confirm Sale",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        checkoutButton.setEnabled(false);

        try {
            // Save the order, order items, and stock changes
            dao.checkout(new ArrayList<>(cart), discount, cash);

            JOptionPane.showMessageDialog(
                    this,
                    String.format(
                            "SALE COMPLETED SUCCESSFULLY!%n%n" +
                            "Subtotal: PHP %.2f%n" +
                            "Discount: PHP %.2f%n" +
                            "Total: PHP %.2f%n" +
                            "Cash Received: PHP %.2f%n" +
                            "Change: PHP %.2f",
                            subtotal, discount, total, cash, change),
                    "Sale Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

            cart.clear();
            discountField.setText("0.00");
            cashField.setText("0.00");

            refreshCart();
            loadProducts();

        } catch (SQLException ex) {
            ex.printStackTrace();

            showError(
                    "SALE FAILED!\n\n" +
                    "The sale could not be saved to MySQL.\n\n" +
                    ex.getMessage()
            );

            loadProducts();

        } finally {
            checkoutButton.setEnabled(true);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "StyleTrack POS",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            POSGUI gui = new POSGUI();
            gui.setVisible(true);
        });
    }
}
