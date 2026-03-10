package addressbook.ui;

import addressbook.io.CsvExporter;
import addressbook.io.CsvImporter;
import addressbook.io.ImportedContact;
import addressbook.io.VCardExporter;
import addressbook.io.VCardImporter;
import addressbook.model.Contact;
import addressbook.model.ContactDraft;
import addressbook.model.Group;
import addressbook.service.ContactService;
import addressbook.service.GroupFilter;
import addressbook.service.GroupService;
import addressbook.service.ImportService;
import addressbook.service.MembershipService;
import addressbook.service.QueryService;
import addressbook.service.SearchService;
import addressbook.service.SortOption;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddressBookFrame extends JFrame {
    private final ContactService contactService;
    private final GroupService groupService;
    private final MembershipService membershipService;
    private final QueryService queryService;
    private final SearchService searchService;
    private final ImportService importService;
    private final Path dataDirectory;

    private final CsvImporter csvImporter = new CsvImporter();
    private final CsvExporter csvExporter = new CsvExporter();
    private final VCardImporter vCardImporter = new VCardImporter();
    private final VCardExporter vCardExporter = new VCardExporter();

    private final DefaultListModel<GroupListItem> groupListModel = new DefaultListModel<>();
    private final JList<GroupListItem> groupList = new JList<>(groupListModel);
    private final ContactTableModel tableModel = new ContactTableModel();
    private final JTable contactTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JLabel statusLabel = new JLabel("就绪");

    private GroupFilter currentFilter = GroupFilter.all();
    private List<Contact> currentContacts = new ArrayList<>();

    public AddressBookFrame(ContactService contactService,
                            GroupService groupService,
                            MembershipService membershipService,
                            QueryService queryService,
                            SearchService searchService,
                            ImportService importService,
                            Path dataDirectory) {
        super("通讯录管理程序");
        this.contactService = contactService;
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.queryService = queryService;
        this.searchService = searchService;
        this.importService = importService;
        this.dataDirectory = dataDirectory;

        initUi();
        refreshAll();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 760);
        setLocationRelativeTo(null);

        JPanel topBar = buildTopBar();
        add(topBar, BorderLayout.NORTH);

        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                currentFilter = filterFromSelection();
                refreshContacts();
            }
        });

        contactTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        contactTable.setRowHeight(24);
        contactTable.getTableHeader().setReorderingAllowed(false);
        contactTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onEditContact();
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(groupList),
                new JScrollPane(contactTable)
        );
        splitPane.setResizeWeight(0.22);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        bottom.add(statusLabel, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshContacts();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshContacts();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshContacts();
            }
        });
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton addContactButton = new JButton("新建联系人");
        JButton editContactButton = new JButton("编辑联系人");
        JButton deleteContactButton = new JButton("删除联系人");
        JButton addGroupButton = new JButton("新建分组");
        JButton deleteGroupButton = new JButton("删除分组");
        JButton adjustGroupButton = new JButton("分组调整");
        JButton importCsvButton = new JButton("导入CSV");
        JButton exportCsvButton = new JButton("导出CSV");
        JButton importVCardButton = new JButton("导入vCard");
        JButton exportVCardButton = new JButton("导出vCard");
        JButton columnButton = new JButton("列设置");

        addContactButton.addActionListener(e -> onAddContact());
        editContactButton.addActionListener(e -> onEditContact());
        deleteContactButton.addActionListener(e -> onDeleteContacts());
        addGroupButton.addActionListener(e -> onAddGroup());
        deleteGroupButton.addActionListener(e -> onDeleteGroup());
        adjustGroupButton.addActionListener(e -> onAdjustMembership());
        importCsvButton.addActionListener(e -> onImportCsv());
        exportCsvButton.addActionListener(e -> onExportCsv());
        importVCardButton.addActionListener(e -> onImportVCard());
        exportVCardButton.addActionListener(e -> onExportVCard());
        columnButton.addActionListener(e -> onConfigureColumns());

        List<JButton> actionButtons = List.of(
                addContactButton,
                editContactButton,
                deleteContactButton,
                addGroupButton,
                deleteGroupButton,
                adjustGroupButton,
                importCsvButton,
                exportCsvButton,
                importVCardButton,
                exportVCardButton,
                columnButton
        );
        applyUniformButtonStyle(actionButtons);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.add(addContactButton);
        row1.add(editContactButton);
        row1.add(deleteContactButton);
        row1.add(addGroupButton);
        row1.add(deleteGroupButton);
        row1.add(adjustGroupButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.add(importCsvButton);
        row2.add(exportCsvButton);
        row2.add(importVCardButton);
        row2.add(exportVCardButton);
        row2.add(columnButton);

        JPanel row3 = new JPanel(new BorderLayout(8, 0));
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel searchRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton clearSearchButton = new JButton("清空");
        searchField.setPreferredSize(new Dimension(260, searchField.getPreferredSize().height));
        searchRight.add(new JLabel("搜索："));
        searchRight.add(searchField);
        searchRight.add(clearSearchButton);
        clearSearchButton.addActionListener(e -> searchField.setText(""));
        row3.add(searchRight, BorderLayout.EAST);

        bar.add(row1);
        bar.add(Box.createVerticalStrut(8));
        bar.add(row2);
        bar.add(Box.createVerticalStrut(8));
        bar.add(row3);

        return bar;
    }
    private void applyUniformButtonStyle(List<JButton> buttons) {
        int maxWidth = 0;
        int maxHeight = 0;
        for (JButton button : buttons) {
            button.setFocusPainted(false);
            button.setMargin(new Insets(5, 12, 5, 12));
            Dimension preferred = button.getPreferredSize();
            maxWidth = Math.max(maxWidth, preferred.width);
            maxHeight = Math.max(maxHeight, preferred.height);
        }
        Dimension uniform = new Dimension(maxWidth, maxHeight);
        for (JButton button : buttons) {
            button.setPreferredSize(uniform);
            button.setMinimumSize(uniform);
        }
    }
    private GroupFilter filterFromSelection() {
        GroupListItem item = groupList.getSelectedValue();
        if (item == null) {
            return GroupFilter.all();
        }
        return switch (item.kind()) {
            case ALL -> GroupFilter.all();
            case UNGROUPED -> GroupFilter.ungrouped();
            case GROUP -> GroupFilter.group(item.groupId());
        };
    }

    private void refreshAll() {
        refreshGroupList();
        refreshContacts();
    }

    private void refreshGroupList() {
        GroupListItem selected = groupList.getSelectedValue();
        String selectedGroupId = selected != null && selected.kind() == GroupListItem.Kind.GROUP ? selected.groupId() : "";
        GroupListItem.Kind selectedKind = selected == null ? GroupListItem.Kind.ALL : selected.kind();

        List<Group> groups = groupService.listGroups();
        List<Contact> allContacts = contactService.listAll();

        Map<String, Integer> countByGroup = new LinkedHashMap<>();
        int ungroupedCount = 0;
        for (Contact contact : allContacts) {
            if (contact.getGroupIds().isEmpty()) {
                ungroupedCount++;
            }
            for (String gid : contact.getGroupIds()) {
                countByGroup.merge(gid, 1, Integer::sum);
            }
        }

        groupListModel.clear();
        groupListModel.addElement(GroupListItem.all("全部联系人(" + allContacts.size() + ")"));
        groupListModel.addElement(GroupListItem.ungrouped("未分组联系人(" + ungroupedCount + ")"));
        for (Group group : groups) {
            int count = countByGroup.getOrDefault(group.getId(), 0);
            groupListModel.addElement(GroupListItem.group(group.getId(), group.getName() + "(" + count + ")"));
        }

        int indexToSelect = 0;
        for (int i = 0; i < groupListModel.size(); i++) {
            GroupListItem item = groupListModel.get(i);
            if (selectedKind == GroupListItem.Kind.GROUP
                    && item.kind() == GroupListItem.Kind.GROUP
                    && item.groupId().equals(selectedGroupId)) {
                indexToSelect = i;
                break;
            }
            if (selectedKind == GroupListItem.Kind.UNGROUPED && item.kind() == GroupListItem.Kind.UNGROUPED) {
                indexToSelect = i;
                break;
            }
        }
        if (!groupListModel.isEmpty()) {
            groupList.setSelectedIndex(indexToSelect);
        }
    }

    private void refreshContacts() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        List<Contact> contacts = searchService.search(keyword, currentFilter, SortOption.NAME_ASC);
        currentContacts = contacts;
        tableModel.setGroupNameMap(queryService.groupIdNameMap());
        tableModel.setContacts(contacts);

        Map<String, List<Contact>> sections = searchService.classifyByInitial(contacts);
        StringBuilder sectionText = new StringBuilder();
        for (Map.Entry<String, List<Contact>> entry : sections.entrySet()) {
            if (!sectionText.isEmpty()) {
                sectionText.append(' ');
            }
            sectionText.append(entry.getKey()).append('(').append(entry.getValue().size()).append(')');
        }
        statusLabel.setText("当前 " + contacts.size() + " 条" + (sectionText.isEmpty() ? "" : "，分类: " + sectionText));
    }

    private void onAddContact() {
        try {
            ContactDraft draft = ContactDialog.showDialog(this, new ContactDraft(), groupService.listGroups(), dataDirectory);
            if (draft == null) {
                return;
            }
            contactService.create(draft);
            refreshAll();
        } catch (Exception ex) {
            showError("新建联系人失败", ex);
        }
    }

    private void onEditContact() {
        try {
            Contact selected = getSingleSelectedContact();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "请先选择一个联系人。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            ContactDraft initial = ContactDraft.fromContact(selected);
            ContactDraft edited = ContactDialog.showDialog(this, initial, groupService.listGroups(), dataDirectory);
            if (edited == null) {
                return;
            }
            contactService.update(selected.getId(), edited);
            refreshAll();
        } catch (Exception ex) {
            showError("编辑联系人失败", ex);
        }
    }

    private void onDeleteContacts() {
        try {
            List<Contact> selected = getSelectedContacts();
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先选择要删除的联系人。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "确认删除选中的 " + selected.size() + " 位联系人吗？",
                    "删除确认",
                    JOptionPane.YES_NO_OPTION
            );
            if (option != JOptionPane.YES_OPTION) {
                return;
            }
            for (Contact contact : selected) {
                contactService.delete(contact.getId());
            }
            refreshAll();
        } catch (Exception ex) {
            showError("删除联系人失败", ex);
        }
    }

    private void onAddGroup() {
        try {
            String name = JOptionPane.showInputDialog(this, "请输入分组名：", "新建分组", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.isBlank()) {
                return;
            }
            groupService.createGroup(name.trim());
            refreshAll();
        } catch (Exception ex) {
            showError("新建分组失败", ex);
        }
    }

    private void onDeleteGroup() {
        GroupListItem selected = groupList.getSelectedValue();
        if (selected == null || selected.kind() != GroupListItem.Kind.GROUP) {
            JOptionPane.showMessageDialog(this, "请在左侧选择一个自定义分组再删除。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int option = JOptionPane.showConfirmDialog(
                    this,
                    "删除该分组后，分组内联系人会变为未分组，是否继续？",
                    "删除分组",
                    JOptionPane.YES_NO_OPTION
            );
            if (option != JOptionPane.YES_OPTION) {
                return;
            }
            groupService.deleteGroup(selected.groupId());
            refreshAll();
        } catch (Exception ex) {
            showError("删除分组失败", ex);
        }
    }

    private void onAdjustMembership() {
        List<Contact> selectedContacts = getSelectedContacts();
        if (selectedContacts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择联系人。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            MembershipDialog.Result result = MembershipDialog.showDialog(this, groupService.listGroups());
            if (result == null || result.groupIds().isEmpty()) {
                return;
            }

            List<String> contactIds = selectedContacts.stream().map(Contact::getId).toList();
            if (result.mode() == MembershipDialog.Mode.ADD) {
                membershipService.addContactsToGroups(contactIds, result.groupIds());
            } else {
                for (String gid : result.groupIds()) {
                    membershipService.removeContactsFromGroup(contactIds, gid);
                }
            }
            refreshAll();
        } catch (Exception ex) {
            showError("调整分组失败", ex);
        }
    }

    private void onImportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导入 CSV");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            Path file = chooser.getSelectedFile().toPath();
            List<ImportedContact> imported = csvImporter.importFrom(file);
            int count = importService.appendImportedContacts(imported);
            refreshAll();
            JOptionPane.showMessageDialog(this, "CSV 导入完成，新增 " + count + " 条联系人。", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("导入 CSV 失败", ex);
        }
    }

    private void onExportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出 CSV");
        chooser.setSelectedFile(new java.io.File("contacts.csv"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            List<Contact> exportContacts = contactsForExport();
            if (exportContacts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "当前没有可导出的联系人。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Path output = normalizeExportPath(chooser, ".csv");
            csvExporter.exportTo(output, exportContacts, queryService.groupIdNameMap());
            JOptionPane.showMessageDialog(this,
                    "CSV 导出完成，共 " + exportContacts.size() + " 条。\n文件：" + output.toAbsolutePath(),
                    "完成",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("导出 CSV 失败", ex);
        }
    }

    private void onImportVCard() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导入 vCard");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            Path file = chooser.getSelectedFile().toPath();
            List<ImportedContact> imported = vCardImporter.importFrom(file);
            int count = importService.appendImportedContacts(imported);
            refreshAll();
            JOptionPane.showMessageDialog(this, "vCard 导入完成，新增 " + count + " 条联系人。", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("导入 vCard 失败", ex);
        }
    }

    private void onExportVCard() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出 vCard");
        chooser.setSelectedFile(new java.io.File("contacts.vcf"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            List<Contact> exportContacts = contactsForExport();
            if (exportContacts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "当前没有可导出的联系人。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Path output = normalizeExportPath(chooser, ".vcf");
            vCardExporter.exportTo(output, exportContacts, queryService.groupIdNameMap());
            JOptionPane.showMessageDialog(this,
                    "vCard 导出完成，共 " + exportContacts.size() + " 条。\n文件：" + output.toAbsolutePath(),
                    "完成",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("导出 vCard 失败", ex);
        }
    }

    private Path normalizeExportPath(JFileChooser chooser, String extension) {
        Path selected = chooser.getSelectedFile().toPath();
        String fileName = selected.getFileName() == null ? "" : selected.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(extension)) {
            fileName = fileName + extension;
            selected = selected.resolveSibling(fileName);
        }
        return selected;
    }
    private void onConfigureColumns() {
        List<ContactColumn> current = tableModel.getVisibleColumns();
        Map<ContactColumn, JCheckBox> checks = new LinkedHashMap<>();

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        for (ContactColumn column : ContactColumn.values()) {
            JCheckBox checkBox = new JCheckBox(column.title(), current.contains(column));
            checks.put(column, checkBox);
            panel.add(checkBox);
        }

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "选择显示列",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        List<ContactColumn> selected = new ArrayList<>();
        for (Map.Entry<ContactColumn, JCheckBox> entry : checks.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }

        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "至少保留一列。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        tableModel.setVisibleColumns(selected);
        refreshContacts();
    }

    private List<Contact> contactsForExport() {
        List<Contact> selected = getSelectedContacts();
        if (!selected.isEmpty()) {
            return selected;
        }
        return new ArrayList<>(currentContacts);
    }

    private List<Contact> getSelectedContacts() {
        int[] rows = contactTable.getSelectedRows();
        List<Contact> contacts = new ArrayList<>();
        for (int row : rows) {
            Contact contact = tableModel.getContactAt(row);
            if (contact != null) {
                contacts.add(contact);
            }
        }
        return contacts;
    }

    private Contact getSingleSelectedContact() {
        int row = contactTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return tableModel.getContactAt(row);
    }

    private void showError(String title, Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
