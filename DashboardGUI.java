
import javax.swing.*;
import java.awt.*;

public class DashboardGUI extends JFrame {

    private final String username;
    private final String role;

    public DashboardGUI(String username, String role) {
        this.username = username;
        this.role = role;

        setTitle("StyleTrack - " + role + " Dashboard");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        panel.add(new JLabel(
            "Welcome, " + username + " (" + role + ")",
            SwingConstants.CENTER
        ));

        if (role.equals("Admin")) {
            JButton productsButton =
                new JButton("Product Management");
            JButton inventoryButton =
                new JButton("Inventory Management");
            JButton customersButton = new JButton("Customer Management");
            JButton onlineOrdersButton = new JButton("Online Orders");

                onlineOrdersButton.addActionListener(e -> {
                new OnlineOrderGUI().setVisible(true);
                });

                customersButton.addActionListener(e ->
                    new CustomerManagementGUI().setVisible(true)
                );

                 productsButton.addActionListener(e ->
                    new ProductManagementGUI().setVisible(true)
                );
                
                inventoryButton.addActionListener(e ->
                    new InventoryGUI().setVisible(true)
                );
                
            panel.add(customersButton);
            panel.add(productsButton);
            panel.add(inventoryButton);
            panel.add(onlineOrdersButton);
        } else if (role.equals("Cashier")) {
            panel.add(new JLabel(
                "Sales features will be added in the next step.",
                SwingConstants.CENTER
            ));
                JButton posButton = new JButton("Point of Sale");
                posButton.addActionListener(e ->
                    new POSGUI().setVisible(true)
                );

            panel.add(posButton);
        }

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginGUI().setVisible(true);
        });

        panel.add(logoutButton);
        add(panel);
    }
}
