package addressbook.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class DateUtil {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<DateTimeFormatter> PARSE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.BASIC_ISO_DATE
    );

    private DateUtil() {
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(ISO);
    }

    public static LocalDate parseFlexible(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim();
        for (DateTimeFormatter formatter : PARSE_FORMATS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
