package addressbook.ui;

import addressbook.model.Contact;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContactTableModel extends AbstractTableModel {
    private List<Contact> contacts = new ArrayList<>();
    private List<ContactColumn> visibleColumns = ContactColumn.defaultColumns();
    private Map<String, String> groupNameMap = new LinkedHashMap<>();

    public void setContacts(List<Contact> contacts) {
        this.contacts = new ArrayList<>(contacts);
        fireTableDataChanged();
    }

    public List<Contact> getContacts() {
        return new ArrayList<>(contacts);
    }

    public Contact getContactAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= contacts.size()) {
            return null;
        }
        return contacts.get(rowIndex);
    }

    public void setVisibleColumns(List<ContactColumn> columns) {
        this.visibleColumns = new ArrayList<>(columns);
        fireTableStructureChanged();
    }

    public List<ContactColumn> getVisibleColumns() {
        return new ArrayList<>(visibleColumns);
    }

    public void setGroupNameMap(Map<String, String> groupNameMap) {
        this.groupNameMap = new LinkedHashMap<>(groupNameMap);
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return contacts.size();
    }

    @Override
    public int getColumnCount() {
        return visibleColumns.size();
    }

    @Override
    public String getColumnName(int column) {
        return visibleColumns.get(column).title();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Contact contact = contacts.get(rowIndex);
        ContactColumn column = visibleColumns.get(columnIndex);
        return column.value(contact, groupNameMap);
    }
}
