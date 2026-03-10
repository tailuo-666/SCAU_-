package addressbook.util;

import addressbook.model.Contact;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public final class NameSortUtil {
    private static final Collator COLLATOR = Collator.getInstance(Locale.CHINA);

    private NameSortUtil() {
    }

    public static Comparator<Contact> contactComparator() {
        return (a, b) -> {
            int byName = COLLATOR.compare(safe(a.getName()), safe(b.getName()));
            if (byName != 0) {
                return byName;
            }
            return safe(a.getId()).compareTo(safe(b.getId()));
        };
    }

    public static String classifyLabel(Contact contact) {
        if (contact == null) {
            return "#";
        }
        String name = safe(contact.getName());
        if (name.isBlank()) {
            return "#";
        }
        char c = name.charAt(0);
        if (Character.isLetter(c)) {
            return String.valueOf(Character.toUpperCase(c));
        }
        char initial = PinyinUtil.initialForChar(c);
        if (initial == 0) {
            return "#";
        }
        return String.valueOf(Character.toUpperCase(initial));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
