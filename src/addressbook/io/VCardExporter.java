package addressbook.io;

import addressbook.model.Contact;
import addressbook.util.DateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class VCardExporter {
    public void exportTo(Path file, List<Contact> contacts, Map<String, String> groupNameMap) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Contact contact : contacts) {
            sb.append("BEGIN:VCARD\r\n");
            sb.append("VERSION:3.0\r\n");

            String name = safe(contact.getName());
            appendProperty(sb, "N", ";" + escape(name) + ";;;");
            appendProperty(sb, "FN", escape(name));
            appendProperty(sb, "TEL;TYPE=HOME", escape(contact.getPhone()));
            appendProperty(sb, "TEL;TYPE=CELL", escape(contact.getMobile()));
            appendProperty(sb, "X-IM-TYPE", escape(contact.getImType()));
            appendProperty(sb, "X-IM-NUMBER", escape(contact.getImNumber()));
            appendProperty(sb, "EMAIL;TYPE=INTERNET", escape(contact.getEmail()));
            appendProperty(sb, "URL", escape(contact.getWebsite()));
            appendProperty(sb, "BDAY", DateUtil.format(contact.getBirthday()));
            appendProperty(sb, "PHOTO;VALUE=URI", escape(contact.getPhotoPath()));
            appendProperty(sb, "ORG", escape(contact.getCompany()));

            String adr = ";;" + escape(contact.getHomeAddress()) + ";;;" + escape(contact.getPostalCode()) + ";";
            appendProperty(sb, "ADR;TYPE=HOME", adr);

            StringJoiner categories = new StringJoiner(",");
            for (String gid : contact.getGroupIds()) {
                String groupName = groupNameMap.getOrDefault(gid, "");
                if (!groupName.isBlank()) {
                    categories.add(escape(groupName));
                }
            }
            appendProperty(sb, "CATEGORIES", categories.toString());
            appendProperty(sb, "NOTE", escape(contact.getNote()));
            appendProperty(sb, "X-PINYIN-OVERRIDE", escape(contact.getPinyinOverride()));
            sb.append("END:VCARD\r\n");
        }

        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private void appendProperty(StringBuilder sb, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String line = key + ":" + value;
        for (String folded : foldLine(line)) {
            sb.append(folded).append("\r\n");
        }
    }

    private List<String> foldLine(String line) {
        List<String> lines = new ArrayList<>();
        int limit = 72;
        if (line.length() <= limit) {
            lines.add(line);
            return lines;
        }

        int start = 0;
        boolean first = true;
        while (start < line.length()) {
            int end = Math.min(start + limit, line.length());
            String chunk = line.substring(start, end);
            if (first) {
                lines.add(chunk);
                first = false;
            } else {
                lines.add(" " + chunk);
            }
            start = end;
        }
        return lines;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
