package com.newgen.loanorigination.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvExportUtility {

    private CsvExportUtility() {
    }

    public static File writeCsv(File targetFile, List<String> headers, List<List<String>> rows) throws IOException {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            // Write headers
            if (headers != null && !headers.isEmpty()) {
                writer.write(toCsvLine(headers));
                writer.newLine();
            }

            // Write rows
            if (rows != null) {
                for (List<String> row : rows) {
                    writer.write(toCsvLine(row));
                    writer.newLine();
                }
            }
        }

        return targetFile;
    }

    private static String toCsvLine(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeSpecialCharacters(values.get(i)));
        }
        return sb.toString();
    }

    private static String escapeSpecialCharacters(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'") || data.contains("\n")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}
