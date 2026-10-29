import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;

/**
 * LudoGameApp - Login / Sign Up screens followed by the Ludo game (PvP).
 * Entry point: run this class.
 */
public class LudoGameApp {
    // Simulated database for storing username/password pairs
    private static HashMap<String, String> userDatabase = new HashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowLogin());
    }

    private static void createAndShowLogin() {
        JFrame loginFrame = new JFrame("Ludo Game - Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 200);
        loginFrame.setLayout(new GridLayout(4, 1, 5, 5));
        loginFrame.setLocationRelativeTo(null);

        JPanel userPanel = new JPanel();
        userPanel.add(new JLabel("Username:"));
        JTextField userText = new JTextField(15);
        userPanel.add(userText);

        JPanel passPanel = new JPanel();
        passPanel.add(new JLabel("Password:"));
        JPasswordField passText = new JPasswordField(15);
        passPanel.add(passText);

        JPanel buttonPanel = new JPanel();
        JButton loginButton = new JButton("Login");
        JButton signUpButton = new JButton("Go to Sign Up");
        buttonPanel.add(loginButton);
        buttonPanel.add(signUpButton);

        JLabel messageLabel = new JLabel("", SwingConstants.CENTER);
        messageLabel.setForeground(Color.RED);

        loginFrame.add(userPanel);
        loginFrame.add(passPanel);
        loginFrame.add(buttonPanel);
        loginFrame.add(messageLabel);

        loginButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passText.getPassword());

            if (userDatabase.containsKey(username) && userDatabase.get(username).equals(password)) {
                loginFrame.dispose();
                LudoGame.launch(username);   // start the Ludo game
            } else {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Invalid username or password.");
            }
        });

        signUpButton.addActionListener(e -> {
            loginFrame.dispose();
            createAndShowSignUp();
        });

        loginFrame.setVisible(true);
    }

    private static void createAndShowSignUp() {
        JFrame signUpFrame = new JFrame("Ludo Game - Sign Up");
        signUpFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        signUpFrame.setSize(300, 200);
        signUpFrame.setLayout(new GridLayout(4, 1, 5, 5));
        signUpFrame.setLocationRelativeTo(null);

        JPanel userPanel = new JPanel();
        userPanel.add(new JLabel("New Username:"));
        JTextField userText = new JTextField(12);
        userPanel.add(userText);

        JPanel passPanel = new JPanel();
        passPanel.add(new JLabel("New Password:"));
        JPasswordField passText = new JPasswordField(12);
        passPanel.add(passText);

        JPanel buttonPanel = new JPanel();
        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Back to Login");
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        JLabel messageLabel = new JLabel("", SwingConstants.CENTER);

        signUpFrame.add(userPanel);
        signUpFrame.add(passPanel);
        signUpFrame.add(buttonPanel);
        signUpFrame.add(messageLabel);

        registerButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passText.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Fields cannot be empty.");
            } else if (userDatabase.containsKey(username)) {
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Username already exists.");
            } else {
                userDatabase.put(username, password);
                messageLabel.setForeground(Color.GREEN);
                messageLabel.setText("Sign Up Successful!");
            }
        });

        backButton.addActionListener(e -> {
            signUpFrame.dispose();
            createAndShowLogin();
        });

        signUpFrame.setVisible(true);
    }
}

// ======================================================================
//  LUDO GAME (Player vs Player)
// ======================================================================
/**
 * Ludo - Player vs Player (2-4 players, local hot-seat).
 *
 * Rules implemented:
 *  - Roll a 6 to bring a token out of the yard.
 *  - Rolling a 6, capturing an opponent, or reaching home gives an extra turn.
 *  - Three 6s in a row forfeits the turn.
 *  - Tokens landing on a non-safe track cell capture every opponent token there.
 *  - Safe cells: each colour's start cell and the four star cells.
 *  - Exact roll is required to reach the home centre.
 *  - First player to bring all 4 tokens home wins.
 */
class LudoGame extends JFrame {

