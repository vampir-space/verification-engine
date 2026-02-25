package space.vampir.engine;

import space.vampir.engine.visualization.Visualization;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VisualStatRepresentation extends Visualization implements Observer {
    ExperimentalEvaluation evaluation;

    // --- GUI Components that require dynamic updates ---
    private DefaultTableModel tableModel;
    private HistogramPanel histogramPanel;
    private JLabel thresholdSliderValueLabel;
    private JLabel timeSliderValueLabel;
    private JLabel timeWindowSliderValueLabel;

    // Metric Labels
    private JLabel availabilityGNSSLabel;
    private JLabel availabilityVerificationEngineLabel;
    private JLabel availabilityImprovementLabel;
    private JLabel integrityGNSSLabel;
    private JLabel integrityVELabel;
    private JLabel integrityImprovementLabel;


    long actualTime;
    long timeWindow = 3;

    // Configuration constants
    private final double MAX_ERROR_RANGE = 2.5; // Max error to show on histogram (meters)
    private final int BIN_COUNT = 5;            // Number of bars in the histogram

    VisualStatRepresentation(ExperimentalEvaluation evaluation, boolean enabled) {
        super(enabled, new Dimension(950, 700));
        this.evaluation = evaluation;
        actualTime = evaluation.getEndTime();
        evaluation.attach(this);
    }

    VisualStatRepresentation(ExperimentalEvaluation evaluation) {
        this(evaluation, true);
    }

    @Override
    public void update() {
        updateDashboard();
    }

    @Override
    public void finish() {
        //todo close application
    }

    /**
     * Entry point to launch the application instance.
     */
    @Override
    public void startVisualization(Dimension dimension) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = createMainFrame(dimension);
            frame.setVisible(true);
        });
    }

    @Override
    public void updateVisualization() {
        updateDashboard();
    }

    /**
     * Creates and configures the main application window.
     */
    private JFrame createMainFrame(Dimension dimension) {
        double diff = evaluation.getDiff();

        JFrame frame = new JFrame("Experimental Evaluation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main layout: BorderLayout
        frame.setLayout(new BorderLayout());
        ((JPanel) frame.getContentPane()).setBorder(new EmptyBorder(10, 20, 20, 20));

        // --- 1. CENTER SECTION: METRICS & VISUALIZATIONS ---
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 40, 0));

        // --- LEFT COLUMN: TABLE + TEXT METRICS ---
        JPanel leftColumn = new JPanel(new BorderLayout());
        JPanel leftContentWrapper = new JPanel(new BorderLayout(0, 10));

        leftContentWrapper.add(createTablePanel(), BorderLayout.NORTH);   // Add Matrix Table
        leftContentWrapper.add(createMetricsPanel(), BorderLayout.CENTER); // Add Text Metrics

        leftColumn.add(leftContentWrapper, BorderLayout.NORTH);

        // --- RIGHT COLUMN: HISTOGRAM ---
        JPanel rightColumn = new JPanel(new BorderLayout(0, 10));
        histogramPanel = new HistogramPanel(); // Save reference for repainting
        rightColumn.add(histogramPanel, BorderLayout.CENTER);

        JLabel graphLabel = new JLabel("<html><b>Figure 1:</b> Error Distribution Percentage</html>", JLabel.CENTER);
        graphLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightColumn.add(graphLabel, BorderLayout.SOUTH);

        // Add columns to the content panel
        contentPanel.add(leftColumn);
        contentPanel.add(rightColumn);

        frame.add(contentPanel, BorderLayout.CENTER);

        // --- 2. BOTTOM SECTION: CONTROL PANEL (SLIDERS) ---

        // We use GridBagLayout to align the labels and sliders perfectly in columns
        JPanel bottomContainer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10); // Padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Row 1: Threshold Slider ---

        // 1. Label (Column 0) - Aligned to the right so it sticks to the slider
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END; // Right align text
        bottomContainer.add(new JLabel("Coordinate Error Threshold (diff): "), gbc);

        // 2. Slider (Column 1)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        thresholdSliderValueLabel = new JLabel(String.format("%.1f m", diff));
        thresholdSliderValueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JSlider thresholdSlider = new JSlider(0, 50, (int) (diff * 10));
        thresholdSlider.setMajorTickSpacing(10);
        thresholdSlider.setMinorTickSpacing(1);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPreferredSize(new Dimension(300, 50));

        thresholdSlider.addChangeListener(e -> {
            evaluation.setDiff(thresholdSlider.getValue() / 10.0);
            thresholdSliderValueLabel.setText(String.format("%.1f m", evaluation.getDiff()));
            updateDashboard();
        });

        bottomContainer.add(thresholdSlider, gbc);

        // 3. Value Label (Column 2) - Aligned to the left
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        bottomContainer.add(thresholdSliderValueLabel, gbc);

        // --- Row 2: Time Slider ---

        // 1. Label (Column 0)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END; // Right align text
        bottomContainer.add(new JLabel("Time: "), gbc);

        // 2. Slider (Column 1)
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        timeSliderValueLabel = new JLabel(String.format("%d", (int) actualTime));
        timeSliderValueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JSlider timeSlider = new JSlider((int) evaluation.getStartTime(), (int) evaluation.getEndTime(), (int) actualTime);
        timeSlider.setMajorTickSpacing(10);
        timeSlider.setMinorTickSpacing(1);
        timeSlider.setPaintTicks(true);
        timeSlider.setPreferredSize(new Dimension(300, 50));

        timeSlider.addChangeListener(e -> {
            actualTime = timeSlider.getValue();
            timeSliderValueLabel.setText(String.format("%d", (int) actualTime));
            updateDashboard();
        });

        bottomContainer.add(timeSlider, gbc);

        // 3. Value Label (Column 2)
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        bottomContainer.add(timeSliderValueLabel, gbc);

        // --- Row 3: Time window Slider ---

        // 1. Label (Column 0)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END; // Right align text
        bottomContainer.add(new JLabel("Time window: "), gbc);

        // 2. Slider (Column 1)
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        timeWindowSliderValueLabel = new JLabel(String.format("%d", (int) actualTime));
        timeWindowSliderValueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JSlider timeWindowSlider = new JSlider((int) evaluation.getStartTime(), (int) evaluation.getEndTime(), (int) actualTime);
        timeWindowSlider.setMajorTickSpacing(10);
        timeWindowSlider.setMinorTickSpacing(1);
        timeWindowSlider.setPaintTicks(true);
        timeWindowSlider.setPreferredSize(new Dimension(300, 50));

        timeWindowSlider.addChangeListener(e -> {
            timeWindow = timeWindowSlider.getValue();
            timeWindowSliderValueLabel.setText(String.format("%d", (int) timeWindow));
            updateDashboard();
        });

        bottomContainer.add(timeWindowSlider, gbc);

        // 3. Value Label (Column 2)
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        bottomContainer.add(timeWindowSliderValueLabel, gbc);

        // Add the organized container to the bottom of the frame
        frame.add(bottomContainer, BorderLayout.SOUTH);

        // Final frame settings
        frame.setSize(dimension);
        frame.setLocationRelativeTo(null);

        // Populate the dashboard with initial data
        updateDashboard();

        return frame;
    }

    /**
     * Initializes the panel containing the text-based metrics (Precision/Recall/Integrity).
     */
    private JPanel createMetricsPanel() {
        JPanel metricsPanel = new JPanel();
        metricsPanel.setLayout(new BoxLayout(metricsPanel, BoxLayout.Y_AXIS));
        metricsPanel.setBorder(new EmptyBorder(20, 10, 0, 0));

        // Initialize labels with placeholders
        availabilityGNSSLabel = new JLabel("GNSS Availability: -");
        availabilityVerificationEngineLabel = new JLabel("VE Availability: -");
        availabilityImprovementLabel = new JLabel("Availability improvement: -");
        integrityGNSSLabel = new JLabel("GNSS Integrity: -");
        integrityVELabel = new JLabel("VE Integrity: -");
        integrityImprovementLabel = new JLabel("Integrity improvement: -");

        Font metricsFont = new Font("SansSerif", Font.BOLD, 14);
        JLabel[] labels = {availabilityGNSSLabel, availabilityVerificationEngineLabel, availabilityImprovementLabel,
                integrityGNSSLabel, integrityVELabel, integrityImprovementLabel};

        // Apply styling and add to panel
        for (JLabel lbl : labels) {
            lbl.setFont(metricsFont);
            metricsPanel.add(lbl);
            // Add a spacer after the Availability section
            if (lbl == availabilityImprovementLabel)
                metricsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        return metricsPanel;
    }

    /**
     * Creates the table panel with a custom model and renderer.
     */
    private JPanel createTablePanel() {
        JPanel mainWrapper = new JPanel(new BorderLayout(10, 10));

        // Column names (empty because we use custom headers in the first row/column)
        String[] columns = {"", "", "", ""};

        // Initialize table model (empty initially)
        tableModel = new DefaultTableModel(new Object[][]{}, columns);

        JTable table = new JTable(tableModel);
        table.setTableHeader(null); // Hide default header
        table.setRowHeight(45);
        table.setShowGrid(true);
        table.setGridColor(Color.GRAY);
        table.setFont(new Font("Arial", Font.BOLD, 14));

        // Custom renderer for visual styling (gray background for headers)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(JLabel.CENTER);

                // Highlight the first row and the first column to look like headers
                if (row == 0 || column == 0) {
                    c.setBackground(new Color(230, 230, 230));
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

        // External Labels for the Table
        JLabel topLabel = new JLabel("Verification Engine", JLabel.CENTER);
        topLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        topLabel.setBorder(BorderFactory.createEmptyBorder(0, 60, 0, 0));

        JLabel leftLabel = new JLabel("GNSS");
        leftLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.add(leftLabel);

        mainWrapper.add(topLabel, BorderLayout.NORTH);
        mainWrapper.add(leftPanel, BorderLayout.WEST);
        mainWrapper.add(table, BorderLayout.CENTER);

        JLabel captionLabel = new JLabel("Table 1: Agreement Matrix", JLabel.CENTER);
        captionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        mainWrapper.add(captionLabel, BorderLayout.SOUTH);

        return mainWrapper;
    }

    /**
     * CORE LOGIC: Updates all UI components based on the current 'diff' value.
     * This is called whenever the slider is moved.
     */
    private void updateDashboard() {
        // 1. Recalculate the matrix based on the new threshold
        int[][] matrix = evaluation.getMatrix(actualTime, timeWindow);

        // 2. Update the Table
        // Reset rows and add new data
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"", "Valid", "Invalid", "Off"});
        tableModel.addRow(new Object[]{"Valid", matrix[0][0], matrix[0][1], matrix[0][2]});
        tableModel.addRow(new Object[]{"Invalid", matrix[1][0], matrix[1][1], matrix[1][2]});

        // 3. Update the Metrics Labels
        int sum = matrix[0][0] + matrix[0][1] + matrix[1][0] + matrix[1][1];
        if (sum == 0) sum = 1; // Prevent division by zero

        // Calculation logic
        // 1. Calculate Availability Metrics
        double gnssAvail = (double) (matrix[0][0] + matrix[0][1]) / sum;
        double veAvail = (double) (matrix[0][0] + matrix[1][0]) / sum;

        // 2. Calculate Availability Improvement (Safe from division by zero)
        // If GNSS availability is 0, we define improvement as 0 to avoid Infinity/NaN
        double availImp = (gnssAvail == 0) ? 0 : (veAvail - gnssAvail) / gnssAvail;

        // 3. Calculate Integrity Metrics
        double gnssInteg = (double) (matrix[0][0] + matrix[0][1] + matrix[0][2]) / sum;
        double veInteg = (double) (matrix[0][0] + matrix[1][0] + matrix[0][2] + matrix[1][2]) / sum;

        // 4. Calculate Integrity Improvement (Safe from division by zero)
        double integImp = (gnssInteg == 0) ? 0 : (veInteg - gnssInteg) / gnssInteg;

        // Set text
        availabilityGNSSLabel.setText(String.format("GNSS Availability: %.2f", gnssAvail));
        availabilityVerificationEngineLabel.setText(String.format("VE Availability: %.2f", veAvail));
        availabilityImprovementLabel.setText(String.format("Availability improvement: %d%%", (int) (availImp * 100)));

        integrityGNSSLabel.setText(String.format("GNSS Integrity: %.2f", gnssInteg));
        integrityVELabel.setText(String.format("VE Integrity: %.2f", veInteg));
        integrityImprovementLabel.setText(String.format("Integrity improvement: %d%%", (int) (integImp * 100)));

        // 4. Update the Histogram
        if (histogramPanel != null) {
            histogramPanel.repaint();
        }
    }

    /**
     * Custom JPanel for drawing the error distribution histogram.
     */
    class HistogramPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int leftMargin = 60, bottomMargin = 60, rightMargin = 30, topMargin = 40;
            int graphWidth = w - leftMargin - rightMargin;
            int graphHeight = h - bottomMargin - topMargin;

            // Draw Axes
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(leftMargin, h - bottomMargin, w - rightMargin, h - bottomMargin); // X Axis
            g2.drawLine(leftMargin, topMargin, leftMargin, h - bottomMargin);             // Y Axis

            // Y-axis labels (0% to 100%)
            for (int i = 0; i <= 10; i++) {
                int yPos = h - bottomMargin - (i * graphHeight / 10);
                g2.drawLine(leftMargin - 5, yPos, leftMargin, yPos);
                String label = (i * 10) + "%";
                g2.drawString(label, leftMargin - g2.getFontMetrics().stringWidth(label) - 10, yPos + 5);
            }

            // Calculate distribution
            double[] GNSSStats = evaluation.getDistribution(evaluation.getGNSS(), BIN_COUNT, MAX_ERROR_RANGE, actualTime, timeWindow);
            double[] verifierStats = evaluation.getDistribution(evaluation.getVerificationEngine(), BIN_COUNT, MAX_ERROR_RANGE, actualTime, timeWindow);

            int groupCount = BIN_COUNT;
            int groupWidth = graphWidth / groupCount;
            int barWidth = (groupWidth / 2) - 10;

            // Draw bars
            for (int i = 0; i < groupCount; i++) {
                int groupX = leftMargin + i * groupWidth + 10;

                // Draw GNSS Bar (Blue)
                drawBar(g2, groupX, h - bottomMargin, barWidth, GNSSStats[i], graphHeight, new Color(0, 0, 255));

                // Draw Verifier Bar (Green)
                drawBar(g2, groupX + barWidth + 2, h - bottomMargin, barWidth, verifierStats[i], graphHeight, new Color(0, 255, 0));

                // X-axis Labels
                g2.setColor(Color.BLACK);
                String label = String.format("%.1fm", (i + 1) * (MAX_ERROR_RANGE / BIN_COUNT));
                g2.drawString(label, groupX + barWidth - (g2.getFontMetrics().stringWidth(label) / 2), h - bottomMargin + 20);
            }

            // Axis Titles
            g2.drawString("Error Distance (meters)", leftMargin + graphWidth / 2 - 60, h - 15);

            // Rotate text for Y-axis
            g2.rotate(-Math.PI / 2);
            g2.drawString("Proportion (%)", -(topMargin + graphHeight / 2) - 40, leftMargin - 45);
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
}
