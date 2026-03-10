package addressbook.ui;

import addressbook.model.Group;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MembershipDialog extends JDialog {
    public enum Mode {
        ADD,
        REMOVE
    }

    public record Result(Mode mode, List<String> groupIds) {
    }

    private Result result;

    private MembershipDialog(Window owner, List<Group> groups) {
        super(owner, "调整分组", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        DefaultListModel<Group> model = new DefaultListModel<>();
        for (Group group : groups) {
            model.addElement(group);
        }

        JList<Group> groupList = new JList<>(model);
        groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        center.add(new JLabel("选择目标分组（可多选）"), BorderLayout.NORTH);
        center.add(new JScrollPane(groupList), BorderLayout.CENTER);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JRadioButton addButton = new JRadioButton("加入分组", true);
        JRadioButton removeButton = new JRadioButton("从分组移除");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(addButton);
        buttonGroup.add(removeButton);
        modePanel.add(addButton);
        modePanel.add(removeButton);
        center.add(modePanel, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        bottom.add(okButton);
        bottom.add(cancelButton);
        add(bottom, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            List<Group> selected = groupList.getSelectedValuesList();
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请至少选择一个分组。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<String> selectedIds = new ArrayList<>();
            for (Group group : selected) {
                selectedIds.add(group.getId());
            }
            Mode mode = addButton.isSelected() ? Mode.ADD : Mode.REMOVE;
            result = new Result(mode, selectedIds);
            dispose();
        });

        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });

        setSize(360, 360);
        setLocationRelativeTo(owner);
    }

    public static Result showDialog(Component owner, List<Group> groups) {
        Window window = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        MembershipDialog dialog = new MembershipDialog(window, groups);
        dialog.setVisible(true);
        return dialog.result;
    }
}
