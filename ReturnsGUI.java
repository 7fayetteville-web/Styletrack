
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ReturnsGUI extends JFrame {

    private final ReturnDAO dao = new ReturnDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{
                    "Order Item ID",
                    "Order ID",
                    "Product",
                    "Size",
                    "Color",
                    "Purchased",
                    "Returned",
                    "Remaining",
                    "Unit Price"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);
    private final JTextField quantityField = new JTextField("1", 6);
    private final JTextField reasonField = new JTextField(22);

    public ReturnsGUI() {
        setTitle("StyleTrack - Returns and Exchanges");
        setSize(1050, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel(
                "Returns and Exchanges", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        topPanel.add(title, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        inputPanel.add(new JLabel("Return Quantity:"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Reason:"));
        inputPanel.add(reasonField);

        JButton processButton = new JButton("Process Return");
        JButton refreshButton = new JButton("Refresh Items");

        inputPanel.add(processButton);
        inputPanel.add(refreshButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        processButton.addActionListener(e -> processSelectedReturn());
        refreshButton.addActionListener(e -> loadItems());

        loadItems();
    }

    // Load purchased items that can still be returned
    private void loadItems() {
        try {
            List<ReturnDAO.ReturnableItem> items =
                    dao.getReturnableItems();

            model.setRowCount(0);

            for (ReturnDAO.ReturnableItem item : items) {
                model.addRow(new Object[]{
                        item.orderItemId,
                        item.orderId,
                        item.productName,
                        item.size,
                        item.color,
                        item.purchasedQuantity,
                        item.alreadyReturned,
                        item.getRemainingQuantity(),
                        String.format("%.2f", item.unitPrice)
                });
            }

        } catch (SQLException ex) {
            showError("Could not load returnable items:\n"
                    + ex.getMessage());
        }
    }

    // Process the selected return
    private void processSelectedReturn() {
        int row = table.getSelectedRow();

        if (row < 0) {
            showError("Please select a purchased item first.");
            return;
        }

        int orderItemId =
                Integer.parseInt(model.getValueAt(row, 0).toString());

        int remaining =
                Integer.parseInt(model.getValueAt(row, 7).toString());

        int quantity;

        try {
            quantity = Integer.parseInt(
                    quantityField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Enter a valid whole-number quantity.");
            return;
        }

        String reason = reasonField.getText().trim();

        if (quantity <= 0 || quantity > remaining) {
            showError("Quantity must be between 1 and "
                    + remaining + ".");
            return;
        }

        if (reason.isEmpty()) {
            showError("Please enter a reason for the return.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Process return of " + quantity +
                        " item(s)?\n\n" +
                        "Stock will be restored after confirmation.",
                "Confirm Return",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            dao.processReturn(orderItemId, quantity, reason);

            JOptionPane.showMessageDialog(
                    this,
                    "Return recorded successfully!\n" +
                    "Inventory stock has been restored.",
                    "Return Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );

            quantityField.setText("1");
            reasonField.setText("");
            loadItems();

        } catch (SQLException ex) {
            showError("Could not process return:\n"
                    + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "StyleTrack",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new ReturnsGUI().setVisible(true));
    }
}
