
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class OnlineOrderGUI extends JFrame {

    private final OnlineOrderDAO dao = new OnlineOrderDAO();

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{
                    "Order ID",
                    "Customer",
                    "Date",
                    "Order Type",
                    "Status",
                    "Total (PHP)"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable orderTable = new JTable(model);

    private final JComboBox<String> statusBox =
            new JComboBox<>(new String[]{
                    "Pending",
                    "Processing",
                    "Ready",
                    "Completed",
                    "Cancelled",
                    "Returned"
            });

    public OnlineOrderGUI() {
        setTitle("StyleTrack - Online Order Management");
        setSize(950, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Title
        JLabel title = new JLabel(
                "Online Order Management",
                SwingConstants.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );
        topPanel.add(title, BorderLayout.CENTER);

        // Buttons and status selection
        JPanel buttonPanel =
                new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton refreshButton = new JButton("Refresh Orders");
        JButton updateButton = new JButton("Update Status");
        JButton returnButton = new JButton("Return");

        buttonPanel.add(new JLabel("New Status:"));
        buttonPanel.add(statusBox);
        buttonPanel.add(updateButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(returnButton);

        // Layout
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(orderTable), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Button actions
        refreshButton.addActionListener(e -> loadOrders());

        updateButton.addActionListener(e -> updateSelectedOrder());

        returnButton.addActionListener(e -> {
            new ReturnsGUI().setVisible(true);
        });

        // Load orders when the window opens
        loadOrders();
    }

    // Load orders from MySQL
    private void loadOrders() {
        try {
            List<OnlineOrderDAO.OnlineOrder> orders =
                    dao.getAllOrders();

            model.setRowCount(0);

            for (OnlineOrderDAO.OnlineOrder order : orders) {
                model.addRow(new Object[]{
                        order.orderId,
                        order.customerName,
                        order.orderDate,
                        order.orderType,
                        order.status,
                        String.format("%.2f", order.total)
                });
            }

        } catch (SQLException ex) {
            showError("Could not load orders:\n" + ex.getMessage());
        }
    }

    // Update the selected order status
    private void updateSelectedOrder() {
        int row = orderTable.getSelectedRow();

        if (row < 0) {
            showError("Please select an order first.");
            return;
        }

        int orderId = Integer.parseInt(
                model.getValueAt(row, 0).toString()
        );

        String currentStatus =
                String.valueOf(model.getValueAt(row, 4));

        String newStatus =
                String.valueOf(statusBox.getSelectedItem());

        if (newStatus.equals(currentStatus)) {
            JOptionPane.showMessageDialog(
                    this,
                    "The order already has this status."
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Change order #" + orderId +
                        " from " + currentStatus +
                        " to " + newStatus + "?",
                "Confirm Status Update",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            dao.updateStatus(orderId, newStatus);

            JOptionPane.showMessageDialog(
                    this,
                    "Order status updated successfully!"
            );

            loadOrders();

        } catch (SQLException ex) {
            showError("Could not update order:\n" + ex.getMessage());
        }
    }

    // Display an error message
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "StyleTrack",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new OnlineOrderGUI().setVisible(true);
        });
    }
}
