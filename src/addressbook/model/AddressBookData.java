package addressbook.model;

import java.util.ArrayList;
import java.util.List;

public class AddressBookData {
    private int version = 1;
    private List<Group> groups = new ArrayList<>();
    private List<Contact> contacts = new ArrayList<>();

    public AddressBookData() {
    }

    public AddressBookData(AddressBookData other) {
        this.version = other.version;
        this.groups = new ArrayList<>();
        for (Group group : other.groups) {
            this.groups.add(new Group(group));
        }
        this.contacts = new ArrayList<>();
        for (Contact contact : other.contacts) {
            this.contacts.add(new Contact(contact));
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }
}
