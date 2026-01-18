package space.vampir.engine;

import org.jetbrains.annotations.NotNull;
import space.vampir.engine.message.Odometry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;

public class ExperimentalEvaluation {
    //TODO object id needed?
    private final HashMap<Long, Odometry> reference = new HashMap<>();
    private final HashMap<Long, Odometry> sensorAndAI = new HashMap<>();
    private final HashMap<Long, Odometry> verificationEngine = new HashMap<>();

    Long startTime;
    Long endTime;
    Long granularity;

    double diff = 0.5;

    public ExperimentalEvaluation(Long startTime, Long endTime, Long granularity){
        this.startTime = startTime;
        this.endTime = endTime;
        this.granularity = granularity;
    }

    public void addOdometries(ArrayList<Odometry> ref, ArrayList<Odometry> sen, ArrayList<Odometry> ver){
        //todo check if they belong to the same time
        for(Odometry refOdometry: ref){
            reference.put(refOdometry.getTime(), refOdometry);
        }
        for(Odometry senOdometry: sen){
            sensorAndAI.put(senOdometry.getTime(), senOdometry);
        }
        for(Odometry verOdometry: ver){
            verificationEngine.put(verOdometry.getTime(), verOdometry);
        }
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

        JLabel precisionLabel = new JLabel("Precision: 0.00");
        JLabel recallLabel = new JLabel("Recall: 0.00");

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

        frame.setSize(1200, 800); // Increased size slightly for better fit
        frame.setLocationRelativeTo(null);

        return frame;
    }

    /**
     * Creates the table panel without a header and with centered content.
     */
    private JPanel createTablePanel() {
        // Fő tároló panel, függőleges elrendezéssel
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Konfúziós Mátrix - 1 (Példa adatokkal, nullákkal)
        Object[][] cm1Data = getConfusionTableContent(sensorAndAI);
        mainContainer.add(createMatrixComponent("Confusion Matrix - Sensor + AI", cm1Data, "Actual", "Predicted"));
        mainContainer.add(Box.createVerticalStrut(30)); // Távolság a mátrixok között

        // 2. Konfúziós Mátrix - 2 (Példa adatokkal, nullákkal)
        Object[][] cm2Data = getConfusionTableContent(verificationEngine);
        mainContainer.add(createMatrixComponent("Confusion Matrix - Verification Engine", cm2Data, "Actual", "Predicted"));
        mainContainer.add(Box.createVerticalStrut(30));

        // 3. Agreement Matrix (Az eredeti táblázatod adatai alapján)
        Object[][] agreementData = getAgreementTableContent();
        mainContainer.add(createMatrixComponent("Agreement Matrix", agreementData, "Sensor", "Verification Engine"));

        return mainContainer;
    }

    /**
     * Segédfüggvény egy stílusos mátrix panel létrehozásához feliratokkal
     */
    private JPanel createMatrixComponent(String title, Object[][] data, String leftLabelText, String topLabelText) {
        JPanel wrapper = new JPanel(new BorderLayout(5, 5));

        // Oszlopnevek (üresen hagyjuk, mert az első sor funkcionál fejlécként)
        String[] columns = new String[data[0].length];
        for (int i = 0; i < columns.length; i++) columns[i] = "";

        JTable table = new JTable(data, columns);
        table.setTableHeader(null);
        table.setRowHeight(40);
        table.setShowGrid(true);
        table.setGridColor(Color.GRAY);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setPreferredScrollableViewportSize(table.getPreferredSize());

        // Renderer a fejléc stílushoz (első sor és első oszlop kiemelése)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(JLabel.CENTER);

                if (row == 0 || column == 0) {
                    c.setBackground(new Color(235, 235, 235));
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

        // Feliratok elhelyezése
        JLabel topLabel = new JLabel(topLabelText, JLabel.CENTER);
        topLabel.setFont(new Font("Arial", Font.ITALIC, 12));

        JLabel leftLabel = new JLabel(leftLabelText);
        leftLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        JPanel leftPanel = new JPanel(new GridBagLayout());
        // Elforgatás nélkül, vagy GridBag-el pozicionálva balra
        leftPanel.add(leftLabel);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        JLabel captionLabel = new JLabel(title, JLabel.CENTER);
        captionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        captionLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));

        // Összeállítás
        wrapper.add(captionLabel, BorderLayout.NORTH);

        JPanel innerPanel = new JPanel(new BorderLayout());
        innerPanel.add(topLabel, BorderLayout.NORTH);
        innerPanel.add(leftPanel, BorderLayout.WEST);
        innerPanel.add(table, BorderLayout.CENTER);

        wrapper.add(innerPanel, BorderLayout.CENTER);

