package space.vampir.engine;

import org.jetbrains.annotations.NotNull;
import space.vampir.engine.message.Odometry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

public class ExperimentalEvaluation {
    private final ArrayList<Odometry> reference = new ArrayList<>();
    private final ArrayList<Odometry> GNSS = new ArrayList<>();
    private final ArrayList<Odometry> verificationEngine = new ArrayList<>();

    // Max error to show on histogram (meters)
    private final double MAX_ERROR_RANGE = 2.5;
    private final int BIN_COUNT = 5; // Number of bars/categories
    double diff = 0.5;

    public void addOdometries(Odometry ref, Odometry sen, Odometry ver){
        reference.add(ref);
        GNSS.add(sen);
        verificationEngine.add(ver);
    }

    /**
     * Entry point to launch the application instance.
     */
    public void startApplication() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = createMainFrame();
            frame.setVisible(true);
        });
    }

    /**
     * Creates and configures the main application window.
     */
    private JFrame createMainFrame() {
        JFrame frame = new JFrame("Experimental Evaluation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main layout: 1 row, 2 columns with 40px horizontal gap
        frame.setLayout(new GridLayout(1, 2, 40, 0));
        ((JPanel)frame.getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- LEFT COLUMN: TABLE + METRICS ---
        JPanel leftColumn = new JPanel(new BorderLayout());
        JPanel leftContentWrapper = new JPanel(new BorderLayout(0, 10));

        // Add Table to the top
        leftContentWrapper.add(createTablePanel(), BorderLayout.NORTH);

        // --- NEW: Metrics Panel (Precision & Recall) ---
        // A BoxLayout.Y_AXIS segítségével függőlegesen egymás alá kerülnek
        JPanel metricsPanel = new JPanel();
        metricsPanel.setLayout(new BoxLayout(metricsPanel, BoxLayout.Y_AXIS));
        metricsPanel.setBorder(new EmptyBorder(20, 10, 0, 0)); // Extra margin above and left

        JLabel precisionLabel = new JLabel("Availability: 0.00");
        JLabel recallLabel = new JLabel("Integrity: 0.00");

        // Styling the labels
        Font metricsFont = new Font("SansSerif", Font.BOLD, 14);
        precisionLabel.setFont(metricsFont);
        recallLabel.setFont(metricsFont);

        // Adding components with a small gap between them
        metricsPanel.add(precisionLabel);
        metricsPanel.add(Box.createRigidArea(new Dimension(0, 8))); // 8px vertical space
        metricsPanel.add(recallLabel);

        // Add metrics to the center (below the table)
        leftContentWrapper.add(metricsPanel, BorderLayout.CENTER);

        leftColumn.add(leftContentWrapper, BorderLayout.NORTH);

        // --- RIGHT COLUMN: HISTOGRAM + TEXT ---
        JPanel rightColumn = new JPanel(new BorderLayout(0, 10));
        rightColumn.add(new HistogramPanel(), BorderLayout.CENTER);

        JLabel graphLabel = new JLabel("<html><b>Figure 1:</b> Error Distribution Percentage</html>", JLabel.CENTER);
        graphLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightColumn.add(graphLabel, BorderLayout.SOUTH);

        // Add columns to the frame
        frame.add(leftColumn);
        frame.add(rightColumn);

        frame.setSize(950, 550); // Increased size slightly for better fit
        frame.setLocationRelativeTo(null);

        return frame;
    }

    /**
     * Creates the table panel without a header and with centered content.
     */
    private JPanel createTablePanel() {
        JPanel mainWrapper = new JPanel(new BorderLayout(10, 10));

        // 1. Data preparation: now using 4 columns
        // The first row: ["", "T", "F", "Dont Use"]
        Object[][] data = getTableContent();

        // Increase column names array size to 4
        String[] columns = {"", "", "", ""};

        JTable table = new JTable(data, columns);
        table.setTableHeader(null);

        // --- VISUAL STYLING ---
        table.setRowHeight(45);
        table.setShowGrid(true);
        table.setGridColor(Color.GRAY);
        table.setFont(new Font("Arial", Font.BOLD, 14));

        // Custom renderer for header-like appearance
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(JLabel.CENTER);

                // Highlight the first row (including "Dont Use") and the first column
                if (row == 0 || column == 0) {
                    c.setBackground(new Color(230, 230, 230)); // Slightly darker gray
                    c.setForeground(Color.BLACK);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.DARK_GRAY);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }

                return c;
            }
        });

        table.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // 2. Labels (Verification Engine and GNSS)
        JLabel topLabel = new JLabel("Verification Engine", JLabel.CENTER);
        topLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        topLabel.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 0));

        JLabel leftLabel = new JLabel("GNSS");
        leftLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.add(leftLabel);

        // 3. Assembly
        mainWrapper.add(topLabel, BorderLayout.NORTH);
        mainWrapper.add(leftPanel, BorderLayout.WEST);
        mainWrapper.add(table, BorderLayout.CENTER);

        JLabel captionLabel = new JLabel("Table 1: Agreement Matrix", JLabel.CENTER);
        captionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        mainWrapper.add(captionLabel, BorderLayout.SOUTH);

        return mainWrapper;
    }

    public Object[] @NotNull [] getTableContent() {
        int tt = 0, ft = 0, tf = 0, ff  = 0;

        for(int i = 0; i < reference.size(); i++){
            if((GNSS.get(i).getX() - reference.get(i).getX() < diff) && (GNSS.get(i).getY() - reference.get(i).getY() < diff)){
                if(verificationEngine.get(i).getX() - reference.get(i).getX() < diff) tt++;
                else tf++;
            }
           else{
               if(verificationEngine.get(i).getX() - reference.get(i).getX() < diff) ft++;
               else ff++;
            }
        }

        return new Object[][]{
                {"", "Valid", "Invalid", "Off"},
                {"Valid", tt, tf, 0},
                {"Invalid", ft, ff, 0}
        };
    }

    /**
     * Custom panel class for rendering the histogram.
     */
    private class HistogramPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int leftMargin = 60, bottomMargin = 60, rightMargin = 30, topMargin = 40;
            int graphWidth = w - leftMargin - rightMargin;
            int graphHeight = h - bottomMargin - topMargin;

            // 1. Draw Axes
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(leftMargin, h - bottomMargin, w - rightMargin, h - bottomMargin); // X
            g2.drawLine(leftMargin, topMargin, leftMargin, h - bottomMargin);             // Y

            // 2. Y-axis labels (0% to 100%)
            for (int i = 0; i <= 10; i++) {
                int yPos = h - bottomMargin - (i * graphHeight / 10);
                g2.drawLine(leftMargin - 5, yPos, leftMargin, yPos);
                String label = (i * 10) + "%";
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, leftMargin - labelWidth - 10, yPos + 5);
            }

            // 3. Process and Draw Bars
            double[] GNSSStats = getDistribution(GNSS);
            double[] verifierStats = getDistribution(verificationEngine);

            int groupCount = BIN_COUNT;
            int groupWidth = graphWidth / groupCount;
            int barWidth = (groupWidth / 2) - 10;

            for (int i = 0; i < groupCount; i++) {
                int groupX = leftMargin + i * groupWidth + 10;

                // GNSS Bar (Blue)
                drawBar(g2, groupX, h - bottomMargin, barWidth, GNSSStats[i], graphHeight, new Color(0, 0, 255));

                // Verifier Bar (Orange)
                drawBar(g2, groupX + barWidth + 2, h - bottomMargin, barWidth, verifierStats[i], graphHeight, new Color(0, 255, 0));

                // X-axis Labels
                g2.setColor(Color.BLACK);
                String label = String.format("%.1fm", (i + 1) * (MAX_ERROR_RANGE / BIN_COUNT));
                int labelX = groupX + (barWidth);
                g2.drawString(label, labelX - (g2.getFontMetrics().stringWidth(label)/2), h - bottomMargin + 20);
            }

            // Axis Names
            g2.drawString("Error Distance (meters)", leftMargin + graphWidth / 2 - 60, h - 15);
            g2.rotate(-Math.PI / 2);
            g2.drawString("Proportion (%)", - (topMargin + graphHeight / 2) - 40, leftMargin - 45);
            g2.rotate(Math.PI / 2);
        }

        private void drawBar(Graphics2D g2, int x, int baseLineY, int width, double percentage, int maxHeight, Color color) {
            int barHeight = (int) (percentage * maxHeight);
            int y = baseLineY - barHeight;
            g2.setColor(color);
            g2.fillRect(x, y, width, barHeight);
            g2.setColor(color.darker());
            g2.drawRect(x, y, width, barHeight);
        }
    }

    private double[] getDistribution(ArrayList<Odometry> dataList) {
        double[] bins = new double[BIN_COUNT];
        if (dataList.isEmpty() || reference.isEmpty()) return bins;
        double step = MAX_ERROR_RANGE / BIN_COUNT;
        for (int i = 0; i < dataList.size(); i++) {
            double error = Math.hypot(dataList.get(i).getX() - reference.get(i).getX(),
                    dataList.get(i).getY() - reference.get(i).getY());
            int binIndex = (int) (error / step);
            if (binIndex >= BIN_COUNT) binIndex = BIN_COUNT - 1;
            bins[binIndex]++;
        }
        for (int i = 0; i < BIN_COUNT; i++) {
            bins[i] /= dataList.size();
        }
        return bins;
    }

    /**
     * Standard main method to instantiate the class and start the app.
     */
    public static void main(String[] args) {
        new ExperimentalEvaluation().startApplication();
    }
}