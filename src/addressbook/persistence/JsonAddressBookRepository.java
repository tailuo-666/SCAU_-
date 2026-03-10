package addressbook.persistence;

import addressbook.model.AddressBookData;
import addressbook.model.Contact;
import addressbook.model.Group;
import addressbook.util.DateUtil;
import addressbook.util.SimpleJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonAddressBookRepository implements AddressBookRepository {
    private static final DateTimeFormatter BACKUP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Path dataFile;
    private final Path dataDir;
    private final Path photosDir;

    public JsonAddressBookRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.dataDir = dataFile.getParent() == null ? Path.of(".") : dataFile.getParent();
        this.photosDir = this.dataDir.resolve("photos");
    }

    public Path getDataDirectory() {
        return dataDir;
    }

    public Path getPhotosDirectory() {
        return photosDir;
    }

    @Override
    public AddressBookData load() throws IOException {
        Files.createDirectories(dataDir);
        Files.createDirectories(photosDir);

        if (!Files.exists(dataFile)) {
            return new AddressBookData();
        }

        String raw = Files.readString(dataFile, StandardCharsets.UTF_8);
        if (raw.isBlank()) {
            return new AddressBookData();
        }

        try {
            Object parsed = SimpleJson.parse(raw);
            if (!(parsed instanceof Map<?, ?> root)) {
                throw new IllegalArgumentException("Root must be a JSON object");
            }
            return fromMap(castMap(root));
        } catch (Exception ex) {
            backupCorruptedFile(raw);
            return new AddressBookData();
        }
    }

    @Override
    public void save(AddressBookData data) throws IOException {
        Files.createDirectories(dataDir);
        Files.createDirectories(photosDir);

        Map<String, Object> root = toMap(data);
        String json = SimpleJson.stringify(root);

        Path temp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        Files.writeString(temp, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        try {
            Files.move(temp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void backupCorruptedFile(String content) {
        try {
            String stamp = java.time.LocalDateTime.now().format(BACKUP_FORMATTER);
            Path backup = dataFile.resolveSibling(dataFile.getFileName() + ".broken." + stamp);
            Files.writeString(backup, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException ignored) {
        }
    }

    private Map<String, Object> toMap(AddressBookData data) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", data.getVersion());

        List<Object> groups = new ArrayList<>();
        for (Group group : data.getGroups()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", safe(group.getId()));
            m.put("name", safe(group.getName()));
            m.put("order", group.getOrder());
            groups.add(m);
        }
        root.put("groups", groups);

        List<Object> contacts = new ArrayList<>();
        for (Contact c : data.getContacts()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", safe(c.getId()));
            m.put("name", safe(c.getName()));
            m.put("phone", safe(c.getPhone()));
            m.put("mobile", safe(c.getMobile()));
            m.put("imType", safe(c.getImType()));
            m.put("imNumber", safe(c.getImNumber()));
            m.put("email", safe(c.getEmail()));
            m.put("website", safe(c.getWebsite()));
            m.put("birthday", DateUtil.format(c.getBirthday()));
            m.put("photoPath", safe(c.getPhotoPath()));
            m.put("company", safe(c.getCompany()));
            m.put("homeAddress", safe(c.getHomeAddress()));
            m.put("postalCode", safe(c.getPostalCode()));
            m.put("groupIds", new ArrayList<>(c.getGroupIds()));
            m.put("note", safe(c.getNote()));
            m.put("pinyinFull", safe(c.getPinyinFull()));
            m.put("pinyinInitials", safe(c.getPinyinInitials()));
            m.put("pinyinOverride", safe(c.getPinyinOverride()));
            contacts.add(m);
        }
        root.put("contacts", contacts);
        return root;
    }

    private AddressBookData fromMap(Map<String, Object> root) {
        AddressBookData data = new AddressBookData();
        data.setVersion(toInt(root.get("version"), 1));

        List<Group> groups = new ArrayList<>();
        for (Object item : toList(root.get("groups"))) {
            if (!(item instanceof Map<?, ?> rawGroup)) {
                continue;
            }
            Map<String, Object> m = castMap(rawGroup);
            Group group = new Group();
            group.setId(toString(m.get("id")));
            group.setName(toString(m.get("name")));
            group.setOrder(toInt(m.get("order"), groups.size()));
            if (!group.getId().isBlank()) {
                groups.add(group);
            }
        }
        data.setGroups(groups);

        List<Contact> contacts = new ArrayList<>();
        for (Object item : toList(root.get("contacts"))) {
            if (!(item instanceof Map<?, ?> rawContact)) {
                continue;
            }
            Map<String, Object> m = castMap(rawContact);
            Contact c = new Contact();
            c.setId(toString(m.get("id")));
            c.setName(toString(m.get("name")));
            c.setPhone(toString(m.get("phone")));
            c.setMobile(toString(m.get("mobile")));
            c.setImType(toString(m.get("imType")));
            c.setImNumber(toString(m.get("imNumber")));
            c.setEmail(toString(m.get("email")));
            c.setWebsite(toString(m.get("website")));
            LocalDate birthday = DateUtil.parseFlexible(toString(m.get("birthday")));
            c.setBirthday(birthday);
            c.setPhotoPath(toString(m.get("photoPath")));
            c.setCompany(toString(m.get("company")));
            c.setHomeAddress(toString(m.get("homeAddress")));
            c.setPostalCode(toString(m.get("postalCode")));

            Set<String> groupIds = new LinkedHashSet<>();
            for (Object gid : toList(m.get("groupIds"))) {
                String value = toString(gid);
                if (!value.isBlank()) {
                    groupIds.add(value);
                }
            }
            c.setGroupIds(groupIds);

            c.setNote(toString(m.get("note")));
            c.setPinyinFull(toString(m.get("pinyinFull")));
            c.setPinyinInitials(toString(m.get("pinyinInitials")));
            c.setPinyinOverride(toString(m.get("pinyinOverride")));
            if (!c.getId().isBlank()) {
                contacts.add(c);
            }
        }
        data.setContacts(contacts);
        return data;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> toList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(toString(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String toString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
