package addressbook.service;

import addressbook.model.Contact;
import addressbook.model.ContactDraft;
import addressbook.model.Group;
import addressbook.util.IdGenerator;
import addressbook.util.PinyinUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ContactService {
    private final AddressBookStore store;
    private final Path dataDir;
    private final Path photosDir;

    public ContactService(AddressBookStore store, Path dataDir, Path photosDir) {
        this.store = store;
        this.dataDir = dataDir;
        this.photosDir = photosDir;
    }

    public Contact create(ContactDraft draft) throws IOException {
        validateDraft(draft);
        return store.mutate(data -> {
            Contact contact = new Contact();
            contact.setId(IdGenerator.newId());
            applyDraft(contact, draft, data.getGroups());
            data.getContacts().add(contact);
            return new Contact(contact);
        });
    }

    public Contact update(String contactId, ContactDraft draft) throws IOException {
        validateDraft(draft);
        return store.mutate(data -> {
            Contact target = findContactById(data.getContacts(), contactId);
            if (target == null) {
                throw new IllegalArgumentException("Contact not found: " + contactId);
            }
            applyDraft(target, draft, data.getGroups());
            return new Contact(target);
        });
    }

    public void delete(String contactId) throws IOException {
        store.mutate(data -> {
            data.getContacts().removeIf(c -> Objects.equals(c.getId(), contactId));
            return null;
        });
    }

    public Contact getById(String contactId) {
        return store.read(data -> {
            Contact contact = findContactById(data.getContacts(), contactId);
            return contact == null ? null : new Contact(contact);
        });
    }

    public List<Contact> listAll() {
        return store.read(data -> {
            List<Contact> list = new ArrayList<>();
            for (Contact contact : data.getContacts()) {
                list.add(new Contact(contact));
            }
            return list;
        });
    }

    private void applyDraft(Contact contact, ContactDraft draft, List<Group> groups) throws IOException {
        contact.setName(safe(draft.getName()));
        contact.setPhone(safe(draft.getPhone()));
        contact.setMobile(safe(draft.getMobile()));
        contact.setImType(safe(draft.getImType()));
        contact.setImNumber(safe(draft.getImNumber()));
        contact.setEmail(safe(draft.getEmail()));
        contact.setWebsite(safe(draft.getWebsite()));
        contact.setBirthday(draft.getBirthday());
        contact.setCompany(safe(draft.getCompany()));
        contact.setHomeAddress(safe(draft.getHomeAddress()));
        contact.setPostalCode(safe(draft.getPostalCode()));
        contact.setNote(safe(draft.getNote()));
        contact.setPinyinOverride(safe(draft.getPinyinOverride()));

        PinyinUtil.PinyinResult pinyin = PinyinUtil.build(contact.getName(), contact.getPinyinOverride());
        contact.setPinyinFull(pinyin.full());
        contact.setPinyinInitials(pinyin.initials());

        Set<String> validGroupIds = new LinkedHashSet<>();
        Map<String, Group> groupMap = new LinkedHashMap<>();
        for (Group group : groups) {
            groupMap.put(group.getId(), group);
        }
        for (String gid : draft.getGroupIds()) {
            if (groupMap.containsKey(gid)) {
                validGroupIds.add(gid);
            }
        }
        contact.setGroupIds(validGroupIds);

        String resolved = resolvePhotoPath(draft.getPhotoPath(), contact.getPhotoPath());
        contact.setPhotoPath(resolved);
    }

    private String resolvePhotoPath(String inputPath, String oldPath) throws IOException {
        if (inputPath == null || inputPath.isBlank()) {
            return "";
        }
        String normalizedInput = inputPath.replace('\\', '/').trim();
        if (normalizedInput.equals(oldPath)) {
            return normalizedInput;
        }

        Path input = Path.of(normalizedInput);
        Path candidate = input;

        if (!input.isAbsolute()) {
            Path underData = dataDir.resolve(input).normalize();
            if (Files.exists(underData) && !Files.isDirectory(underData)) {
                candidate = underData;
            } else {
                Path underCwd = Path.of(normalizedInput).toAbsolutePath().normalize();
                if (Files.exists(underCwd) && !Files.isDirectory(underCwd)) {
                    candidate = underCwd;
                }
            }
        }

        if (!Files.exists(candidate) || Files.isDirectory(candidate)) {
            return normalizedInput;
        }

        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        Path normalizedPhotosDir = photosDir.toAbsolutePath().normalize();

        if (normalizedCandidate.startsWith(normalizedPhotosDir)) {
            return toStoragePath(normalizedCandidate);
        }

        Files.createDirectories(photosDir);
        String originalName = normalizedCandidate.getFileName().toString();
        String ext = "";
        int idx = originalName.lastIndexOf('.');
        if (idx >= 0) {
            ext = originalName.substring(idx);
        }
        Path target = photosDir.resolve(IdGenerator.newId() + ext);
        Files.copy(normalizedCandidate, target, StandardCopyOption.REPLACE_EXISTING);
        return toStoragePath(target.toAbsolutePath().normalize());
    }

    private String toStoragePath(Path absolutePath) {
        Path relative = dataDir.toAbsolutePath().normalize().relativize(absolutePath);
        return relative.toString().replace('\\', '/');
    }

    private static Contact findContactById(List<Contact> contacts, String contactId) {
        for (Contact contact : contacts) {
            if (Objects.equals(contact.getId(), contactId)) {
                return contact;
            }
        }
        return null;
    }

    private static void validateDraft(ContactDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Contact draft is required.");
        }
        if (safe(draft.getName()).isBlank()) {
            throw new IllegalArgumentException("Contact name is required.");
        }
        LocalDate birthday = draft.getBirthday();
        if (birthday != null && birthday.getYear() < 1900) {
            throw new IllegalArgumentException("Birthday year is too old.");
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
