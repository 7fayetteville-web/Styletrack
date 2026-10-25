
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginGUI extends JFrame {

    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField =
        new JPasswordField(15);

    public LoginGUI() {
        setTitle("StyleTrack - Login");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton loginButton = new JButton("Login");
        JButton clearButton = new JButton("Clear");

        panel.add(loginButton);
        panel.add(clearButton);

        loginButton.addActionListener(e -> login());
        clearButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
        });

        add(panel);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        String sql = """
            SELECT user_id, username, role
            FROM users
            WHERE username = ? AND password = ?
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String loggedInUser = rs.getString("username");

                    JOptionPane.showMessageDialog(
                        this, "Welcome, " + loggedInUser + "!"
                    );

                    dispose();
                    new DashboardGUI(loggedInUser, role).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Invalid username or password.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this, "Database error: " + ex.getMessage()
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new LoginGUI().setVisible(true)
        );
    }
}
