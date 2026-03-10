package addressbook.util;

import java.util.ArrayList;
import java.util.List;

public final class CsvUtil {
    private CsvUtil() {
    }

    public static List<List<String>> parse(String content) {
        List<List<String>> records = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return records;
        }

        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(ch);
                }
                continue;
            }

            if (ch == '"') {
                inQuotes = true;
                continue;
            }
            if (ch == ',') {
                row.add(cell.toString());
                cell.setLength(0);
                continue;
            }
            if (ch == '\n') {
                row.add(cell.toString());
                records.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
                continue;
            }
            if (ch == '\r') {
                continue;
            }
            cell.append(ch);
        }

        row.add(cell.toString());
        if (!(row.size() == 1 && row.get(0).isEmpty() && records.isEmpty())) {
            records.add(row);
        }
        return records;
    }

    public static String toCsv(List<List<String>> records) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            List<String> row = records.get(i);
            for (int j = 0; j < row.size(); j++) {
                if (j > 0) {
                    sb.append(',');
                }
                sb.append(escapeCell(row.get(j)));
            }
            if (i < records.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static String escapeCell(String cell) {
        String value = cell == null ? "" : cell;
        boolean needQuote = value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"");
        if (!needQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
