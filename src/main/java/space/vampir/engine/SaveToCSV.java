package space.vampir.engine;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class SaveToCSV implements Observer {
    ExperimentalEvaluation evaluation;

    SaveToCSV(ExperimentalEvaluation evaluation) {
        this.evaluation = evaluation;
    }

    @Override
    public void update() {
        // Update logic if needed
    }

    @Override
    public void finish() {
        int[][] matrix = evaluation.getMatrix();

        // --- Calculation Logic ---

        // 3. Update the Metrics Labels
        int sum = 0;
        // Ensure we don't get an IndexOutOfBounds exception if the matrix is smaller than expected
        if (matrix.length > 1 && matrix[0].length > 1) {
            sum = matrix[0][0] + matrix[0][1] + matrix[1][0] + matrix[1][1];
        }
        if (sum == 0) sum = 1; // Prevent division by zero

        double gnssAvail = (double) (matrix[0][0] + matrix[0][1]) / sum;
        double veAvail = (double) (matrix[0][0] + matrix[1][0]) / sum;

        double availImp  = (gnssAvail == 0) ? 0 : (veAvail - gnssAvail) / gnssAvail;

        // 3. Calculate Integrity Metrics
        double gnssInteg = (double)(matrix[0][0] + matrix[0][1] + matrix[0][2]) / sum;
        double veInteg = (double)(matrix[0][0] + matrix[1][0] + matrix[0][2] + matrix[1][2]) / sum;

        // 4. Calculate Integrity Improvement (Safe from division by zero)
        double integImp = (gnssInteg == 0) ? 0 : (veInteg - gnssInteg) / gnssInteg;


        // --- CSV SAVING IMPLEMENTATION ---
        String filename = "evaluation_result.csv";

        try (FileWriter writer = new FileWriter(filename)) {

            // 1. Section: Save Matrix
            writer.write("Agreement Matrix\n");

            // Matrix Header
            writer.write("GNSS\\VE;Valid;Invalid;Off\n");

            writer.write("Valid"); // Row identifier
            for (int j = 0; j < matrix[0].length; j++) {
                writer.write(";" + matrix[0][j]);
            }
            writer.write("\n");

            writer.write("Invalid"); // Row identifier
            for (int j = 0; j < matrix[1].length; j++) {
                writer.write(";" + matrix[1][j]);
            }
            writer.write("\n");


            writer.write("\n"); // Empty line as separator

            // 2. Section: Calculated Metrics
            writer.write("Calculated Metrics;Percentage\n");

            writer.write(String.format(Locale.US, "GNSS Availability;%.2f\n", gnssAvail));
            writer.write(String.format(Locale.US, "VE Availability;%.2f\n", veAvail));
            writer.write(String.format(Locale.US, "Availability improvement;%.2f%%\n", availImp * 100));
            writer.write(String.format(Locale.US, "GNSS Integrity;%.2f\n", gnssInteg));
            writer.write(String.format(Locale.US, "VE Integrity;%.2f\n", veInteg));
            writer.write(String.format(Locale.US, "Integrity Improvement;%.2f%%\n", integImp * 100));

            System.out.println("CSV saved successfully: " + filename);

        } catch (IOException e) {
            System.err.println("Error occurred while saving CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to write a single metric row
    private void writeMetricRow(FileWriter writer, String label, double value) throws IOException {
        // Using Locale.US to ensure dot (.) is used as decimal separator,
        // avoiding conflict with the semicolon (;) CSV delimiter.
        // Format: Name ; Value (0.1234) ; Percentage (12.34%)
        String line = String.format(Locale.US, "%s;;%.2f%%\n", label, value * 100);
        writer.write(line);
    }
}