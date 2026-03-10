package addressbook.service;

import addressbook.model.AddressBookData;
import addressbook.model.Contact;
import addressbook.model.Group;
import addressbook.persistence.AddressBookRepository;
import addressbook.util.IdGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class AddressBookStore {
    @FunctionalInterface
    public interface Reader<T> {
        T read(AddressBookData data);
    }

    @FunctionalInterface
    public interface Mutator<T> {
        T mutate(AddressBookData data) throws IOException;
    }

    private final AddressBookRepository repository;
    private final AddressBookData data;

    public AddressBookStore(AddressBookRepository repository) throws IOException {
        this.repository = repository;
        this.data = repository.load();
        normalize();
        repository.save(data);
    }

    public synchronized <T> T read(Reader<T> reader) {
        return reader.read(data);
    }

    public synchronized <T> T mutate(Mutator<T> mutator) throws IOException {
        T result = mutator.mutate(data);
        repository.save(data);
        return result;
    }

    public synchronized AddressBookData snapshot() {
        return new AddressBookData(data);
    }

    private void normalize() {
        if (data.getGroups() == null) {
            data.setGroups(new ArrayList<>());
        }
        if (data.getContacts() == null) {
            data.setContacts(new ArrayList<>());
        }

        Set<String> groupIds = new HashSet<>();
        Iterator<Group> groupIterator = data.getGroups().iterator();
        int order = 0;
        while (groupIterator.hasNext()) {
            Group group = groupIterator.next();
            if (group == null) {
                groupIterator.remove();
                continue;
            }
            if (group.getId() == null || group.getId().isBlank()) {
                group.setId(IdGenerator.newId());
            }
            if (group.getName() == null) {
                group.setName("");
            }
            if (!groupIds.add(group.getId())) {
                groupIterator.remove();
                continue;
            }
            group.setOrder(order++);
        }

        Iterator<Contact> contactIterator = data.getContacts().iterator();
        Set<String> contactIds = new HashSet<>();
        while (contactIterator.hasNext()) {
            Contact contact = contactIterator.next();
            if (contact == null) {
                contactIterator.remove();
                continue;
            }
            if (contact.getId() == null || contact.getId().isBlank()) {
                contact.setId(IdGenerator.newId());
            }
            if (!contactIds.add(contact.getId())) {
                contactIterator.remove();
                continue;
            }
            if (contact.getName() == null) {
                contact.setName("");
            }
            if (contact.getGroupIds() == null) {
                contact.setGroupIds(new java.util.LinkedHashSet<>());
            }
            contact.getGroupIds().removeIf(id -> !groupIds.contains(id));
        }
    }
}
