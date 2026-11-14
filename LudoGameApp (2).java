import DBConnection.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class LudoGameApp {

    // Vibrant Ludo Color Palette
    private static final Color RED_COLOR = new Color(231, 76, 60);
    private static final Color GREEN_COLOR = new Color(46, 204, 113);
    private static final Color YELLOW_COLOR = new Color(241, 196, 15);
    private static final Color BLUE_COLOR = new Color(52, 152, 219);
    private static final Color BG_CREAM = new Color(254, 253, 245);

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(LudoGameApp::showLogin);
    }

    private static boolean isValidUsername(String username) {
        if (username == null) return false;
        return username.matches("^[a-zA-Z][a-zA-Z0-9_]{2,19}$");
    }

    private static boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean validateLogin(String username, String password) {
        String sql = "SELECT user_password FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("user_password").equals(password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    } 

    private static boolean registerUser(String username, String password, String email) {
        String sql = "INSERT INTO users (username, user_password, email) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Colorful & Styled Login Window UI
     */
    private static void showLogin() {
        JFrame frame = new JFrame("Ludo Game - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(440, 480);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true); // Enabled maximize / resize

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int barHeight = 8;
                int w = getWidth();
                int q = w / 4;
                g2d.setColor(RED_COLOR); g2d.fillRect(0, 0, q, barHeight);
                g2d.setColor(GREEN_COLOR); g2d.fillRect(q, 0, q, barHeight);
                g2d.setColor(YELLOW_COLOR); g2d.fillRect(q * 2, 0, q, barHeight);
                g2d.setColor(BLUE_COLOR); g2d.fillRect(q * 3, 0, w - (q * 3), barHeight);
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_CREAM);
        mainPanel.setBorder(new EmptyBorder(25, 35, 25, 35));

        JLabel titleLabel = new JLabel("🎲 Ludo Master");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(44, 62, 80));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Please sign in to your account");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(127, 140, 141));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridLayout(4, 1, 5, 10));
        formPanel.setBackground(BG_CREAM);
        formPanel.setMaximumSize(new Dimension(370, 150));

        JTextField userField = new JTextField(15);
        styleTextField(userField);

        JPasswordField passField = new JPasswordField(15);
        styleTextField(passField);

        formPanel.add(createLabeledPanel("Username:", userField));
        formPanel.add(createLabeledPanel("Password:", passField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(BG_CREAM);
        
        JButton loginButton = createStyledButton("Login", BLUE_COLOR);
        JButton signUpButton = createStyledButton("Sign Up", GREEN_COLOR);
        buttonPanel.add(loginButton);
        buttonPanel.add(signUpButton);

        JLabel message = new JLabel(" ", SwingConstants.CENTER);
        message.setFont(new Font("SansSerif", Font.BOLD, 12));
        message.setForeground(RED_COLOR);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(message);

        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Please enter both username and password.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
                );
            } else if (validateLogin(username, password)) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Login successful!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                frame.dispose();
                LudoGame.launch(username);
            } else {
                JOptionPane.showMessageDialog(
                    frame,
                    "Incorrect username or password. Please try again.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE
                );
                passField.setText("");
            }
        });

        signUpButton.addActionListener(e -> {
            frame.dispose();
            showSignUp();
        });

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    /**
     * Colorful & Styled Sign Up Window UI
     */
    private static void showSignUp() {
        JFrame frame = new JFrame("Ludo Game - Sign Up");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(440, 520);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true); // Enabled maximize / resize

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int barHeight = 8;
                int w = getWidth();
                int q = w / 4;
                g2d.setColor(RED_COLOR); g2d.fillRect(0, 0, q, barHeight);
                g2d.setColor(GREEN_COLOR); g2d.fillRect(q, 0, q, barHeight);
                g2d.setColor(YELLOW_COLOR); g2d.fillRect(q * 2, 0, q, barHeight);
                g2d.setColor(BLUE_COLOR); g2d.fillRect(q * 3, 0, w - (q * 3), barHeight);
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_CREAM);
        mainPanel.setBorder(new EmptyBorder(25, 35, 25, 35));

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(44, 62, 80));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridLayout(6, 1, 5, 8));
        formPanel.setBackground(BG_CREAM);
        formPanel.setMaximumSize(new Dimension(370, 200));

        JTextField userField = new JTextField(15);
        JTextField emailField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        styleTextField(userField);
        styleTextField(emailField);
        styleTextField(passField);

        formPanel.add(createLabeledPanel("New username:", userField));
        formPanel.add(createLabeledPanel("Email Address (@gmail.com):", emailField));
        formPanel.add(createLabeledPanel("New password:", passField));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(BG_CREAM);

        JButton registerButton = createStyledButton("Register", GREEN_COLOR);
        JButton backButton = createStyledButton("Back to Login", new Color(127, 140, 141));
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        JLabel message = new JLabel(" ", SwingConstants.CENTER);
        message.setFont(new Font("SansSerif", Font.BOLD, 11));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(message);

        registerButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String email = emailField.getText().trim().toLowerCase();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Please fill in all fields.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
                );
            } else if (!isValidUsername(username)) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Username must be 3-20 characters, start with a letter, and contain only letters, numbers and underscores.",
                    "Invalid Username",
                    JOptionPane.ERROR_MESSAGE
                );
            } else if (!email.endsWith("@gmail.com") || email.equals("@gmail.com")) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Please enter a valid Gmail address.",
                    "Invalid Email",
                    JOptionPane.ERROR_MESSAGE
                );
            } else if (userExists(username)) {
                JOptionPane.showMessageDialog(
                    frame,
                    "This username already exists. Please choose another.",
                    "Username Already Exists",
                    JOptionPane.ERROR_MESSAGE
                );
            } else if (!isValidStrongPassword(password)) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Password must be at least 5 characters and include:\n"
                        + "1 uppercase letter\n"
                        + "1 lowercase letter\n"
                        + "1 number\n"
                        + "1 special character.",
                    "Weak Password",
                    JOptionPane.ERROR_MESSAGE
                );
            } else if (registerUser(username, password, email)) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Registration successful! You can now log in.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                frame.dispose();
                showLogin();
            } else {
                JOptionPane.showMessageDialog(
                    frame,
                    "Registration failed. Please check your database connection.",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        backButton.addActionListener(e -> {
            frame.dispose();
            showLogin();
        });

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static JPanel createLabeledPanel(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.setBackground(BG_CREAM);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(52, 73, 94));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private static void styleTextField(JComponent field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
    }

    private static JButton createStyledButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static boolean isValidStrongPassword(String password) {
        if (password == null || password.length() < 5) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?/~`'\"\\";

        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isUpperCase(ch)) hasUpper = true;
            else if (Character.isLowerCase(ch)) hasLower = true;
            else if (Character.isDigit(ch)) hasDigit = true;
            else if (specialChars.indexOf(ch) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}

/**
 * Custom panel to render the dice face with dots.
 */
class DicePanel extends JPanel {
    int value = 0;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(5, 5, w - 10, h - 10, 15, 15);
        g2d.setColor(new Color(189, 195, 199));
        g2d.drawRoundRect(5, 5, w - 10, h - 10, 15, 15);

        if (value < 1 || value > 6) return;

        g2d.setColor(new Color(44, 62, 80));
        int dotSize = Math.min(w, h) / 6;
        int center = w / 2;
        int left = w / 4;
        int right = w * 3 / 4;
        int top = h / 4;
        int bottom = h * 3 / 4;

        if (value == 1 || value == 3 || value == 5) {
            fillDot(g2d, center - dotSize / 2, center - dotSize / 2, dotSize);
        }
        if (value >= 2) {
            fillDot(g2d, left - dotSize / 2, top - dotSize / 2, dotSize);
            fillDot(g2d, right - dotSize / 2, bottom - dotSize / 2, dotSize);
        }
        if (value >= 4) {
            fillDot(g2d, left - dotSize / 2, bottom - dotSize / 2, dotSize);
            fillDot(g2d, right - dotSize / 2, top - dotSize / 2, dotSize);
        }
        if (value == 6) {
            fillDot(g2d, left - dotSize / 2, center - dotSize / 2, dotSize);
            fillDot(g2d, right - dotSize / 2, center - dotSize / 2, dotSize);
        }
    }

    private void fillDot(Graphics2D g2d, int x, int y, int size) {
        g2d.fillOval(x, y, size, size);
    }
}

class LudoGame extends JFrame {
    private static final int[][] TRACK = {
        {6,1},{6,2},{6,3},{6,4},{6,5},
        {5,6},{4,6},{3,6},{2,6},{1,6},{0,6},
        {0,7},{0,8},
        {1,8},{2,8},{3,8},{4,8},{5,8},
        {6,9},{6,10},{6,11},{6,12},{6,13},{6,14},
        {7,14},{8,14},
        {8,13},{8,12},{8,11},{8,10},{8,9},
        {9,8},{10,8},{11,8},{12,8},{13,8},{14,8},
        {14,7},{14,6},
        {13,6},{12,6},{11,6},{10,6},{9,6},
        {8,5},{8,4},{8,3},{8,2},{8,1},{8,0},
        {7,0},{6,0}
    };

    private static final Color[] COLORS = {
        new Color(231, 76, 60),  // Red
        new Color(46, 204, 113), // Green
        new Color(241, 196, 15), // Yellow
        new Color(52, 152, 219)  // Blue
    };

    private static final String[] NAMES = {"Red", "Green", "Yellow", "Blue"};
    private static final int[] START = {0, 13, 26, 39};
    private static final int[][] YARD = {{0,0},{0,9},{9,9},{9,0}};
    private static final Set<Integer> SAFE =
        new HashSet<>(Arrays.asList(0, 13, 26, 39, 8, 21, 34, 47));
    private static final int FINISH = 56;

    private final int[] seats = {0, 1, 2, 3};
    private final int humanPlayers;
    private final int[][] progress = new int[4][4];
    private final Random random = new Random();

    private int turnSeat;
    private int dice;
    private int consecutiveSixes;
    private boolean busy;
    private boolean awaitingMove;
    private boolean gameOver;

    private final BoardPanel board = new BoardPanel();
    private final DicePanel dicePanel = new DicePanel();
    private final JButton rollButton = new JButton("Roll Dice");
    private final JLabel turnLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel scoreLabel = new JLabel();
    private final String username;

    /**
     * Beautiful Colorful Game Mode Selector Dialog (using Ludo colors for each button).
     */
    static void launch(String username) {
        JDialog dialog = new JDialog((Frame) null, "Ludo - Select Game Mode", true);
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(true);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int barHeight = 8;
                int w = getWidth();
                int q = w / 4;
                g2d.setColor(COLORS[0]); g2d.fillRect(0, 0, q, barHeight);
                g2d.setColor(COLORS[1]); g2d.fillRect(q, 0, q, barHeight);
                g2d.setColor(COLORS[2]); g2d.fillRect(q * 2, 0, q, barHeight);
                g2d.setColor(COLORS[3]); g2d.fillRect(q * 3, 0, w - (q * 3), barHeight);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(254, 253, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("🎮 Select Game Mode");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(44, 62, 80));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Choose number of human players (vs AI)");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(127, 140, 141));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        optionsPanel.setBackground(new Color(254, 253, 245));
        optionsPanel.setMaximumSize(new Dimension(360, 130));

        final int[] selectedHumanCount = {-1};

        String[] options = {"1 Human (Red)", "2 Humans", "3 Humans", "4 Humans"};
        for (int i = 0; i < options.length; i++) {
            final int count = i + 1;
            JButton btn = new JButton(options[i]);
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
            btn.setBackground(COLORS[i]);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            btn.addActionListener(e -> {
                selectedHumanCount[0] = count;
                dialog.dispose();
            });
            optionsPanel.add(btn);
        }

        panel.add(Box.createVerticalStrut(5));
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(15));
        panel.add(optionsPanel);

        dialog.add(panel);
        dialog.setVisible(true);

        if (selectedHumanCount[0] > 0) {
            final int humanCount = selectedHumanCount[0];
            SwingUtilities.invokeLater(() ->
                new LudoGame(humanCount, username).setVisible(true));
        }
    }

    LudoGame(int humanPlayers, String username) {
        super("Ludo - Humans and AI (" + username + ")");
        this.username = username;
        this.humanPlayers = Math.max(1, Math.min(4, humanPlayers));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(board, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        pack();
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        newGame();
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        side.setPreferredSize(new Dimension(210, 600));
        side.setBackground(new Color(254, 253, 245));

        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        turnLabel.setAlignmentX(CENTER_ALIGNMENT);
        turnLabel.setMaximumSize(new Dimension(200, 30));

        dicePanel.setAlignmentX(CENTER_ALIGNMENT);
        dicePanel.setPreferredSize(new Dimension(90, 90));
        dicePanel.setMaximumSize(new Dimension(90, 90));

        rollButton.setAlignmentX(CENTER_ALIGNMENT);
        rollButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        rollButton.setBackground(COLORS[3]);
        rollButton.setForeground(Color.WHITE);
        rollButton.setFocusPainted(false);
        rollButton.addActionListener(e -> rollDice());

        messageLabel.setAlignmentX(CENTER_ALIGNMENT);
        messageLabel.setMaximumSize(new Dimension(200, 60));

        scoreLabel.setAlignmentX(CENTER_ALIGNMENT);

        JButton newButton = new JButton("New Game");
        newButton.setAlignmentX(CENTER_ALIGNMENT);
        newButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        newButton.setBackground(new Color(127, 140, 141));
        newButton.setForeground(Color.WHITE);
        newButton.setFocusPainted(false);
        newButton.addActionListener(e -> newGame());

        side.add(turnLabel);
        side.add(Box.createVerticalStrut(15));
        side.add(dicePanel);
        side.add(Box.createVerticalStrut(15));
        side.add(rollButton);
        side.add(Box.createVerticalStrut(10));
        side.add(messageLabel);
        side.add(Box.createVerticalStrut(15));
        side.add(scoreLabel);
        side.add(Box.createVerticalGlue());
        side.add(newButton);
        return side;
    }

    private boolean isAiTurn() {
        return turnSeat >= humanPlayers;
    }

    private void newGame() {
        for (int[] row : progress) {
            Arrays.fill(row, -1);
        }

        turnSeat = 0;
        dice = 0;
        consecutiveSixes = 0;
        busy = false;
        awaitingMove = false;
        gameOver = false;
        dicePanel.value = 0;
        messageLabel.setText("Roll a 6 to leave the yard.");
        refresh();
    }

    private int currentPlayer() {
        return seats[turnSeat];
    }

    private void rollDice() {
        if (busy || awaitingMove || gameOver) return;

        busy = true;
        refresh();

        final int[] ticks = {0};
        javax.swing.Timer timer = new javax.swing.Timer(70, null);
        timer.addActionListener(e -> {
            dicePanel.value = 1 + random.nextInt(6);
            dicePanel.repaint();

            if (++ticks[0] >= 9) {
                timer.stop();
                dice = 1 + random.nextInt(6);
                dicePanel.value = dice;
                dicePanel.repaint();
                afterRoll();
            }
        });
        timer.start();
    }

    private void afterRoll() {
        if (dice == 6) {
            consecutiveSixes++;
        } else {
            consecutiveSixes = 0;
        }

        if (consecutiveSixes == 3) {
            messageLabel.setText(html(NAMES[currentPlayer()] + " rolled three 6s. Turn forfeited."));
            consecutiveSixes = 0;
            passTurnLater();
            return;
        }

        if (movableTokens().isEmpty()) {
            messageLabel.setText(html(NAMES[currentPlayer()] + " rolled " + dice + " — no valid move."));
            passTurnLater();
            return;
        }

        awaitingMove = true;
        busy = false;
        messageLabel.setText(html("Rolled " + dice + (isAiTurn() ? ". AI is choosing a token." : ". Click a highlighted token.")));
        refresh();

        if (isAiTurn()) {
            scheduleAiAction();
        }
    }

    private void passTurnLater() {
        busy = true;
        refresh();
        javax.swing.Timer timer = new javax.swing.Timer(900, e -> nextPlayer());
        timer.setRepeats(false);
        timer.start();
    }

    private void nextPlayer() {
        turnSeat = (turnSeat + 1) % seats.length;
        consecutiveSixes = 0;
        dice = 0;
        busy = false;
        awaitingMove = false;
        dicePanel.value = 0;
        refresh();

        if (isAiTurn()) {
            scheduleAiAction();
        }
    }

    private void scheduleAiAction() {
        if (!isAiTurn() || gameOver) return;

        javax.swing.Timer timer = new javax.swing.Timer(650, e -> {
            if (gameOver || !isAiTurn()) return;

            if (awaitingMove) {
                List<Integer> options = movableTokens();
                if (!options.isEmpty()) {
                    moveToken(chooseAiToken(options));
                }
            } else if (!busy) {
                rollDice();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private int chooseAiToken(List<Integer> options) {
        int player = currentPlayer();
        int bestToken = options.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int token : options) {
            int score = decisionTreeScore(player, token);
            if (score > bestScore) {
                bestScore = score;
                bestToken = token;
            }
        }
        return bestToken;
    }

    private int decisionTreeScore(int player, int token) {
        int current = progress[player][token];
        int next = current < 0 ? 0 : current + dice;

        if (next == FINISH) return 1000;
        if (next > 50) return 700 + next;

        int trackIndex = (START[player] + next) % 52;

        if (!SAFE.contains(trackIndex) && canCapture(player, trackIndex)) return 800;
        if (current < 0) return 650;
        if (SAFE.contains(trackIndex)) return 600;

        return 100 + next;
    }

    private boolean canCapture(int player, int trackIndex) {
        for (int opponent : seats) {
            if (opponent == player) continue;

            for (int opponentToken = 0; opponentToken < 4; opponentToken++) {
                int opponentPosition = progress[opponent][opponentToken];
                if (opponentPosition >= 0 && opponentPosition <= 50
                        && (START[opponent] + opponentPosition) % 52 == trackIndex) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Integer> movableTokens() {
        List<Integer> result = new ArrayList<>();
        int player = currentPlayer();

        for (int token = 0; token < 4; token++) {
            int position = progress[player][token];
            if (position < 0) {
                if (dice == 6) result.add(token);
            } else if (position + dice <= FINISH) {
                result.add(token);
            }
        }
        return result;
    }

    private void moveToken(int token) {
        int player = currentPlayer();
        boolean captured = false;

        progress[player][token] = progress[player][token] < 0 ? 0 : progress[player][token] + dice;
        int position = progress[player][token];

        if (position <= 50) {
            int trackIndex = (START[player] + position) % 52;

            if (!SAFE.contains(trackIndex)) {
                for (int opponent : seats) {
                    if (opponent == player) continue;

                    for (int opponentToken = 0; opponentToken < 4; opponentToken++) {
                        int opponentPosition = progress[opponent][opponentToken];
                        if (opponentPosition >= 0 && opponentPosition <= 50
                            && (START[opponent] + opponentPosition) % 52 == trackIndex) {
                            progress[opponent][opponentToken] = -1;
                            captured = true;
                        }
                    }
                }
            }
        }

        boolean reachedHome = position == FINISH;

        if (finishedCount(player) == 4) {
            gameOver = true;
            awaitingMove = false;
            busy = false;
            messageLabel.setText(html(NAMES[player] + " wins!"));
            refresh();
            JOptionPane.showMessageDialog(this, NAMES[player] + " wins the game!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        awaitingMove = false;

        if (dice == 6 || captured || reachedHome) {
            String reason = captured ? "Captured a token!" : reachedHome ? "Token reached home!" : "Rolled a 6!";
            messageLabel.setText(html(reason + " " + NAMES[player] + " rolls again."));
            dice = 0;
            busy = false;
            dicePanel.value = 0;
            refresh();

            if (isAiTurn()) {
                scheduleAiAction();
            }
        } else {
            nextPlayer();
        }
    }

    private int finishedCount(int player) {
        int count = 0;
        for (int token = 0; token < 4; token++) {
            if (progress[player][token] == FINISH) count++;
        }
        return count;
    }

    private void refresh() {
        int player = currentPlayer();
        turnLabel.setText(gameOver ? "Game Over" : NAMES[player] + (isAiTurn() ? " (AI)'s turn" : "'s turn"));
        turnLabel.setForeground(COLORS[player].darker());
        rollButton.setEnabled(!busy && !awaitingMove && !gameOver && !isAiTurn());

        StringBuilder score = new StringBuilder("<html><center><b>Home tokens</b><br>");
        for (int seat : seats) {
            score.append("<font color='")
                .append(hex(COLORS[seat].darker()))
                .append("'>")
                .append(NAMES[seat])
                .append(seat >= humanPlayers ? " (AI): " : ": ")
                .append(finishedCount(seat))
                .append("/4</font><br>");
        }
        score.append("</center></html>");
        scoreLabel.setText(score.toString());
        board.repaint();
    }

    private static String html(String text) {
        return "<html><center>" + text + "</center></html>";
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static double[] cellOf(int player, int position) {
        if (position <= 50) {
            int[] cell = TRACK[(START[player] + position) % 52];
            return new double[]{cell[0] + 0.5, cell[1] + 0.5};
        }

        if (position <= 55) {
            int k = position - 51;
            switch (player) {
                case 0: return new double[]{7.5, 1.5 + k};
                case 1: return new double[]{1.5 + k, 7.5};
                case 2: return new double[]{7.5, 13.5 - k};
                default: return new double[]{13.5 - k, 7.5};
            }
        }

        switch (player) {
            case 0: return new double[]{7.5, 6.7};
            case 1: return new double[]{6.7, 7.5};
            case 2: return new double[]{7.5, 8.3};
            default: return new double[]{8.3, 7.5};
        }
    }

    private static double[] yardSpot(int player, int token) {
        return new double[]{
            YARD[player][0] + (token < 2 ? 2 : 4),
            YARD[player][1] + (token % 2 == 0 ? 2 : 4)
        };
    }

    private double[] tokenLocation(int player, int token) {
        int position = progress[player][token];
        return position < 0 ? yardSpot(player, token) : cellOf(player, position);
    }

    private String tokenKey(int player, int token) {
        int position = progress[player][token];
        if (position < 0) return "yard-" + player + "-" + token;
        if (position <= 50) return "track-" + ((START[player] + position) % 52);
        return "home-" + player + "-" + position;
    }

    private class BoardPanel extends JPanel {
        private double cellSize;
        private double offsetX;
        private double offsetY;

        BoardPanel() {
            setPreferredSize(new Dimension(620, 620));
            setBackground(new Color(245, 240, 230));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    handleClick(event.getX(), event.getY());
                }
            });
        }

        private void updateMetrics() {
            cellSize = Math.min(getWidth(), getHeight()) / 15.0;
            offsetX = (getWidth() - cellSize * 15) / 2;
            offsetY = (getHeight() - cellSize * 15) / 2;
        }

        private double px(double column) { return offsetX + column * cellSize; }
        private double py(double row) { return offsetY + row * cellSize; }

        private void handleClick(int mouseX, int mouseY) {
            if (!awaitingMove || gameOver || isAiTurn()) return;

            updateMetrics();
            int player = currentPlayer();

            for (int token : movableTokens()) {
                double[] location = tokenLocation(player, token);
                double dx = mouseX - px(location[1]);
                double dy = mouseY - py(location[0]);

                if (dx * dx + dy * dy <= Math.pow(cellSize * 0.45, 2)) {
                    moveToken(token);
                    return;
                }
            }
        }

        private void fillCell(Graphics2D g, int row, int column, Color color) {
            g.setColor(color);
            g.fillRect((int) px(column), (int) py(row), (int) Math.ceil(cellSize), (int) Math.ceil(cellSize));
            g.setColor(new Color(189, 195, 199));
            g.drawRect((int) px(column), (int) py(row), (int) Math.ceil(cellSize), (int) Math.ceil(cellSize));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            updateMetrics();

            for (int player = 0; player < 4; player++) {
                Color base = COLORS[player];
                g.setColor(base);
                g.fillRect((int) px(YARD[player][1]), (int) py(YARD[player][0]), (int) (cellSize * 6), (int) (cellSize * 6));

                g.setColor(Color.WHITE);
                g.fillRoundRect((int) px(YARD[player][1] + 1), (int) py(YARD[player][0] + 1),
                    (int) (cellSize * 4), (int) (cellSize * 4), (int) (cellSize * 0.6), (int) (cellSize * 0.6));

                for (int token = 0; token < 4; token++) {
                    double[] spot = yardSpot(player, token);
                    g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 90));
                    g.fill(new Ellipse2D.Double(px(spot[1]) - cellSize * 0.4, py(spot[0]) - cellSize * 0.4, cellSize * 0.8, cellSize * 0.8));
                }
            }

            for (int index = 0; index < 52; index++) {
                Color color = Color.WHITE;
                for (int player = 0; player < 4; player++) {
                    if (START[player] == index) color = COLORS[player];
                }
                fillCell(g, TRACK[index][0], TRACK[index][1], color);
            }

            for (int player = 0; player < 4; player++) {
                for (int position = 51; position <= 55; position++) {
                    double[] cell = cellOf(player, position);
                    fillCell(g, (int) cell[0], (int) cell[1], COLORS[player]);
                }
            }

            g.setFont(new Font("SansSerif", Font.BOLD, (int) (cellSize * 0.7)));
            FontMetrics metrics = g.getFontMetrics();
            for (int index : new int[]{8, 21, 34, 47}) {
                int[] cell = TRACK[index];
                String star = "\u2605";
                g.setColor(new Color(127, 140, 141));
                g.drawString(star,
                    (float) (px(cell[1] + 0.5) - metrics.stringWidth(star) / 2.0),
                    (float) (py(cell[0] + 0.5) + metrics.getAscent() / 2.6));
            }

            int[][][] triangles = {
                {{6,6},{9,6},{7,7}},
                {{6,6},{6,9},{7,7}},
                {{6,9},{9,9},{7,7}},
                {{9,6},{9,9},{7,7}}
            };

            for (int player = 0; player < 4; player++) {
                Polygon polygon = new Polygon();
                for (int point = 0; point < 3; point++) {
                    double row = triangles[player][point][0];
                    double column = triangles[player][point][1];
                    if (point == 2) { row = 7.5; column = 7.5; }
                    polygon.addPoint((int) px(column), (int) py(row));
                }
                g.setColor(COLORS[player]);
                g.fillPolygon(polygon);
                g.setColor(new Color(189, 195, 199));
                g.drawPolygon(polygon);
            }

            drawTokens(g);
        }

        private void drawTokens(Graphics2D g) {
            Map<String, List<int[]>> groups = new LinkedHashMap<>();

            for (int player : seats) {
                for (int token = 0; token < 4; token++) {
                    groups.computeIfAbsent(tokenKey(player, token), key -> new ArrayList<>())
                        .add(new int[]{player, token});
                }
            }

            Set<String> movable = new HashSet<>();
            if (awaitingMove && !gameOver) {
                for (int token : movableTokens()) {
                    movable.add(currentPlayer() + ":" + token);
                }
            }

            for (List<int[]> group : groups.values()) {
                for (int index = 0; index < group.size(); index++) {
                    int player = group.get(index)[0];
                    int token = group.get(index)[1];
                    double[] location = tokenLocation(player, token);

                    double cx = px(location[1]);
                    double cy = py(location[0]);
                    double radius = cellSize * 0.4;

                    if (group.size() > 1) {
                        double angle = (2 * Math.PI * index) / group.size();
                        cx += Math.cos(angle) * (cellSize * 0.15);
                        cy += Math.sin(angle) * (cellSize * 0.15);
                        radius = cellSize * 0.3;
                    }

                    g.setColor(COLORS[player]);
                    g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));

                    if (player == currentPlayer() && movable.contains(player + ":" + token)) {
                        g.setColor(new Color(241, 196, 15, 220));
                        g.setStroke(new BasicStroke(3.0f));
                        g.draw(new Ellipse2D.Double(cx - radius - 3, cy - radius - 3, (radius * 2) + 6, (radius * 2) + 6));
                    }
                }
            }
        }
    }
}