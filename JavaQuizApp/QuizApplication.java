import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class QuizApplication extends JFrame {

    private static final Color C_NAVY  = new Color(21,  67,  96);
    private static final Color C_BLUE  = new Color(41, 128, 185);
    private static final Color C_GREEN = new Color(39, 174,  96);
    private static final Color C_RED   = new Color(192, 57,  43);
    private static final Color C_GOLD  = new Color(243, 156,  18);
    private static final Color C_GREY  = new Color(127, 140, 141);
    private static final Color C_LIGHT = new Color(236, 240, 241);
    private static final Color C_WHITE = Color.WHITE;

    private CardLayout cardLayout;
    private JPanel     rootPanel;

    private Map<String, List<Question>> allSections;
    private String         currentTopic;
    private List<Question> currentQuestions;
    private int            qIndex;
    private int            score;
    private List<Object[]> wrongList;     // {Question, char selectedLetter}
    private ConfettiPanel  activeConfetti;

    // quiz screen
    private JLabel         wTopicLabel;
    private JLabel         wCounterLabel;
    private JProgressBar   wProgressBar;
    private JLabel         wQuestionLabel;
    private JRadioButton[] wOptions;
    private ButtonGroup    wGroup;
    private JButton        wNextBtn;

    // result screen
    private JLabel      wRTopic;
    private JLabel      wRScore;
    private JLabel      wRCorrect;
    private JLabel      wRWrong;
    private JLabel      wRPct;
    private JPanel      wWrongSection;
    private JPanel      wWrongList;
    private JScrollPane wResultScroll;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new QuizApplication().setVisible(true);
        });
    }

    public QuizApplication() {
        allSections = loadQuizFile();
        buildFrame();
    }

    private Map<String, List<Question>> loadQuizFile() {
        try {
            return QuizLoader.load("Quiz_file.txt");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                "Cannot load Quiz_file.txt\n\n" + ex.getMessage() +
                "\n\nMake sure Quiz_file.txt is in the same folder as the program.",
                "File Not Found", JOptionPane.ERROR_MESSAGE);
            return new LinkedHashMap<>();
        }
    }

    private void buildFrame() {
        setTitle("Java Quiz Application");
        setSize(800, 620);
        setMinimumSize(new Dimension(640, 480));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        rootPanel  = new JPanel(cardLayout);

        rootPanel.add(buildMenuPanel(),   "MENU");
        rootPanel.add(buildQuizPanel(),   "QUIZ");
        rootPanel.add(buildResultPanel(), "RESULT");

        add(rootPanel);
        cardLayout.show(rootPanel, "MENU");
    }

    // ---- SCREEN 1: MENU ----

    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_LIGHT);

        JPanel header = new JPanel();
        header.setBackground(C_NAVY);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(26, 30, 22, 35));

        JLabel title = label("☕  Java Quiz Application", 26, Font.BOLD, C_WHITE);
        JLabel sub   = label("Test your Java programming knowledge", 13, Font.PLAIN,
                             new Color(175, 210, 235));
        title.setAlignmentX(CENTER_ALIGNMENT);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);

        JPanel body = new JPanel();
        body.setBackground(C_LIGHT);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(30, 100, 26, 100));

        JLabel prompt = label("Select a Quiz Topic:", 15, Font.BOLD, C_NAVY);
        prompt.setAlignmentX(CENTER_ALIGNMENT);
        body.add(prompt);
        body.add(Box.createVerticalStrut(18));

        String[][] topics = {
            { "📦", "Arrays" },
            { "🔄", "Loops" },
            { "🔀", "Conditional Statements" },
            { "⚙️",  "Functions / Methods" },
            { "🏗️", "Object-Oriented Programming Basics" }
        };

        for (String[] t : topics) {
            final String topicName = t[1];
            JButton btn = topicButton(t[0] + "   " + topicName, C_BLUE);
            btn.addActionListener(e -> startQuiz(topicName));
            body.add(btn);
            body.add(Box.createVerticalStrut(9));
        }

        body.add(Box.createVerticalStrut(10));
        JButton exitBtn = topicButton("🚪   Exit Application", C_GREY);
        exitBtn.addActionListener(e -> {
            if (confirm("Are you sure you want to exit the application?"))
                System.exit(0);
        });
        body.add(exitBtn);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll,  BorderLayout.CENTER);
        return panel;
    }

    // ---- SCREEN 2: QUIZ ----

    private JPanel buildQuizPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_WHITE);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(C_BLUE);
        topBar.setBorder(new EmptyBorder(13, 24, 13, 24));

        wTopicLabel   = label("Topic", 15, Font.BOLD, C_WHITE);
        wCounterLabel = label("Question 1 of 10", 13, Font.PLAIN, C_WHITE);
        topBar.add(wTopicLabel,   BorderLayout.WEST);
        topBar.add(wCounterLabel, BorderLayout.EAST);

        wProgressBar = new JProgressBar(0, 10);
        wProgressBar.setForeground(C_GREEN);
        wProgressBar.setBackground(C_NAVY);
        wProgressBar.setPreferredSize(new Dimension(0, 8));
        wProgressBar.setBorderPainted(false);

        JPanel northBlock = new JPanel(new BorderLayout());
        northBlock.add(topBar,       BorderLayout.NORTH);
        northBlock.add(wProgressBar, BorderLayout.SOUTH);

        JPanel qArea = new JPanel();
        qArea.setBackground(C_WHITE);
        qArea.setLayout(new BoxLayout(qArea, BoxLayout.Y_AXIS));
        qArea.setBorder(new EmptyBorder(32, 50, 24, 55));

        wQuestionLabel = new JLabel("<html><body style='width:600px'>Question goes here</body></html>");
        wQuestionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        wQuestionLabel.setForeground(C_NAVY);
        wQuestionLabel.setAlignmentX(LEFT_ALIGNMENT);
        qArea.add(wQuestionLabel);
        qArea.add(Box.createVerticalStrut(26));

        wOptions = new JRadioButton[4];
        wGroup   = new ButtonGroup();
        char[] letters = {'A', 'B', 'C', 'D'};

        for (int i = 0; i < 4; i++) {
            // each row is a bold letter label + the radio button text
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            row.setBackground(C_WHITE);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            JLabel letterLbl = label(letters[i] + ".  ", 14, Font.BOLD, C_BLUE);

            wOptions[i] = new JRadioButton();
            wOptions[i].setFont(new Font("Segoe UI", Font.PLAIN, 14));
            wOptions[i].setBackground(C_WHITE);
            wOptions[i].setFocusPainted(false);
            wGroup.add(wOptions[i]);

            row.add(letterLbl);
            row.add(wOptions[i]);
            qArea.add(row);
            qArea.add(Box.createVerticalStrut(10));
        }

        JPanel botBar = new JPanel(new BorderLayout());
        botBar.setBackground(C_LIGHT);
        botBar.setBorder(new EmptyBorder(10, 24, 10, 24));

        JButton backBtn = actionButton("← Menu", C_GREY);
        backBtn.addActionListener(e -> {
            if (confirm("Return to main menu? Your progress will be lost."))
                cardLayout.show(rootPanel, "MENU");
        });

        wNextBtn = actionButton("Next  →", C_GREEN);
        wNextBtn.setPreferredSize(new Dimension(155, 40));
        wNextBtn.addActionListener(e -> handleNext());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(C_LIGHT);
        btnRow.add(backBtn);
        btnRow.add(wNextBtn);
        botBar.add(btnRow, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(qArea);
        scroll.setBorder(null);

        panel.add(northBlock, BorderLayout.NORTH);
        panel.add(scroll,     BorderLayout.CENTER);
        panel.add(botBar,     BorderLayout.SOUTH);
        return panel;
    }

    // ---- SCREEN 3: RESULT ----

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_LIGHT);

        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(C_NAVY);
        header.setPreferredSize(new Dimension(0, 82));
        header.add(label("🎉  Quiz Completed!", 24, Font.BOLD, C_WHITE));

        JPanel body = new JPanel();
        body.setBackground(C_LIGHT);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(28, 70, 30, 70));

        wRTopic = label("Topic: -", 17, Font.BOLD, C_NAVY);
        wRTopic.setAlignmentX(CENTER_ALIGNMENT);
        body.add(wRTopic);
        body.add(Box.createVerticalStrut(20));

        // score card
        JPanel scoreCard = new JPanel();
        scoreCard.setBackground(C_WHITE);
        scoreCard.setLayout(new BoxLayout(scoreCard, BoxLayout.Y_AXIS));
        scoreCard.setBorder(new LineBorder(new Color(210, 215, 220), 1, true));
        scoreCard.setAlignmentX(CENTER_ALIGNMENT);
        scoreCard.setMaximumSize(new Dimension(560, Integer.MAX_VALUE));

        wRScore   = new JLabel("-");
        wRCorrect = new JLabel("-");
        wRWrong   = new JLabel("-");
        wRPct     = new JLabel("-");

        scoreCard.add(resultRow("🏆  Final Score",     wRScore,   C_GOLD));
        scoreCard.add(new JSeparator());
        scoreCard.add(resultRow("✅  Correct Answers",  wRCorrect, C_GREEN));
        scoreCard.add(new JSeparator());
        scoreCard.add(resultRow("❌  Wrong Answers",    wRWrong,   C_RED));
        scoreCard.add(new JSeparator());
        scoreCard.add(resultRow("📊  Percentage",       wRPct,     C_BLUE));

        body.add(scoreCard);
        body.add(Box.createVerticalStrut(22));

        // buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setBackground(C_LIGHT);
        btnRow.setAlignmentX(CENTER_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JButton tryAgain = actionButton("↺  Try Again", C_BLUE);
        tryAgain.addActionListener(e -> startQuiz(currentTopic));

        JButton toMenu = actionButton("⌂  Main Menu", C_GREY);
        toMenu.addActionListener(e -> {
            if (activeConfetti != null) { activeConfetti.stop(); activeConfetti = null; }
            cardLayout.show(rootPanel, "MENU");
        });

        btnRow.add(tryAgain);
        btnRow.add(toMenu);
        body.add(btnRow);

        // missed questions section — hidden when score is perfect
        body.add(Box.createVerticalStrut(28));

        wWrongSection = new JPanel();
        wWrongSection.setBackground(C_LIGHT);
        wWrongSection.setLayout(new BoxLayout(wWrongSection, BoxLayout.Y_AXIS));
        wWrongSection.setAlignmentX(CENTER_ALIGNMENT);
        wWrongSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel missedHdr = label("Missed Questions", 15, Font.BOLD, C_NAVY);
        missedHdr.setAlignmentX(LEFT_ALIGNMENT);
        wWrongSection.add(missedHdr);
        wWrongSection.add(Box.createVerticalStrut(10));

        wWrongList = new JPanel();
        wWrongList.setBackground(C_LIGHT);
        wWrongList.setLayout(new BoxLayout(wWrongList, BoxLayout.Y_AXIS));
        wWrongList.setAlignmentX(LEFT_ALIGNMENT);
        wWrongSection.add(wWrongList);

        body.add(wWrongSection);

        wResultScroll = new JScrollPane(body);
        wResultScroll.setBorder(null);
        wResultScroll.getVerticalScrollBar().setUnitIncrement(14);
        wResultScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(header,       BorderLayout.NORTH);
        panel.add(wResultScroll, BorderLayout.CENTER);
        return panel;
    }

    // valueLabel is populated later in showResult()
    private JPanel resultRow(String text, JLabel val, Color valColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(C_WHITE);
        row.setBorder(new EmptyBorder(13, 24, 13, 24));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JLabel lbl = label(text, 14, Font.PLAIN, new Color(80, 80, 80));
        val.setFont(new Font("Segoe UI", Font.BOLD, 16));
        val.setForeground(valColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // ---- QUIZ LOGIC ----

    private void startQuiz(String topic) {
        if (activeConfetti != null) { activeConfetti.stop(); activeConfetti = null; }
        List<Question> qs = allSections.get(topic);
        if (qs == null || qs.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No questions found for topic: " + topic,
                "Missing Questions", JOptionPane.ERROR_MESSAGE);
            return;
        }
        currentTopic     = topic;
        currentQuestions = qs;
        wrongList        = new ArrayList<>();
        qIndex           = 0;
        score            = 0;
        loadQuestion(0);
        cardLayout.show(rootPanel, "QUIZ");
    }

    private void loadQuestion(int idx) {
        Question q   = currentQuestions.get(idx);
        int      tot = currentQuestions.size();

        wTopicLabel.setText(currentTopic);
        wCounterLabel.setText("Question " + (idx + 1) + " of " + tot);

        wProgressBar.setMaximum(tot);
        wProgressBar.setValue(idx);

        wQuestionLabel.setText(
            "<html><body style='width:600px'>" +
            (idx + 1) + ".&nbsp;&nbsp;" + q.getText() +
            "</body></html>");

        wGroup.clearSelection();
        for (int i = 0; i < 4; i++)
            wOptions[i].setText(q.getOptions()[i]);

        // swap button label on the last question
        wNextBtn.setText(idx == tot - 1 ? "Finish  ✓" : "Next  →");
    }

    private void handleNext() {
        char selected = 0;
        char[] letters = {'A', 'B', 'C', 'D'};
        for (int i = 0; i < 4; i++) {
            if (wOptions[i].isSelected()) {
                selected = letters[i];
                break;
            }
        }

        if (selected == 0) {
            JOptionPane.showMessageDialog(this,
                "Please select an answer before continuing.",
                "No Answer Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Question q = currentQuestions.get(qIndex);
        if (String.valueOf(selected).equals(q.getCorrectAnswer())) {
            score++;
        } else {
            wrongList.add(new Object[]{q, selected});
        }

        qIndex++;
        if (qIndex < currentQuestions.size()) {
            loadQuestion(qIndex);
        } else {
            showResult();
        }
    }

    private void showResult() {
        int total = currentQuestions.size();
        int wrong = total - score;
        int pct   = total > 0 ? (score * 100) / total : 0;

        wRTopic.setText("Topic:  " + currentTopic);
        wRScore.setText(score + " / " + total);
        wRCorrect.setText(String.valueOf(score));
        wRWrong.setText(String.valueOf(wrong));
        wRPct.setText(pct + "%");

        wWrongList.removeAll();
        if (wrongList.isEmpty()) {
            wWrongSection.setVisible(false);
        } else {
            wWrongSection.setVisible(true);
            for (int i = 0; i < wrongList.size(); i++) {
                Question q   = (Question) wrongList.get(i)[0];
                char     sel = (char)     wrongList.get(i)[1];
                wWrongList.add(buildWrongCard(i + 1, q, sel));
                wWrongList.add(Box.createVerticalStrut(10));
            }
        }
        wWrongList.revalidate();
        wWrongList.repaint();

        saveResultToFile(total, wrong, pct);
        cardLayout.show(rootPanel, "RESULT");

        // scroll back to top so score card is visible first
        SwingUtilities.invokeLater(() ->
            wResultScroll.getVerticalScrollBar().setValue(0));

        if (pct == 100) launchConfetti();
    }

    private JPanel buildWrongCard(int num, Question q, char sel) {
        int    selIdx   = sel - 'A';
        int    corrIdx  = q.getCorrectAnswer().charAt(0) - 'A';
        String selText  = q.getOptions()[selIdx];
        String corrText = q.getOptions()[corrIdx];

        JPanel card = new JPanel();
        card.setBackground(C_WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, C_RED),
            BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 225), 1),
                new EmptyBorder(12, 14, 12, 14)
            )
        ));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel qLbl = new JLabel(
            "<html><b>Q" + num + ".</b>&nbsp;" + escHtml(q.getText()) + "</html>");
        qLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        qLbl.setForeground(new Color(40, 40, 40));
        qLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel yourLbl = new JLabel("✗  Your answer:    " + sel + ". " + selText);
        yourLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        yourLbl.setForeground(C_RED);
        yourLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel corrLbl = new JLabel(
            "✓  Correct answer: " + q.getCorrectAnswer() + ". " + corrText);
        corrLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        corrLbl.setForeground(new Color(27, 153, 80));
        corrLbl.setAlignmentX(LEFT_ALIGNMENT);

        card.add(qLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(yourLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(corrLbl);
        return card;
    }

    private String escHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---- CONFETTI ANIMATION ----

    private void launchConfetti() {
        activeConfetti = new ConfettiPanel();
        setGlassPane(activeConfetti);
        activeConfetti.setVisible(true);
        activeConfetti.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (activeConfetti != null) {
                    activeConfetti.stop();
                    activeConfetti = null;
                }
            }
        });
        activeConfetti.start();
    }

    private class ConfettiPanel extends JPanel {
        private final int     NUM    = 130;
        private final float[] px     = new float[NUM];
        private final float[] py     = new float[NUM];
        private final float[] vx     = new float[NUM];
        private final float[] vy     = new float[NUM];
        private final float[] rot    = new float[NUM];
        private final float[] rotV   = new float[NUM];
        private final Color[] colors = new Color[NUM];
        private final int[]   cw     = new int[NUM];
        private final int[]   ch     = new int[NUM];
        private long              startTime;
        private javax.swing.Timer timer;

        ConfettiPanel() {
            setOpaque(false);
            Color[] palette = { C_GOLD, C_GREEN, C_BLUE, C_RED,
                                new Color(155, 89, 182), new Color(26, 188, 156),
                                new Color(231, 76, 60),  new Color(52, 152, 219) };
            Random rnd    = new Random();
            int    frameW = QuizApplication.this.getWidth();
            for (int i = 0; i < NUM; i++) {
                px[i]    = rnd.nextFloat() * frameW;
                py[i]    = -rnd.nextInt(450);
                vx[i]    = (rnd.nextFloat() - 0.5f) * 2.5f;
                vy[i]    = 2.0f + rnd.nextFloat() * 2.5f;
                rot[i]   = rnd.nextFloat() * 360f;
                rotV[i]  = (rnd.nextFloat() - 0.5f) * 7f;
                colors[i] = palette[rnd.nextInt(palette.length)];
                cw[i]    = 7  + rnd.nextInt(7);
                ch[i]    = 4  + rnd.nextInt(4);
            }
        }

        void start() {
            startTime = System.currentTimeMillis();
            timer = new javax.swing.Timer(28, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > 4800) { stop(); return; }
                for (int i = 0; i < NUM; i++) {
                    px[i] += vx[i];
                    py[i] += vy[i];
                    rot[i] += rotV[i];
                    if (py[i] > getHeight() + 20) {
                        py[i] = -20;
                        px[i] = (float)(Math.random() * getWidth());
                    }
                }
                repaint();
            });
            timer.start();
        }

        void stop() {
            if (timer != null) timer.stop();
            setVisible(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            long  elapsed = System.currentTimeMillis() - startTime;
            float alpha   = elapsed < 3200
                ? 1f
                : Math.max(0f, 1f - (elapsed - 3200) / 1600f);

            for (int i = 0; i < NUM; i++) {
                Graphics2D g3 = (Graphics2D) g2.create();
                g3.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
                g3.setColor(colors[i]);
                g3.translate(px[i], py[i]);
                g3.rotate(Math.toRadians(rot[i]));
                g3.fillRect(-cw[i] / 2, -ch[i] / 2, cw[i], ch[i]);
                g3.dispose();
            }
            g2.dispose();
        }
    }

    // ---- FILE SAVE ----

    private void saveResultToFile(int total, int wrong, int pct) {
        String filename  = "quiz_results.txt";
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String border = "=".repeat(46);

        try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
            pw.println(border);
            pw.println("Quiz Result");
            pw.println("Date/Time   : " + timestamp);
            pw.println("Topic       : " + currentTopic);
            pw.println("Total Q's   : " + total);
            pw.println("Correct     : " + score);
            pw.println("Wrong       : " + wrong);
            pw.println("Final Score : " + score + " / " + total);
            pw.println("Percentage  : " + pct + "%");
            pw.println(border);
            pw.println();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not save results:\n" + ex.getMessage(),
                "Save Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ---- WIDGET HELPERS ----

    private JLabel label(String text, int size, int style, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(fg);
        return l;
    }

    private JButton topicButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        b.setBackground(bg);
        b.setForeground(C_WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 47));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(12, 22, 12, 22));
        Color hover = bg.darker();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);   }
        });
        return b;
    }

    private JButton actionButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(C_WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(138, 40));
        Color hover = bg.darker();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);   }
        });
        return b;
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(
            this, message, "Confirm",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