    // ---------- Board model ----------
    // The 52 shared track cells as {row, col}, clockwise, beginning at Red's start cell.
    static final int[][] TRACK = {
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
    // Seat order is clockwise: Red, Green, Yellow, Blue
    static final Color[] COLORS = {
        new Color(220, 50, 47), new Color(46, 160, 67),
        new Color(240, 190, 30), new Color(40, 110, 220)
    };
    static final String[] NAMES = {"Red", "Green", "Yellow", "Blue"};
    static final int[] START = {0, 13, 26, 39};
    static final int[][] YARD = {{0,0},{0,9},{9,9},{9,0}};
    static final Set<Integer> SAFE = new HashSet<>(Arrays.asList(0,13,26,39, 8,21,34,47));
    static final int FINISH = 56;   // progress: -1 yard, 0..50 track, 51..55 home column, 56 home

    // ---------- Game state ----------
    private final int[] seats;                 // colour indices taking part
    private final int[][] prog = new int[4][4];
    private int turnSeat;                      // index into seats[]
    private int dice;                          // 0 = not rolled yet
    private int sixes;
    private boolean busy;                      // rolling / waiting for animation
    private boolean awaitingMove;              // dice rolled, player must click a token
    private boolean gameOver;
    private final Random rng = new Random();

    // ---------- UI ----------
    private final BoardPanel board = new BoardPanel();
    private final DicePanel dicePanel = new DicePanel();
    private final JButton rollBtn = new JButton("Roll Dice");
    private final JLabel turnLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel msgLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel scoreLabel = new JLabel();
    private final String username;

    public static void launch(String username) {
        SwingUtilities.invokeLater(() -> {
            String[] opts = {"2 Players", "3 Players", "4 Players"};
            int c = JOptionPane.showOptionDialog(null, "How many players?", "Ludo - Setup",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
            if (c < 0) return;
            new LudoGame(c + 2, username).setVisible(true);
        });
    }

    public LudoGame(int players, String username) {
        super("Ludo - PvP  (" + username + ")");
        this.username = username;
        seats = players == 2 ? new int[]{0, 2}
              : players == 3 ? new int[]{0, 1, 2}
              : new int[]{0, 1, 2, 3};

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

        rollBtn.setAlignmentX(CENTER_ALIGNMENT);
        rollBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        rollBtn.addActionListener(e -> rollDice());

        msgLabel.setAlignmentX(CENTER_ALIGNMENT);
        msgLabel.setMaximumSize(new Dimension(200, 60));
        msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        scoreLabel.setAlignmentX(CENTER_ALIGNMENT);

        JButton newBtn = new JButton("New Game");
        newBtn.setAlignmentX(CENTER_ALIGNMENT);
        newBtn.addActionListener(e -> newGame());

        side.add(turnLabel);
        side.add(Box.createVerticalStrut(15));
        side.add(dicePanel);
        side.add(Box.createVerticalStrut(15));
        side.add(rollBtn);
        side.add(Box.createVerticalStrut(10));
        side.add(msgLabel);
        side.add(Box.createVerticalStrut(15));
        side.add(scoreLabel);
        side.add(Box.createVerticalGlue());
        side.add(newBtn);
        return side;
    }

    // ---------- Game flow ----------
    private void newGame() {
        for (int[] row : prog) Arrays.fill(row, -1);
        turnSeat = 0; dice = 0; sixes = 0;
        busy = false; awaitingMove = false; gameOver = false;
        dicePanel.value = 0;
        msgLabel.setText("Roll a 6 to leave the yard.");
        refresh();
    }

    private int cur() { return seats[turnSeat]; }

    private void rollDice() {
        if (busy || awaitingMove || gameOver) return;
        busy = true;
        refresh();
        final int[] ticks = {0};
        javax.swing.Timer t = new javax.swing.Timer(70, null);
        t.addActionListener(e -> {
            dicePanel.value = 1 + rng.nextInt(6);
            dicePanel.repaint();
            if (++ticks[0] >= 9) {
                t.stop();
                dice = 1 + rng.nextInt(6);
                dicePanel.value = dice;
                dicePanel.repaint();
                afterRoll();
            }
        });
        t.start();
    }

    private void afterRoll() {
        if (dice == 6) sixes++; else sixes = 0;
        if (sixes == 3) {
            msgLabel.setText(html(NAMES[cur()] + " rolled three 6s in a row. Turn forfeited!"));
            sixes = 0;
            passTurnLater();
            return;
        }
        if (movableTokens().isEmpty()) {
            msgLabel.setText(html(NAMES[cur()] + " rolled " + dice + " - no valid move."));
            passTurnLater();
            return;
        }
        awaitingMove = true;
        busy = false;
        msgLabel.setText(html("Rolled " + dice + ". Click a highlighted token."));
        refresh();
    }

    private void passTurnLater() {
        busy = true;
        refresh();
        javax.swing.Timer t = new javax.swing.Timer(1000, e -> nextPlayer());
        t.setRepeats(false);
        t.start();
    }

    private void nextPlayer() {
        turnSeat = (turnSeat + 1) % seats.length;
        sixes = 0; dice = 0; busy = false; awaitingMove = false;
        dicePanel.value = 0;
        refresh();
    }

    private List<Integer> movableTokens() {
        List<Integer> list = new ArrayList<>();
        int pl = cur();
        for (int i = 0; i < 4; i++) {
            int p = prog[pl][i];
            if (p < 0) { if (dice == 6) list.add(i); }
            else if (p + dice <= FINISH) list.add(i);
        }
        return list;
    }

    private void moveToken(int i) {
        int pl = cur();
        boolean captured = false;

        prog[pl][i] = prog[pl][i] < 0 ? 0 : prog[pl][i] + dice;
        int p = prog[pl][i];

        if (p <= 50) {
            int idx = (START[pl] + p) % 52;
            if (!SAFE.contains(idx)) {
                for (int s : seats) {
                    if (s == pl) continue;
                    for (int j = 0; j < 4; j++) {
                        int q = prog[s][j];
                        if (q >= 0 && q <= 50 && (START[s] + q) % 52 == idx) {
                            prog[s][j] = -1;
                            captured = true;
                        }
                    }
                }
            }
        }
        boolean home = p == FINISH;

        if (finishedCount(pl) == 4) {
            gameOver = true; awaitingMove = false; busy = false;
            msgLabel.setText(html(NAMES[pl] + " wins!"));
            refresh();
            JOptionPane.showMessageDialog(this, NAMES[pl] + " wins the game!", "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        awaitingMove = false;
        if (dice == 6 || captured || home) {
            String why = captured ? "Captured a token!" : home ? "Token reached home!" : "Rolled a 6!";
            msgLabel.setText(html(why + " " + NAMES[pl] + " rolls again."));
            dice = 0; busy = false;
            dicePanel.value = 0;
            refresh();
        } else {
            nextPlayer();
        }
    }

    private int finishedCount(int pl) {
        int c = 0;
        for (int i = 0; i < 4; i++) if (prog[pl][i] == FINISH) c++;
        return c;
    }

    private void refresh() {
        int pl = cur();
        turnLabel.setText(gameOver ? "Game Over" : NAMES[pl] + "'s turn");
        turnLabel.setForeground(darker(COLORS[pl]));
        rollBtn.setEnabled(!busy && !awaitingMove && !gameOver);
        StringBuilder sb = new StringBuilder("<html><center><b>Home tokens</b><br>");
        for (int s : seats) {
            sb.append("<font color='").append(hex(darker(COLORS[s]))).append("'>")
              .append(NAMES[s]).append(": ").append(finishedCount(s)).append("/4</font><br>");
        }
        sb.append("</center></html>");
        scoreLabel.setText(sb.toString());
        board.repaint();
    }

    private static String html(String s) { return "<html><center>" + s + "</center></html>"; }
    private static Color darker(Color c) { return c.darker(); }
    private static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

    // ---------- Geometry helpers ----------
    /** Centre (row, col) in cell units of a token with given progress. */
    private static double[] cellOf(int pl, int p) {
        if (p <= 50) {
            int[] t = TRACK[(START[pl] + p) % 52];
            return new double[]{t[0] + 0.5, t[1] + 0.5};
        }
        if (p <= 55) {
            int k = p - 51;
            switch (pl) {
                case 0:  return new double[]{7.5, 1.5 + k};
                case 1:  return new double[]{1.5 + k, 7.5};
                case 2:  return new double[]{7.5, 13.5 - k};
                default: return new double[]{13.5 - k, 7.5};
            }
        }
        switch (pl) {
            case 0:  return new double[]{7.5, 6.7};
            case 1:  return new double[]{6.7, 7.5};
            case 2:  return new double[]{7.5, 8.3};
            default: return new double[]{8.3, 7.5};
        }
    }

    private static double[] yardSpot(int pl, int i) {
        return new double[]{YARD[pl][0] + (i < 2 ? 2 : 4), YARD[pl][1] + (i % 2 == 0 ? 2 : 4)};
    }

    private double[] where(int pl, int i) {
        int p = prog[pl][i];
        return p < 0 ? yardSpot(pl, i) : cellOf(pl, p);
    }

    private String keyOf(int pl, int i) {
        int p = prog[pl][i];
        if (p < 0) return "y" + pl + i;
        if (p <= 50) return "t" + ((START[pl] + p) % 52);
        return "h" + pl + "_" + p;
    }

    // ---------- Board panel ----------
    class BoardPanel extends JPanel {
        double cs, ox, oy;

        BoardPanel() {
            setPreferredSize(new Dimension(620, 620));
            setBackground(new Color(245, 240, 230));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { onClick(e.getX(), e.getY()); }
            });
        }

        void metrics() {
            cs = Math.min(getWidth(), getHeight()) / 15.0;
            ox = (getWidth() - cs * 15) / 2;
            oy = (getHeight() - cs * 15) / 2;
        }
        double px(double col) { return ox + col * cs; }
        double py(double row) { return oy + row * cs; }

        void onClick(int mx, int my) {
            if (!awaitingMove || gameOver) return;
            metrics();
            int pl = cur();
            for (int i : movableTokens()) {
                double[] w = where(pl, i);
                double dx = mx - px(w[1]), dy = my - py(w[0]);
                if (dx * dx + dy * dy <= (cs * 0.45) * (cs * 0.45)) {
                    moveToken(i);
                    return;
                }
            }
        }

        void fillCell(Graphics2D g, int r, int c, Color col) {
            g.setColor(col);
            g.fill(new java.awt.geom.Rectangle2D.Double(px(c), py(r), cs, cs));
            g.setColor(new Color(120, 120, 120));
            g.draw(new java.awt.geom.Rectangle2D.Double(px(c), py(r), cs, cs));
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            metrics();

            // Yards
            for (int pl = 0; pl < 4; pl++) {
                boolean in = false;
                for (int s : seats) if (s == pl) in = true;
                Color base = in ? COLORS[pl] : new Color(200, 200, 200);
                g.setColor(base);
                g.fill(new java.awt.geom.Rectangle2D.Double(px(YARD[pl][1]), py(YARD[pl][0]), cs * 6, cs * 6));
                g.setColor(Color.WHITE);
                g.fill(new java.awt.geom.RoundRectangle2D.Double(px(YARD[pl][1] + 1), py(YARD[pl][0] + 1),
                        cs * 4, cs * 4, cs * 0.6, cs * 0.6));
                for (int i = 0; i < 4; i++) {
                    double[] s = yardSpot(pl, i);
                    g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 90));
                    g.fill(new Ellipse2D.Double(px(s[1]) - cs * 0.4, py(s[0]) - cs * 0.4, cs * 0.8, cs * 0.8));
                }
            }

            // Track cells
            for (int idx = 0; idx < 52; idx++) {
                Color col = Color.WHITE;
                for (int pl = 0; pl < 4; pl++) if (START[pl] == idx) col = COLORS[pl];
                fillCell(g, TRACK[idx][0], TRACK[idx][1], col);
            }
            // Home columns
            for (int pl = 0; pl < 4; pl++) {
                for (int p = 51; p <= 55; p++) {
                    double[] c = cellOf(pl, p);
                    fillCell(g, (int) c[0], (int) c[1], COLORS[pl]);
                }
            }
            // Safe stars
            g.setFont(new Font("SansSerif", Font.BOLD, (int) (cs * 0.7)));
            FontMetrics fm = g.getFontMetrics();
            for (int idx : new int[]{8, 21, 34, 47}) {
                String star = "\u2605";
                int[] t = TRACK[idx];
                g.setColor(new Color(150, 150, 150));
                g.drawString(star, (float) (px(t[1] + 0.5) - fm.stringWidth(star) / 2.0),
                        (float) (py(t[0] + 0.5) + fm.getAscent() / 2.6));
            }

            // Centre triangles
            int[][][] tri = {
                {{6,6},{9,6},{7,7}},   // Red   (left)
                {{6,6},{6,9},{7,7}},   // Green (top)
                {{6,9},{9,9},{7,7}},   // Yellow(right)
                {{9,6},{9,9},{7,7}}    // Blue  (bottom)
            };
            for (int pl = 0; pl < 4; pl++) {
                Polygon poly = new Polygon();
                for (int k = 0; k < 3; k++) {
                    double r = tri[pl][k][0], c = tri[pl][k][1];
                    if (k == 2) { r = 7.5; c = 7.5; }
                    poly.addPoint((int) px(c), (int) py(r));
                }
                g.setColor(COLORS[pl]);
                g.fillPolygon(poly);
                g.setColor(new Color(120, 120, 120));
                g.drawPolygon(poly);
            }

            drawTokens(g);
        }

        void drawTokens(Graphics2D g) {
            // group tokens that share a cell so they don't hide each other
            Map<String, List<int[]>> groups = new LinkedHashMap<>();
            for (int s : seats)
                for (int i = 0; i < 4; i++)
                    groups.computeIfAbsent(keyOf(s, i), k -> new ArrayList<>()).add(new int[]{s, i});

            Set<String> movable = new HashSet<>();
            if (awaitingMove && !gameOver)
                for (int i : movableTokens()) movable.add(cur() + ":" + i);

            for (List<int[]> grp : groups.values()) {
                int n = grp.size();
                for (int k = 0; k < n; k++) {
                    int pl = grp.get(k)[0], i = grp.get(k)[1];
                    double[] w = where(pl, i);
                    double off = n > 1 ? (k - (n - 1) / 2.0) * 0.22 : 0;
                    double x = px(w[1] + off), y = py(w[0]);
                    double r = cs * (n > 1 ? 0.28 : 0.36);

                    if (movable.contains(pl + ":" + i)) {
                        g.setColor(new Color(255, 255, 255, 200));
                        g.setStroke(new BasicStroke(4f));
                        g.draw(new Ellipse2D.Double(x - r - 4, y - r - 4, 2 * r + 8, 2 * r + 8));
                        g.setColor(Color.BLACK);
                        g.setStroke(new BasicStroke(2f));
                        g.draw(new Ellipse2D.Double(x - r - 4, y - r - 4, 2 * r + 8, 2 * r + 8));
                    }
                    g.setColor(COLORS[pl].darker());
                    g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                    g.setColor(COLORS[pl].brighter());
                    g.fill(new Ellipse2D.Double(x - r * 0.75, y - r * 0.75, 1.5 * r, 1.5 * r));
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                }
            }
        }
    }

