package addressbook.service;

import addressbook.io.ImportedContact;
import addressbook.model.ContactDraft;
import addressbook.model.Group;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ImportService {
    private final GroupService groupService;
    private final ContactService contactService;

    public ImportService(GroupService groupService, ContactService contactService) {
        this.groupService = groupService;
        this.contactService = contactService;
    }

    public int appendImportedContacts(List<ImportedContact> importedContacts) throws IOException {
        if (importedContacts == null || importedContacts.isEmpty()) {
            return 0;
        }

        Map<String, Group> groupByName = new LinkedHashMap<>();
        for (Group group : groupService.listGroups()) {
            groupByName.put(normalize(group.getName()), group);
        }

        int success = 0;
        for (ImportedContact imported : importedContacts) {
            ContactDraft original = imported.getDraft();
            ContactDraft draft = copyDraft(original);

            Set<String> groupIds = new LinkedHashSet<>();
            for (String groupName : imported.getGroupNames()) {
                String normalized = normalize(groupName);
                if (normalized.isBlank()) {
                    continue;
                }
                Group existing = groupByName.get(normalized);
                if (existing == null) {
                    existing = groupService.createGroup(groupName.trim());
                    groupByName.put(normalized, existing);
                }
                groupIds.add(existing.getId());
            }
            draft.setGroupIds(groupIds);

            contactService.create(draft);
            success++;
        }
        return success;
    }

    private ContactDraft copyDraft(ContactDraft source) {
        ContactDraft copy = new ContactDraft();
        copy.setName(source.getName());
        copy.setPhone(source.getPhone());
        copy.setMobile(source.getMobile());
        copy.setImType(source.getImType());
        copy.setImNumber(source.getImNumber());
        copy.setEmail(source.getEmail());
        copy.setWebsite(source.getWebsite());
        copy.setBirthday(source.getBirthday());
        copy.setPhotoPath(source.getPhotoPath());
        copy.setCompany(source.getCompany());
        copy.setHomeAddress(source.getHomeAddress());
        copy.setPostalCode(source.getPostalCode());
        copy.setNote(source.getNote());
        copy.setPinyinOverride(source.getPinyinOverride());
        copy.setGroupIds(new LinkedHashSet<>(source.getGroupIds()));
        return copy;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
