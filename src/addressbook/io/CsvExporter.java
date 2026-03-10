package addressbook.io;

import addressbook.model.Contact;
import addressbook.util.CsvUtil;
import addressbook.util.DateUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class CsvExporter {
    private static final List<String> HEADER = List.of(
            "name", "phone", "mobile", "imType", "imNumber", "email", "website",
            "birthday", "photoPath", "company", "homeAddress", "postalCode", "groups",
            "note", "pinyinOverride"
    );

    public void exportTo(Path file, List<Contact> contacts, Map<String, String> groupNameMap) throws IOException {
        List<List<String>> records = new ArrayList<>();
        records.add(HEADER);

        for (Contact contact : contacts) {
            StringJoiner groups = new StringJoiner("|");
            for (String gid : contact.getGroupIds()) {
                String name = groupNameMap.getOrDefault(gid, "");
                if (!name.isBlank()) {
                    groups.add(name);
                }
            }
            records.add(List.of(
                    safe(contact.getName()),
                    safe(contact.getPhone()),
                    safe(contact.getMobile()),
                    safe(contact.getImType()),
                    safe(contact.getImNumber()),
                    safe(contact.getEmail()),
                    safe(contact.getWebsite()),
                    DateUtil.format(contact.getBirthday()),
                    safe(contact.getPhotoPath()),
                    safe(contact.getCompany()),
                    safe(contact.getHomeAddress()),
                    safe(contact.getPostalCode()),
                    groups.toString(),
                    safe(contact.getNote()),
                    safe(contact.getPinyinOverride())
            ));
        }

        String csv = CsvUtil.toCsv(records);
        Files.writeString(file, csv, StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
