package addressbook.service;

import addressbook.model.Contact;
import addressbook.model.Group;
import addressbook.util.NameSortUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryService {
    private final AddressBookStore store;

    public QueryService(AddressBookStore store) {
        this.store = store;
    }

    public List<Contact> query(GroupFilter filter, SortOption sortOption) {
        return store.read(data -> {
            List<Contact> result = new ArrayList<>();
            for (Contact contact : data.getContacts()) {
                if (matchesFilter(contact, filter)) {
                    result.add(new Contact(contact));
                }
            }
            sort(result, sortOption);
            return result;
        });
    }

    public Map<String, List<Contact>> classifyByInitial(List<Contact> contacts) {
        Map<String, List<Contact>> grouped = new LinkedHashMap<>();
        for (Contact contact : contacts) {
            String key = NameSortUtil.classifyLabel(contact);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(contact);
        }
        return grouped;
    }

    public Map<String, String> groupIdNameMap() {
        return store.read(data -> {
            Map<String, String> map = new LinkedHashMap<>();
            for (Group group : data.getGroups()) {
                map.put(group.getId(), group.getName());
            }
            return map;
        });
    }

    private void sort(List<Contact> contacts, SortOption sortOption) {
        if (sortOption == null || sortOption == SortOption.NAME_ASC) {
            contacts.sort(NameSortUtil.contactComparator());
        }
    }

    private boolean matchesFilter(Contact contact, GroupFilter filter) {
        if (filter == null || filter.getType() == GroupFilter.Type.ALL) {
            return true;
        }
        if (filter.getType() == GroupFilter.Type.UNGROUPED) {
            return contact.getGroupIds().isEmpty();
        }
        if (filter.getType() == GroupFilter.Type.GROUP) {
            return contact.getGroupIds().contains(filter.getGroupId());
        }
        return true;
    }
}
