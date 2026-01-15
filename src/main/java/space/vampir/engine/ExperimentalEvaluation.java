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
    private final ArrayList<Odometry> sensorAndAI = new ArrayList<>();
    private final ArrayList<Odometry> verificationEngine = new ArrayList<>();

    public void addOdometries(Odometry ref, Odometry sen, Odometry ver){
        reference.add(ref);
        sensorAndAI.add(sen);
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
        JFrame frame = new JFrame("Experimental Evaluation and Analysis");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main layout: 1 row, 2 columns with 40px horizontal gap
        frame.setLayout(new GridLayout(1, 2, 40, 0));
        ((JPanel)frame.getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- LEFT COLUMN: TABLE + TEXT ---
        // Using BorderLayout here; placing content in NORTH prevents vertical stretching
        JPanel leftColumn = new JPanel(new BorderLayout());

        JPanel leftContentWrapper = new JPanel(new BorderLayout(0, 10));
        leftContentWrapper.add(createTablePanel(), BorderLayout.NORTH);

        JLabel tableLabel = new JLabel("<html><b>Table 1:</b> TBD</html>");
        tableLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftContentWrapper.add(tableLabel, BorderLayout.SOUTH);

        leftColumn.add(leftContentWrapper, BorderLayout.NORTH);

        // --- RIGHT COLUMN: HISTOGRAM + TEXT ---
        JPanel rightColumn = new JPanel(new BorderLayout(0, 10));
        rightColumn.add(new HistogramPanel(), BorderLayout.CENTER);

        JLabel graphLabel = new JLabel("<html><b>Figure 1:</b> TBD</html>");
        graphLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightColumn.add(graphLabel, BorderLayout.SOUTH);

        // Add columns to the frame
        frame.add(leftColumn);
        frame.add(rightColumn);

        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);

        return frame;
    }

    /**
     * Creates the table panel without a header and with centered content.
     */
    private JPanel createTablePanel() {
        JPanel tableContainer = new JPanel(new BorderLayout());

        // 1. Prepare data (including the first row which acts as a visual header)
        //TODO: 3 féle koordináta (odometry), határ diff, szétválogatás, dont uset kitalálni
        //funkciók
        //új odometry adása
        //frissítés (rajz update)
        //írja ki egy fájlba ( az odometrit és a kirjaozlást ) csv fájlba

        Object[][] data = getTableContent();

        // 2. Initialize column names with empty strings to avoid NullPointerException
        int columnCount = data[0].length;
        String[] columns = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columns[i] = "";
        }

        JTable table = new JTable(data, columns);

        // Disable the actual table header
        table.setTableHeader(null);

        // --- VISUAL STYLING ---
        table.setRowHeight(40);
        table.setShowGrid(true);
        table.setGridColor(Color.LIGHT_GRAY);

        // Center-align text in all cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        table.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        tableContainer.add(table, BorderLayout.CENTER);

        return tableContainer;
    }

    public Object[] @NotNull [] getTableContent() {
        int tt = 0, ft = 0, tf = 0, ff  = 0;

        for(int i = 0; i < reference.size(); i++){
            if((sensorAndAI.get(i).getX() - reference.get(i).getX() < 0.5) && (sensorAndAI.get(i).getY() - reference.get(i).getY() < 0.5)){
                if(verificationEngine.get(i).getX() - reference.get(i).getX() < 0.5) tt++;
                else tf++;
            }
           else{
               if(verificationEngine.get(i).getX() - reference.get(i).getX() < 0.5) ft++;
               else ff++;
            }
        }

        return new Object[][]{
                {"", "T", "F"},
                {"T", tt, tf},
                {"F", ft, ff}
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
            int margin = 30;

            // Draw axes
            g2.setColor(Color.BLACK);
            g2.drawLine(margin, h - margin, w - margin, h - margin); // X-axis
            g2.drawLine(margin, margin, margin, h - margin);         // Y-axis

            // Sample data for the histogram
            double[] values = {0.15, 0.45, 0.8, 0.65, 0.3, 0.1};
            int barCount = values.length;
            int availableWidth = w - 2 * margin;
            int barWidth = (availableWidth / barCount) - 15;
            int maxBarHeight = h - 2 * margin;

            for (int i = 0; i < barCount; i++) {
                int barHeight = (int) (values[i] * maxBarHeight);
                int x = margin + i * (availableWidth / barCount) + 7;
                int y = h - margin - barHeight;

                // Apply gradient fill for a modern look
                GradientPaint gp = new GradientPaint(x, y, new Color(100, 150, 255), x, y + barHeight, new Color(30, 80, 180));
                g2.setPaint(gp);
                g2.fillRect(x, y, barWidth, barHeight);

                // Draw bar outline
                g2.setColor(new Color(20, 50, 120));
                g2.drawRect(x, y, barWidth, barHeight);
            }
        }
    }

    /**
     * Standard main method to instantiate the class and start the app.
     */
    public static void main(String[] args) {
        new ExperimentalEvaluation().startApplication();
    }
}