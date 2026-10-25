
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public class CustomerManagementGUI extends JFrame {

    private final CustomerDAO dao = new CustomerDAO();

    private final JTextField nameField = new JTextField(15);
    private final JTextField phoneField = new JTextField(15);
    private final JTextField emailField = new JTextField(15);
    private final JTextField addressField = new JTextField(15);

    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"ID", "Name", "Phone", "Email", "Address"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(model);

    public CustomerManagementGUI() {
        setTitle("StyleTrack - Customer Management");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.add(new JLabel("Customer Name:"));
        form.add(nameField);
        form.add(new JLabel("Phone:"));
        form.add(phoneField);
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Address:"));
        form.add(addressField);

        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");

        JPanel buttons = new JPanel();
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);

        add(form, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addCustomer());
        updateButton.addActionListener(e -> updateCustomer());
        deleteButton.addActionListener(e -> deleteCustomer());
        clearButton.addActionListener(e -> clearFields());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();

                if (row >= 0) {
                    nameField.setText(model.getValueAt(row, 1).toString());
                    phoneField.setText(model.getValueAt(row, 2).toString());
                    emailField.setText(model.getValueAt(row, 3).toString());
                    addressField.setText(model.getValueAt(row, 4).toString());
                }
            }
        });

        loadCustomers();
    }

    private void loadCustomers() {
        try {
            model.setRowCount(0);

            for (Customer c : dao.getAllCustomers()) {
                model.addRow(new Object[]{
                    c.getCustomerId(),
                    c.getCustomerName(),
                    c.getPhone(),
                    c.getEmail(),
                    c.getAddress()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private boolean validateName() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Please enter the customer name."
            );
            return false;
        }
        return true;
    }

    private Customer readForm(int id) {
        return new Customer(
            id,
            nameField.getText().trim(),
            phoneField.getText().trim(),
            emailField.getText().trim(),
            addressField.getText().trim()
        );
    }

    private void addCustomer() {
        if (!validateName()) return;

        try {
            dao.addCustomer(readForm(0));
            loadCustomers();
            clearFields();
            JOptionPane.showMessageDialog(this, "Customer added!");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateCustomer() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }

        if (!validateName()) return;

        try {
            int id = Integer.parseInt(model.getValueAt(row, 0).toString());
            dao.updateCustomer(readForm(id));
            loadCustomers();
            clearFields();
            JOptionPane.showMessageDialog(this, "Customer updated!");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteCustomer() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this customer?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            int id = Integer.parseInt(model.getValueAt(row, 0).toString());
            dao.deleteCustomer(id);
            loadCustomers();
            clearFields();
            JOptionPane.showMessageDialog(this, "Customer deleted!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Cannot delete this customer if existing orders use it.\n"
                    + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void clearFields() {
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");
        table.clearSelection();
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
        SwingUtilities.invokeLater(() ->
            new CustomerManagementGUI().setVisible(true)
        );
    }
}
    