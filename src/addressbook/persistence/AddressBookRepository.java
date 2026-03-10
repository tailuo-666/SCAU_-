package addressbook.persistence;

import addressbook.model.AddressBookData;

import java.io.IOException;

public interface AddressBookRepository {
    AddressBookData load() throws IOException;

    void save(AddressBookData data) throws IOException;
}
