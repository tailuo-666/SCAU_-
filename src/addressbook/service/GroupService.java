package addressbook.service;

import addressbook.model.Group;
import addressbook.util.IdGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GroupService {
    private final AddressBookStore store;

    public GroupService(AddressBookStore store) {
        this.store = store;
    }

    public Group createGroup(String name) throws IOException {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be empty.");
        }

        return store.mutate(data -> {
            for (Group group : data.getGroups()) {
                if (group.getName() != null && group.getName().equalsIgnoreCase(normalized)) {
                    return new Group(group);
                }
            }

            int maxOrder = -1;
            for (Group group : data.getGroups()) {
                maxOrder = Math.max(maxOrder, group.getOrder());
            }
            Group created = new Group(IdGenerator.newId(), normalized, maxOrder + 1);
            data.getGroups().add(created);
            data.getGroups().sort(Comparator.comparingInt(Group::getOrder).thenComparing(g -> g.getName().toLowerCase(Locale.ROOT)));
            return new Group(created);
        });
    }

    public void deleteGroup(String groupId) throws IOException {
        if (groupId == null || groupId.isBlank()) {
            return;
        }
        store.mutate(data -> {
            data.getGroups().removeIf(group -> Objects.equals(group.getId(), groupId));
            data.getContacts().forEach(contact -> contact.getGroupIds().remove(groupId));
            int order = 0;
            for (Group group : data.getGroups()) {
                group.setOrder(order++);
            }
            return null;
        });
    }

    public List<Group> listGroups() {
        return store.read(data -> {
            List<Group> groups = new ArrayList<>();
            data.getGroups().stream()
                    .sorted(Comparator.comparingInt(Group::getOrder).thenComparing(Group::getName, String.CASE_INSENSITIVE_ORDER))
                    .forEach(group -> groups.add(new Group(group)));
            return groups;
        });
    }

    public Group findByName(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return null;
        }
        return store.read(data -> {
            for (Group group : data.getGroups()) {
                if (groupName.equalsIgnoreCase(group.getName())) {
                    return new Group(group);
                }
            }
            return null;
        });
    }
}
