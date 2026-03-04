package space.vampir.engine;

import space.vampir.engine.verification.UpdatedVerificationCase;
import space.vampir.engine.visualization.Visualization;
import space.vampir.engine.visualization.controller.ControllerObserver;
import space.vampir.engine.visualization.controller.KeyBindingManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class VisualStatRepresentation extends Visualization implements Observer, ControllerObserver {
    ExperimentalEvaluation evaluation;
    private final boolean timeSliderEnabled;

    // --- GUI Components that require dynamic updates ---
    private final JFrame frame = new JFrame("Experimental Evaluation");
    private final JPanel panel = new JPanel();
    private DefaultTableModel tableModel;
    private HistogramPanel histogramPanel;
    private JSlider timeSlider;
    private JSlider timeWindowSlider;
    private JLabel thresholdSliderValueLabel;
    private JLabel timeSliderValueLabel;
    private JLabel timeWindowSliderValueLabel;

    // Metric Labels
    private JLabel integrityRiskOfGNSSLabel;
    private JLabel integrityRiskImprovementLabel;

    int actualTime;
    int timeWindow;

    // Configuration constants
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1200, 700);
    private final double MAX_ERROR_RANGE = 2.5; // Max error to show on histogram (meters)
    private final int BIN_COUNT = 5;            // Number of bars in the histogram

    VisualStatRepresentation(ExperimentalEvaluation evaluation, boolean enabled, boolean timeSliderEnabled) {
        super(enabled, DEFAULT_WINDOW_SIZE);
        this.evaluation = evaluation;
        this.timeSliderEnabled = timeSliderEnabled;
        actualTime = Math.max(0, evaluation.getSize() - 1);
        timeWindow = evaluation.getSize();
        evaluation.attach(this);
    }

    VisualStatRepresentation(ExperimentalEvaluation evaluation) {
        this(evaluation, true, true);
    }

    @Override
    public void update() {
        updateDashboard();
    }

    @Override
    public void finish() {
        frame.dispose();
    }

    /**
     * Entry point to launch the application instance.
     */
    @Override
    public void startVisualization(Dimension dimension) {
        SwingUtilities.invokeLater(() -> {
            createMainPanel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setPreferredSize(dimension);
            frame.setContentPane(panel);
            frame.pack();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = frame.getSize();
            frame.setLocation(0, screenSize.height - frameSize.height);

            frame.setVisible(true);
        });
    }

    @Override
    public void updateVisualization() {
        updateDashboard();
    }

    @Override
    public void doVisualize(UpdatedVerificationCase updatedVerificationCase) {
        long time = updatedVerificationCase.scenario().time();
        evaluation.addOdometries(
                Map.of(time, updatedVerificationCase.groundTruth()),
                Map.of(time, updatedVerificationCase.scenario().odometry()),
                Map.of(time, updatedVerificationCase.updatedByVerificationEngine())
        );
    }

    @Override
    public void registerHotkeys(KeyBindingManager keyBindingManager) {
        keyBindingManager.registerDefaultHotkeys(panel);
    }

    @Override
    public void select(long time, int index) {
        actualTime = index;
        if (timeSlider != null) {
            timeSlider.setValue(index);
        }
        if (timeWindowSlider != null) {
            boolean timeWindowSliderAtMax = timeWindowSlider.getValue() == timeWindowSlider.getMaximum();
            int timeWindowMax = Math.max(1, Math.min(evaluation.getSize(), evaluation.getSizeBefore(time)));
            timeWindowSlider.setMaximum(timeWindowMax);
            if (timeWindowSliderAtMax) {
                timeWindowSlider.setValue(timeWindowMax);
            }
        }
    }

    @Override
    public void sizeChanged(long maxTime, int size) {
        if (timeSlider != null) {
            timeSlider.setMaximum(size - 1);
        }
    }

    /**
     * Creates and configures the main application window.
     */
    private void createMainPanel() {
        double diff = evaluation.getDiff();

        // Main layout: BorderLayout
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 20, 20, 20));

        // --- 1. CENTER SECTION: METRICS & VISUALIZATIONS ---
        JPanel contentPanel = new JPanel(new BorderLayout(40, 0));

        // --- LEFT COLUMN: TABLE + TEXT METRICS ---
        JPanel leftColumn = new JPanel(new BorderLayout());
        JPanel leftContentWrapper = new JPanel(new BorderLayout(0, 10));

        leftContentWrapper.add(createTablePanel(), BorderLayout.NORTH);   // Add Matrix Table
        leftContentWrapper.add(createMetricsPanel(), BorderLayout.CENTER); // Add Text Metrics

        leftColumn.add(leftContentWrapper, BorderLayout.NORTH);

        // --- RIGHT COLUMN: HISTOGRAM ---
        JPanel rightColumn = new JPanel(new BorderLayout(0, 10));

        // FIX THE DIAGRAM WIDTH (e.g., to 450 pixels) so it doesn't stretch with the window!
        rightColumn.setPreferredSize(new Dimension(450, 0));

        histogramPanel = new HistogramPanel(); // Save reference for repainting
        rightColumn.add(histogramPanel, BorderLayout.CENTER);

        JLabel graphLabel = new JLabel("<html><b>Figure 1:</b> Error Distribution Percentage</html>", JLabel.CENTER);
        graphLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightColumn.add(graphLabel, BorderLayout.SOUTH);

        // --- ADDING TO CONTENT ---
        // The table goes into CENTER (occupying all remaining space)
        contentPanel.add(leftColumn, BorderLayout.CENTER);
        // The diagram goes into EAST (sticks to the right with a fixed 450px width)
        contentPanel.add(rightColumn, BorderLayout.EAST);

        panel.add(contentPanel, BorderLayout.CENTER);

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

        timeSliderValueLabel = new JLabel(String.format("%d", actualTime));
        timeSliderValueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        timeSlider = new JSlider(0, Math.max(0, evaluation.getSize() - 1), actualTime);
        timeSlider.setMajorTickSpacing(10);
        timeSlider.setMinorTickSpacing(1);
        timeSlider.setPaintTicks(true);
        timeSlider.setPreferredSize(new Dimension(300, 50));
        if (!timeSliderEnabled) {
            timeSlider.setEnabled(false);
        }

        timeSlider.addChangeListener(e -> {
            actualTime = timeSlider.getValue();
            timeSliderValueLabel.setText(String.format("%d", actualTime));
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

        var maxSize = Math.max(1, evaluation.getSize());
        timeWindowSliderValueLabel = new JLabel(String.format("%d", maxSize));
        timeWindowSliderValueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        timeWindowSlider = new JSlider(1, maxSize, maxSize);
        timeWindowSlider.setMajorTickSpacing(10);
        timeWindowSlider.setMinorTickSpacing(1);
        timeWindowSlider.setPaintTicks(true);
        timeWindowSlider.setPreferredSize(new Dimension(300, 50));

        timeWindowSlider.addChangeListener(e -> {
            timeWindow = timeWindowSlider.getValue();
            timeWindowSliderValueLabel.setText(String.format("%d", timeWindow));
            updateDashboard();
        });

        bottomContainer.add(timeWindowSlider, gbc);

        // 3. Value Label (Column 2)
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        bottomContainer.add(timeWindowSliderValueLabel, gbc);

        // Add the organized container to the bottom of the frame
        panel.add(bottomContainer, BorderLayout.SOUTH);

        // Populate the dashboard with initial data
        updateDashboard();
    }

    /**
     * Initializes the panel containing the text-based metrics (Precision/Recall/Integrity).
     */
    private JPanel createMetricsPanel() {
        JPanel metricsPanel = new JPanel();
        metricsPanel.setLayout(new BoxLayout(metricsPanel, BoxLayout.Y_AXIS));
        metricsPanel.setBorder(new EmptyBorder(20, 10, 0, 0));

        // Initialize labels with placeholders
        integrityRiskOfGNSSLabel = new JLabel("Integrity Risk of GNSS: ");
        integrityRiskImprovementLabel = new JLabel("Integrity Risk Improvement: ");


        Font metricsFont = new Font("SansSerif", Font.BOLD, 14);
        JLabel[] labels = {integrityRiskOfGNSSLabel, integrityRiskImprovementLabel};

        // Apply styling and add to panel
        for (JLabel lbl : labels) {
            lbl.setFont(metricsFont);
            metricsPanel.add(lbl);
        }

        return metricsPanel;
    }

    /**
     * Creates the table panel with a custom model and renderer.
     */
    private JPanel createTablePanel() {
        JPanel mainWrapper = new JPanel(new BorderLayout(10, 10));

        // 1. START WITH 8 COLUMNS IMMEDIATELY!
        // This prevents the need to call setColumnCount() in updateDashboard(),
        // which would reset our custom renderer and column widths every time.
        String[] columns = {"0", "1", "2", "3", "4", "5", "6", "7"};
        tableModel = new DefaultTableModel(new Object[][]{}, columns);

        JTable table = new JTable(tableModel);
        table.setTableHeader(null); // Hide default header
        table.setRowHeight(35);     // Slightly smaller row height to fit better
        table.setEnabled(false);    // Make table non-editable

        // 2. MANDATORY: Disable default grid so our custom borders are visible
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // 3. CUSTOM RENDERER TO SIMULATE CELL MERGING
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Reset alignments for safety on every redraw
                c.setHorizontalAlignment(JLabel.CENTER);
                c.setVerticalAlignment(JLabel.CENTER);

                // Set colors (Headers are gray, data cells are white)
                if (row < 5 || column < 5) {
                    c.setBackground(new Color(230, 230, 230));
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }

                // Default border thickness for all cells (top, left, bottom, right)
                int t = 1, l = 1, b = 1, r = 1;

                // --- VISUAL MERGING LOGIC ---

                // A) Top-left large empty box (No internal grid lines)
                if (row < 5 && column < 5) {
                    t = (row == 0) ? 1 : 0; // Only top border for the first row
                    l = (column == 0) ? 1 : 0; // Only left border for the first column
                    b = (row == 4) ? 1 : 0; // Only bottom border for the last row of the box
                    r = (column == 4) ? 1 : 0; // Only right border for the last column of the box
                }

                // B) Merge "VerificationEngine" (Row: 0, Columns: 5, 6, 7) - 3 columns wide
                if (row == 0) {
                    if (column == 5) {
                        r = 0;
                    } // Col 5 is just empty background
                    if (column == 6) {
                        l = 0;
                        r = 0;
                        c.setHorizontalAlignment(JLabel.RIGHT);
                    } // "Verification "
                    if (column == 7) {
                        l = 0;
                        c.setHorizontalAlignment(JLabel.LEFT);
                    } // "Engine"
                }

                // C) Merge "USE" and "%" horizontally (Rows: 1 and 2, Columns: 5, 6)
                if (row == 1 || row == 2) {
                    if (column == 5) {
                        r = 0;
                        c.setHorizontalAlignment(JLabel.RIGHT);
                    } // "US", "9"
                    if (column == 6) {
                        l = 0;
                        c.setHorizontalAlignment(JLabel.LEFT);
                    }  // "E", "8%"
                }


                // D) Merge "GNSS" (Column: 0, Rows: 5, 6, 7)
                if (column == 0 && row >= 5) {
                    t = (row == 5) ? 1 : 0;
                    b = (row == 7) ? 1 : 0;
                }

                // E) Merge "USE" and "100%" vertically (Columns: 1, 2, Rows: 5, 6)
                if ((column == 1 || column == 2) && (row == 5 || row == 6)) {
                    if (row == 5) {
                        b = 0;
                        // Gravity trick: Push text to the bottom edge so it sits on the center line!
                        c.setVerticalAlignment(JLabel.BOTTOM);
                    }
                    if (row == 6) {
                        t = 0;
                    }
                }

                // G) Merge "2%" vertically under "DoNotUse" (Column: 7, Rows: 2, 3, 4)
                if (column == 7 && (row >= 2 && row <= 4)) {
                    t = (row == 2) ? 1 : 0; // Only keep the top border for the first row of the merge
                    b = (row == 4) ? 1 : 0; // Only keep the bottom border for the last row of the merge
                }

                // Final step: Apply the computed border configuration
                c.setBorder(BorderFactory.createMatteBorder(t, l, b, r, Color.GRAY));
                return c;
            }
        };

        // 4. GUARANTEE: Apply the custom renderer to all 8 columns via a loop!
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        /// 5. SET COLUMN WIDTHS TO CREATE A MATHEMATICAL CENTER
        // Col 5 (82) + Col 6 (83) = 165px -> Perfect center for 2-column merges!
        // Col 5 (82) + Col 6 (83) + Col 7 (165) = 330px -> Perfect center for 3-column merges!
        table.getColumnModel().getColumn(5).setPreferredWidth(82);
        table.getColumnModel().getColumn(6).setPreferredWidth(83);
        table.getColumnModel().getColumn(7).setPreferredWidth(165);

        // Outer border for the entire table
        table.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Add caption label below the table
        JLabel captionLabel = new JLabel("Table 1: Agreement Matrix", JLabel.CENTER);
        captionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Assemble the panel
        mainWrapper.add(table, BorderLayout.CENTER);
        mainWrapper.add(captionLabel, BorderLayout.SOUTH);

        return mainWrapper;
    }

    /**
     * CORE LOGIC: Updates all UI components based on the current 'diff' value.
     * This is called whenever the slider is moved.
     */
    private void updateDashboard() {
        SwingUtilities.invokeLater(() -> {
            if (this.tableModel == null) {
                return; // dashboard not initialized yet, skip update
            }

            // 1. Recalculate the matrix based on the new threshold
            int[][] matrix = evaluation.getMatrix(actualTime, timeWindow);

            // 2. Update the Table
            // Since the new table structure has 8 columns, we need to adjust the column count
            // (Note: Make sure your specific DefaultTableModel initialization allows this)
            tableModel.setRowCount(0); // Clear existing rows

            // --- TABLE HEADER SECTION (Verification Engine breakdown) ---
            tableModel.addRow(new Object[]{"", "", "", "", "", "", "Verification ", "Engine"});

            // We split "USE" and "%" across columns 5 and 6 to perfectly center them
            tableModel.addRow(new Object[]{"", "", "", "", "", "US", "E", "DoNotUse"});

            int sum = matrix[0][0] + matrix[0][1] + matrix[0][2] + matrix[1][0] + matrix[1][1] + matrix[1][2] + matrix[2][0] + matrix[2][1] + matrix[2][2];

            // Note for dynamic data later: you can use substring to split your percentage variable!
            int dontUseVE = (int) ((((double) matrix[0][2] + (double) matrix[1][2] + (double) matrix[2][2]) / (double) sum * 100));
            int useVE = 100 - dontUseVE;
            int tensPlace = useVE / 10;
            int onesPlace = useVE - tensPlace * 10;

            tableModel.addRow(new Object[]{"", "", "", "", "", tensPlace, onesPlace + "%", ""});

            tableModel.addRow(new Object[]{"", "", "", "", "", "VALID", "Misleading", dontUseVE + "%"});
            int validVE = (int) ((((double) matrix[0][0] + (double) matrix[1][0] + (double) matrix[2][0]) / ((double) matrix[0][0] + (double) matrix[1][0] + (double) matrix[2][0] + (double) matrix[0][1] + (double) matrix[1][1] + (double) matrix[2][1])) * 100);
            int misleadingVE = 100 - validVE;

            tableModel.addRow(new Object[]{"", "", "", "", "", validVE + "%", misleadingVE + "%", ""});

            // --- TABLE DATA SECTION (GNSS breakdown) ---

            int validGNSS = (int) ((((double) matrix[0][0] + (double) matrix[0][1] + (double) matrix[0][2]) / ((double) matrix[0][0] + (double) matrix[0][1] + (double) matrix[0][2] + (double) matrix[1][0] + (double) matrix[1][1] + (double) matrix[1][2])) * 100);
            int misleadingGNSS = 100 - validGNSS;
            int dontUseGNSS = (int) ((((double) matrix[2][0] + (double) matrix[2][1] + (double) matrix[2][2]) / sum) * 100);
            int useGNSS = 100 - dontUseGNSS;
            tableModel.addRow(new Object[]{
                    "", "USE", useGNSS + "%", "VALID", validGNSS + "%", matrix[0][0], matrix[0][1], matrix[0][2]
            });
            tableModel.addRow(new Object[]{
                    "GNSS", "", "", "Misleading", misleadingGNSS + "%", matrix[1][0], matrix[1][1], matrix[1][2]
            });

            tableModel.addRow(new Object[]{
                    "", "DoNotUse", dontUseGNSS + "%", "", "", matrix[2][0], matrix[2][1], matrix[2][2]
            });

            // 3. Update the Metrics Labels

            // Calculation logic
            double risk = (((double) matrix[1][0] + (double) matrix[1][2])/(double) sum);

            // Set text
            integrityRiskOfGNSSLabel.setText(String.format("Integrity risk of GNSS: %d%%", misleadingGNSS));
            integrityRiskImprovementLabel.setText(String.format("Integrity risk improvement: %.2f", risk));

            // 4. Update the Histogram
            if (histogramPanel != null) {
                histogramPanel.repaint();
            }
        });
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
