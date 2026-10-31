
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;

/**
 * Save as LudoGameApp.java
 *
 * Local Ludo game with login/sign-up and AI-controlled seats.
 * The number selected at setup is the number of human players.
 */
public class LudoGameApp {
    private static final Map<String, String> userDatabase = new HashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LudoGameApp::showLogin);
    }

    private static void showLogin() {
        JFrame frame = new JFrame("Ludo Game - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(340, 190);
        frame.setLayout(new GridLayout(4, 1, 5, 5));
        frame.setLocationRelativeTo(null);

        JPanel userPanel = new JPanel();
        userPanel.add(new JLabel("Username:"));
        JTextField userField = new JTextField(15);
        userPanel.add(userField);

        JPanel passPanel = new JPanel();
        passPanel.add(new JLabel("Password:"));
        JPasswordField passField = new JPasswordField(15);
        passPanel.add(passField);

        JPanel buttonPanel = new JPanel();
        JButton loginButton = new JButton("Login");
        JButton signUpButton = new JButton("Sign Up");
        buttonPanel.add(loginButton);
        buttonPanel.add(signUpButton);

        JLabel message = new JLabel(" ", SwingConstants.CENTER);
        message.setForeground(Color.RED);

        frame.add(userPanel);
        frame.add(passPanel);
        frame.add(buttonPanel);
        frame.add(message);

        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (userDatabase.containsKey(username)
                    && userDatabase.get(username).equals(password)) {
                frame.dispose();
                LudoGame.launch(username);
            } else {
                message.setForeground(Color.RED);
                message.setText("Invalid username or password.");
            }
        });

        signUpButton.addActionListener(e -> {
            frame.dispose();
            showSignUp();
        });

        frame.setVisible(true);
    }

    private static void showSignUp() {
        JFrame frame = new JFrame("Ludo Game - Sign Up");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(340, 190);
        frame.setLayout(new GridLayout(4, 1, 5, 5));
        frame.setLocationRelativeTo(null);

        JPanel userPanel = new JPanel();
        userPanel.add(new JLabel("New username:"));
        JTextField userField = new JTextField(15);
        userPanel.add(userField);

        JPanel passPanel = new JPanel();
        passPanel.add(new JLabel("New password:"));
        JPasswordField passField = new JPasswordField(15);
        passPanel.add(passField);

        JPanel buttonPanel = new JPanel();
        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back to Login");
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        JLabel message = new JLabel(" ", SwingConstants.CENTER);
        frame.add(userPanel);
        frame.add(passPanel);
        frame.add(buttonPanel);
        frame.add(message);

        registerButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                message.setForeground(Color.RED);
                message.setText("Fields cannot be empty.");
            } else if (userDatabase.containsKey(username)) {
                message.setForeground(Color.RED);
                message.setText("Username already exists.");
            } else {
                userDatabase.put(username, password);
                message.setForeground(new Color(0, 130, 0));
                message.setText("Sign up successful. Go back to login.");
            }
        });

        backButton.addActionListener(e -> {
            frame.dispose();
            showLogin();
        });

        frame.setVisible(true);
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
        new Color(220, 50, 47),
        new Color(46, 160, 67),
        new Color(240, 190, 30),
        new Color(40, 110, 220)
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

    static void launch(String username) {
        String[] options = {"1 Human", "2 Humans", "3 Humans", "4 Humans"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "How many human players?",
            "Ludo - Setup",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (choice < 0) {
            return;
        }

        final int humanCount = choice + 1;
        SwingUtilities.invokeLater(() ->
            new LudoGame(humanCount, username).setVisible(true));
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

        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        turnLabel.setAlignmentX(CENTER_ALIGNMENT);
        turnLabel.setMaximumSize(new Dimension(200, 30));

        dicePanel.setAlignmentX(CENTER_ALIGNMENT);
        dicePanel.setPreferredSize(new Dimension(90, 90));
        dicePanel.setMaximumSize(new Dimension(90, 90));

        rollButton.setAlignmentX(CENTER_ALIGNMENT);
        rollButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        rollButton.addActionListener(e -> rollDice());

        messageLabel.setAlignmentX(CENTER_ALIGNMENT);
        messageLabel.setMaximumSize(new Dimension(200, 60));

        scoreLabel.setAlignmentX(CENTER_ALIGNMENT);

        JButton newButton = new JButton("New Game");
        newButton.setAlignmentX(CENTER_ALIGNMENT);
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
        if (busy || awaitingMove || gameOver) {
            return;
        }

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
            messageLabel.setText(html(NAMES[currentPlayer()]
                + " rolled three 6s. Turn forfeited."));
            consecutiveSixes = 0;
            passTurnLater();
            return;
        }

        if (movableTokens().isEmpty()) {
            messageLabel.setText(html(NAMES[currentPlayer()]
                + " rolled " + dice + " — no valid move."));
            passTurnLater();
            return;
        }

        awaitingMove = true;
        busy = false;
        messageLabel.setText(html("Rolled " + dice
            + (isAiTurn() ? ". AI is choosing a token." : ". Click a highlighted token.")));
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
        if (!isAiTurn() || gameOver) {
            return;
        }

        javax.swing.Timer timer = new javax.swing.Timer(650, e -> {
            if (gameOver || !isAiTurn()) {
                return;
            }

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

        for (int token : options) {
            int nextProgress = progress[player][token] < 0
                ? 0
                : progress[player][token] + dice;

            if (nextProgress > 50) {
                continue;
            }

            int trackIndex = (START[player] + nextProgress) % 52;
            if (SAFE.contains(trackIndex)) {
                continue;
            }

            for (int opponent : seats) {
                if (opponent == player) {
                    continue;
                }

                for (int opponentToken = 0; opponentToken < 4; opponentToken++) {
                    int opponentProgress = progress[opponent][opponentToken];
                    if (opponentProgress >= 0 && opponentProgress <= 50
                        && (START[opponent] + opponentProgress) % 52 == trackIndex) {
                        return token;
                    }
                }
            }
        }

        return options.get(random.nextInt(options.size()));
    }

    private List<Integer> movableTokens() {
        List<Integer> result = new ArrayList<>();
        int player = currentPlayer();

        for (int token = 0; token < 4; token++) {
            int position = progress[player][token];
            if (position < 0) {
                if (dice == 6) {
                    result.add(token);
                }
            } else if (position + dice <= FINISH) {
                result.add(token);
            }
        }

        return result;
    }

    private void moveToken(int token) {
        int player = currentPlayer();
        boolean captured = false;

        progress[player][token] = progress[player][token] < 0
            ? 0
            : progress[player][token] + dice;

        int position = progress[player][token];

        if (position <= 50) {
            int trackIndex = (START[player] + position) % 52;

            if (!SAFE.contains(trackIndex)) {
                for (int opponent : seats) {
                    if (opponent == player) {
                        continue;
                    }

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
            JOptionPane.showMessageDialog(
                this,
                NAMES[player] + " wins the game!",
                "Game Over",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        awaitingMove = false;

        if (dice == 6 || captured || reachedHome) {
            String reason = captured
                ? "Captured a token!"
                : reachedHome
                ? "Token reached home!"
                : "Rolled a 6!";

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
            if (progress[player][token] == FINISH) {
                count++;
            }
        }
        return count;
    }

    private void refresh() {
        int player = currentPlayer();
        turnLabel.setText(gameOver
            ? "Game Over"
            : NAMES[player] + (isAiTurn() ? " (AI)'s turn" : "'s turn"));
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
        return String.format("#%02x%02x%02x",
            color.getRed(), color.getGreen(), color.getBlue());
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
        return position < 0
            ? yardSpot(player, token)
            : cellOf(player, position);
    }

    private String tokenKey(int player, int token) {
        int position = progress[player][token];

        if (position < 0) {
            return "yard-" + player + "-" + token;
        }
        if (position <= 50) {
            return "track-" + ((START[player] + position) % 52);
        }
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

        private double px(double column) {
            return offsetX + column * cellSize;
        }

        private double py(double row) {
            return offsetY + row * cellSize;
        }

        private void handleClick(int mouseX, int mouseY) {
            if (!awaitingMove || gameOver || isAiTurn()) {
                return;
            }

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
            g.fillRect((int) px(column), (int) py(row),
                (int) Math.ceil(cellSize), (int) Math.ceil(cellSize));
            g.setColor(new Color(120, 120, 120));
            g.drawRect((int) px(column), (int) py(row),
                (int) Math.ceil(cellSize), (int) Math.ceil(cellSize));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            updateMetrics();

            for (int player = 0; player < 4; player++) {
                Color base = COLORS[player];
                g.setColor(base);
                g.fillRect((int) px(YARD[player][1]), (int) py(YARD[player][0]),
                    (int) (cellSize * 6), (int) (cellSize * 6));

                g.setColor(Color.WHITE);
                g.fillRoundRect((int) px(YARD[player][1] + 1),
                    (int) py(YARD[player][0] + 1),
                    (int) (cellSize * 4), (int) (cellSize * 4),
                    (int) (cellSize * 0.6), (int) (cellSize * 0.6));

                for (int token = 0; token < 4; token++) {
                    double[] spot = yardSpot(player, token);
                    g.setColor(new Color(base.getRed(), base.getGreen(),
                        base.getBlue(), 90));
                    g.fill(new Ellipse2D.Double(
                        px(spot[1]) - cellSize * 0.4,
                        py(spot[0]) - cellSize * 0.4,
                        cellSize * 0.8,
                        cellSize * 0.8));
                }
            }

            for (int index = 0; index < 52; index++) {
                Color color = Color.WHITE;
                for (int player = 0; player < 4; player++) {
                    if (START[player] == index) {
                        color = COLORS[player];
                    }
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
                g.setColor(new Color(150, 150, 150));
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

                    if (point == 2) {
                        row = 7.5;
                        column = 7.5;
                    }

                    polygon.addPoint((int) px(column), (int) py(row));
                }

                g.setColor(COLORS[player]);
                g.fillPolygon(polygon);
                g.setColor(new Color(120, 120, 120));
                g.drawPolygon(polygon);
            }

            drawTokens(g);
        }

        private void drawTokens(Graphics2D g) {
            Map<String, List<int[]>> groups = new LinkedHashMap<>();

            for (int player : seats) {
                for (int token = 0; token < 4; token++) {
                    groups.computeIfAbsent(tokenKey(player, token),
                        key -> new ArrayList<>())
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
                    double offset = group.size() > 1
                        ? (index - (group.size() - 1) / 2.0) * 0.22
                        : 0;
                    double x = px(location[1] + offset);
                    double y = py(location[0]);
                    double radius = cellSize * (group.size() > 1 ? 0.28 : 0.36);

                    if (movable.contains(player + ":" + token)) {
                        g.setColor(Color.WHITE);
                        g.setStroke(new BasicStroke(4f));
                        g.draw(new Ellipse2D.Double(
                            x - radius - 4, y - radius - 4,
                            2 * radius + 8, 2 * radius + 8));
                        g.setColor(Color.BLACK);
                        g.setStroke(new BasicStroke(2f));
                        g.draw(new Ellipse2D.Double(
                            x - radius - 4, y - radius - 4,
                            2 * radius + 8, 2 * radius + 8));
                    }

                    g.setColor(COLORS[player].darker());
                    g.fill(new Ellipse2D.Double(
                        x - radius, y - radius, 2 * radius, 2 * radius));
                    g.setColor(COLORS[player].brighter());
                    g.fill(new Ellipse2D.Double(
                        x - radius * 0.75, y - radius * 0.75,
                        1.5 * radius, 1.5 * radius));
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(new Ellipse2D.Double(
                        x - radius, y - radius, 2 * radius, 2 * radius));
                }
            }
        }
    }

    private static class DicePanel extends JPanel {
        int value;

        DicePanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 6;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, size, size, 18, 18);
            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(x, y, size, size, 18, 18);

            if (value == 0) {
                g.setFont(new Font("SansSerif", Font.BOLD, size / 2));
                FontMetrics metrics = g.getFontMetrics();
                g.drawString("?",
                    x + (size - metrics.stringWidth("?")) / 2,
                    y + (size + metrics.getAscent() / 2) / 2 + 4);
                return;
            }

            double[][] pips = {
                {},
                {.5,.5},
                {.25,.25,.75,.75},
                {.25,.25,.5,.5,.75,.75},
                {.25,.25,.25,.75,.75,.25,.75,.75},
                {.25,.25,.25,.75,.5,.5,.75,.25,.75,.75},
                {.25,.25,.25,.5,.25,.75,.75,.25,.75,.5,.75,.75}
            };

            int diameter = size / 6;
            g.setColor(Color.BLACK);
            for (int i = 0; i < pips[value].length; i += 2) {
                g.fillOval(
                    (int) (x + pips[value][i] * size) - diameter / 2,
                    (int) (y + pips[value][i + 1] * size) - diameter / 2,
                    diameter,
                    diameter);
            }
        }
    }
}