        return wrapper;
    }

    public Object[] @NotNull [] getAgreementTableContent() {
        //TODO: true/false negatives?
        int tt = 0, ft = 0, tf = 0, ff  = 0;

        for (Map.Entry<Long, Odometry> entry : reference.entrySet()) {
            Long time = entry.getKey();
            if((sensorAndAI.get(time).getX() - reference.get(time).getX() < diff) && (sensorAndAI.get(time).getY() - reference.get(time).getY() < diff)){
                if(verificationEngine.get(time).getX() - reference.get(time).getX() < diff) tt++;
                else tf++;
            }
           else{
               if(verificationEngine.get(time).getX() - reference.get(time).getX() < diff) ft++;
               else ff++;
            }
        }

        return new Object[][]{
                {"", "T", "F", "Don't Use"},
                {"T", tt, tf, 0},
                {"F", ft, ff, 0}
        };
    }

    public Object[] @NotNull [] getConfusionTableContent(HashMap<Long, Odometry> odometryHashMap) {
       int tp = 0, fp = 0, tn = 0, fn  = 0;

        for (long time = startTime; time <= endTime; time=time+granularity) {
            if(reference.get(time) != null){
                if(odometryHashMap.get(time) != null && (odometryHashMap.get(time).getX() - reference.get(time).getX()) < diff)  tp++;
                else fn++;
            }
            else{
                if(odometryHashMap.get(time) == null)  tn++;
                else fp++;
            }
        }

        return new Object[][]{
                {"", "T", "F"},
                {"T", tp, fn},
                {"F", fp, tn}
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

            int w = getWidth();
            int h = getHeight();

            // Megnövelt margók a feliratoknak
            int leftMargin = 50;
            int bottomMargin = 50;
            int rightMargin = 30;
            int topMargin = 30;

            int graphWidth = w - leftMargin - rightMargin;
            int graphHeight = h - bottomMargin - topMargin;

            // 1. Tengelyek rajzolása
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(leftMargin, h - bottomMargin, w - rightMargin, h - bottomMargin); // X-tengely
            g2.drawLine(leftMargin, topMargin, leftMargin, h - bottomMargin);             // Y-tengely

            // 2. Y-tengely feliratozása (1-től 10-ig)
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            for (int i = 0; i <= 10; i++) {
                int yPos = h - bottomMargin - (i * graphHeight / 10);

                // Kis jelölő vonalak (ticks)
                g2.drawLine(leftMargin - 5, yPos, leftMargin, yPos);

                // Számok (igazítva a vonalhoz)
                String label = String.valueOf(i);
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, leftMargin - labelWidth - 10, yPos + 5);
            }

            // Y-tengely neve (függőlegesen)
            g2.rotate(-Math.PI / 2);
            g2.drawString("TBD", - (topMargin + graphHeight / 2) - 30, leftMargin - 35);
            g2.rotate(Math.PI / 2);

            // 3. Adatok és X-tengely feliratozása
            double[] values = getHistogramData(); // Feltételezzük, hogy 0.0 és 1.0 közötti értékek az Y-hoz képest
            int barCount = values.length;
            int barWidth = (graphWidth / barCount) - 10;

            for (int i = 0; i < barCount; i++) {
                // Oszlop rajzolása
                int barHeight = (int) (values[i] * graphHeight);
                int x = leftMargin + i * (graphWidth / barCount) + 5;
                int y = h - bottomMargin - barHeight;

                GradientPaint gp = new GradientPaint(x, y, new Color(100, 150, 255), x, y + barHeight, new Color(30, 80, 180));
                g2.setPaint(gp);
                g2.fillRect(x, y, barWidth, barHeight);

                g2.setColor(new Color(20, 50, 120));
                g2.drawRect(x, y, barWidth, barHeight);

                // X-tengely feliratok (0-100% elosztva)
                g2.setColor(Color.BLACK);
                String percentLabel = (i * (100 / (barCount - 1))) + "%";
                int labelX = x + (barWidth / 2) - (g2.getFontMetrics().stringWidth(percentLabel) / 2);
                g2.drawString(percentLabel, labelX, h - bottomMargin + 20);
            }

            // X-tengely neve
            g2.drawString("TBD(%)", leftMargin + graphWidth / 2 - 40, h - 10);
        }
    }

    private double[] getHistogramData() {

        return new double[]{0.15, 0.45, 0.8, 0.65, 0.3, 0.1};
    }

    private double calculatePrecision(){
        var data = getAgreementTableContent();
        return (double)data[1][1]/((double) data[1][1]+ (double) data[0][1]);
    }

    /**
     * Standard main method to instantiate the class and start the app.
     */
    public static void main(String[] args) {
        new ExperimentalEvaluation(0L, 0L, 0L).startApplication();
    }
}