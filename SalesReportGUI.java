
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SalesReportGUI extends JFrame {

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{
                    "Order ID",
                    "Date",
                    "Order Type",
                    "Status",
                    "Subtotal",
                    "Discount",
                    "Total"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable reportTable = new JTable(model);

    private final JLabel totalSalesLabel =
            new JLabel("Total Sales: PHP 0.00");

    private final JLabel ordersLabel =
            new JLabel("Completed Orders: 0");

    private final JLabel averageLabel =
            new JLabel("Average Sale: PHP 0.00");

    public SalesReportGUI() {
        setTitle("StyleTrack - Sales Reports");
        setSize(1000, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel(
                "Sales Reports", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        topPanel.add(title, BorderLayout.CENTER);

        JPanel summaryPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 30, 10));

        summaryPanel.add(totalSalesLabel);
        summaryPanel.add(ordersLabel);
        summaryPanel.add(averageLabel);

        JButton refreshButton = new JButton("Refresh Reports");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(summaryPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> loadReports());

        loadReports();
    }

    private void loadReports() {
        String sql =
                "SELECT order_id, order_date, order_type, status, " +
                "subtotal, discount, total " +
                "FROM orders " +
                "ORDER BY order_date DESC";

        String summarySql =
                "SELECT COUNT(*) AS completed_orders, " +
                "COALESCE(SUM(total), 0) AS total_sales, " +
                "COALESCE(AVG(total), 0) AS average_sale " +
                "FROM orders WHERE status = 'Completed'";

        try (Connection conn = DBConnection.getConnection()) {

            model.setRowCount(0);

            try (PreparedStatement stmt =
                         conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("order_id"),
                            rs.getTimestamp("order_date"),
                            rs.getString("order_type"),
                            rs.getString("status"),
                            String.format("%.2f",
                                    rs.getDouble("subtotal")),
                            String.format("%.2f",
                                    rs.getDouble("discount")),
                            String.format("%.2f",
                                    rs.getDouble("total"))
                    });
                }
            }

            try (PreparedStatement stmt =
                         conn.prepareStatement(summarySql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    int count = rs.getInt("completed_orders");
                    double sales = rs.getDouble("total_sales");
                    double average = rs.getDouble("average_sale");

                    totalSalesLabel.setText(String.format(
                            "Total Sales: PHP %.2f", sales));

                    ordersLabel.setText(
                            "Completed Orders: " + count);

                    averageLabel.setText(String.format(
                            "Average Sale: PHP %.2f", average));
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load sales reports:\n"
                            + ex.getMessage(),
                    "StyleTrack",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new SalesReportGUI().setVisible(true));
    }
}
