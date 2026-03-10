package addressbook.io;

import addressbook.model.ContactDraft;

import java.util.LinkedHashSet;
import java.util.Set;

public class ImportedContact {
    private final ContactDraft draft;
    private final Set<String> groupNames;

    public ImportedContact(ContactDraft draft, Set<String> groupNames) {
        this.draft = draft;
        this.groupNames = new LinkedHashSet<>(groupNames);
    }

    public ContactDraft getDraft() {
        return draft;
    }

    public Set<String> getGroupNames() {
        return new LinkedHashSet<>(groupNames);
    }
}
