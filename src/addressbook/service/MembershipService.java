package addressbook.service;

import addressbook.model.Contact;
import addressbook.model.Group;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MembershipService {
    private final AddressBookStore store;

    public MembershipService(AddressBookStore store) {
        this.store = store;
    }

    public void addContactsToGroups(List<String> contactIds, List<String> groupIds) throws IOException {
        Set<String> contactIdSet = new HashSet<>(contactIds);
        Set<String> groupIdSet = new HashSet<>(groupIds);

        store.mutate(data -> {
            Set<String> validGroupIds = new HashSet<>();
            for (Group group : data.getGroups()) {
                if (groupIdSet.contains(group.getId())) {
                    validGroupIds.add(group.getId());
                }
            }
            for (Contact contact : data.getContacts()) {
                if (contactIdSet.contains(contact.getId())) {
                    contact.getGroupIds().addAll(validGroupIds);
                }
            }
            return null;
        });
    }

    public void removeContactsFromGroup(List<String> contactIds, String groupId) throws IOException {
        if (groupId == null || groupId.isBlank()) {
            return;
        }
        Set<String> contactIdSet = new HashSet<>(contactIds);
        store.mutate(data -> {
            for (Contact contact : data.getContacts()) {
                if (contactIdSet.contains(contact.getId())) {
                    contact.getGroupIds().removeIf(id -> Objects.equals(id, groupId));
                }
            }
            return null;
        });
    }
}