    // ---------- Dice panel ----------
    static class DicePanel extends JPanel {
        int value = 0;

        DicePanel() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int s = Math.min(getWidth(), getHeight()) - 6;
            int x = (getWidth() - s) / 2, y = (getHeight() - s) / 2;
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, s, s, 18, 18);
            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(x, y, s, s, 18, 18);
            if (value == 0) {
                g.setFont(new Font("SansSerif", Font.BOLD, s / 2));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("?", x + (s - fm.stringWidth("?")) / 2, y + (s + fm.getAscent() / 2) / 2 + 4);
                return;
            }
            double[][] pips = {
                {}, {.5,.5}, {.25,.25,.75,.75}, {.25,.25,.5,.5,.75,.75},
                {.25,.25,.25,.75,.75,.25,.75,.75},
                {.25,.25,.25,.75,.5,.5,.75,.25,.75,.75},
                {.25,.25,.25,.5,.25,.75,.75,.25,.75,.5,.75,.75}
            };
            double[] pp = pips[value];
            int d = s / 6;
            g.setColor(Color.BLACK);
            for (int i = 0; i < pp.length; i += 2) {
                g.fillOval((int) (x + pp[i] * s) - d / 2, (int) (y + pp[i + 1] * s) - d / 2, d, d);
            }
        }
    }
}
