import addressbook.persistence.JsonAddressBookRepository;
import addressbook.service.AddressBookStore;
import addressbook.service.ContactService;
import addressbook.service.GroupService;
import addressbook.service.ImportService;
import addressbook.service.MembershipService;
import addressbook.service.QueryService;
import addressbook.service.SearchService;
import addressbook.ui.AddressBookFrame;

import javax.swing.*;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Path dataFile = Path.of("data", "addressbook.json");
                JsonAddressBookRepository repository = new JsonAddressBookRepository(dataFile);
                AddressBookStore store = new AddressBookStore(repository);

                ContactService contactService = new ContactService(
                        store,
                        repository.getDataDirectory(),
                        repository.getPhotosDirectory()
                );
                GroupService groupService = new GroupService(store);
                MembershipService membershipService = new MembershipService(store);
                QueryService queryService = new QueryService(store);
                SearchService searchService = new SearchService(queryService);
                ImportService importService = new ImportService(groupService, contactService);

                AddressBookFrame frame = new AddressBookFrame(
                        contactService,
                        groupService,
                        membershipService,
                        queryService,
                        searchService,
                        importService,
                        repository.getDataDirectory()
                );
                frame.setVisible(true);
            } catch (Exception ex) {
                String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                JOptionPane.showMessageDialog(null, msg, "启动失败", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
