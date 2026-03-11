package addressbook.ui;

import addressbook.model.ContactDraft;
import addressbook.model.Group;
import addressbook.util.DateUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ContactDialog extends JDialog {
    private static final int PREVIEW_WIDTH = 240;
    private static final int PREVIEW_HEIGHT = 160;

    private ContactDraft result;
    private final Path dataDirectory;
    private String pinyinOverrideValue;
    private final JTextField photoField;
    private final JLabel pinyinOverrideValueLabel = new JLabel();
    private final JLabel photoPreviewLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton viewOriginalButton = new JButton("查看原图");

    private ContactDialog(Window owner, ContactDraft initial, List<Group> groups, Path dataDirectory) {
        super(owner, "联系人", ModalityType.APPLICATION_MODAL);
        ContactDraft draft = initial == null ? new ContactDraft() : initial;
        this.dataDirectory = dataDirectory == null ? Path.of("data") : dataDirectory;
        this.pinyinOverrideValue = draft.getPinyinOverride() == null ? "" : draft.getPinyinOverride();

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameField = new JTextField(draft.getName(), 24);
        JTextField phoneField = new JTextField(draft.getPhone(), 24);
        JTextField mobileField = new JTextField(draft.getMobile(), 24);
        JTextField imTypeField = new JTextField(draft.getImType(), 24);
        JTextField imNumberField = new JTextField(draft.getImNumber(), 24);
        JTextField emailField = new JTextField(draft.getEmail(), 24);
        JTextField websiteField = new JTextField(draft.getWebsite(), 24);
        JTextField birthdayField = new JTextField(DateUtil.format(draft.getBirthday()), 24);
        this.photoField = new JTextField(draft.getPhotoPath(), 24);
        JTextField companyField = new JTextField(draft.getCompany(), 24);
        JTextField addressField = new JTextField(draft.getHomeAddress(), 24);
        JTextField postalField = new JTextField(draft.getPostalCode(), 24);
        JTextArea noteArea = new JTextArea(draft.getNote(), 4, 24);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JButton browsePhotoButton = new JButton("选择相片");
        browsePhotoButton.addActionListener(e -> choosePhotoFile());
        JButton editPinyinOverrideButton = new JButton("拼音Override");
        editPinyinOverrideButton.addActionListener(e -> editPinyinOverride());
        refreshPinyinOverrideLabel();

        photoField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshPhotoPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshPhotoPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshPhotoPreview();
            }
        });

        viewOriginalButton.setEnabled(false);
        viewOriginalButton.addActionListener(e -> openOriginalPhoto());

        DefaultListModel<Group> groupModel = new DefaultListModel<>();
        for (Group group : groups) {
            groupModel.addElement(group);
        }
        JList<Group> groupList = new JList<>(groupModel);
        groupList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectInitialGroups(groupList, groups, draft.getGroupIds());

        int row = 0;
        addRow(form, gbc, row++, "姓名*", nameField);
        addRow(form, gbc, row++, "电话", phoneField);
        addRow(form, gbc, row++, "手机", mobileField);
        addRow(form, gbc, row++, "即时通信工具", imTypeField);
        addRow(form, gbc, row++, "即时通信号码", imNumberField);
        addRow(form, gbc, row++, "电子邮箱", emailField);
        addRow(form, gbc, row++, "个人主页", websiteField);
        addRow(form, gbc, row++, "生日(yyyy-MM-dd)", birthdayField);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel("拼音Override"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        form.add(pinyinOverrideValueLabel, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        form.add(editPinyinOverrideButton, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel("相片"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        form.add(photoField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        form.add(browsePhotoButton, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("相片预览"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel previewPanel = buildPhotoPreviewPanel();
        previewPanel.setPreferredSize(new Dimension(280, 210));
        form.add(previewPanel, gbc);
        row++;

        addRow(form, gbc, row++, "工作单位", companyField);
        addRow(form, gbc, row++, "家庭地址", addressField);
        addRow(form, gbc, row++, "邮编", postalField);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("所属组"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane groupPane = new JScrollPane(groupList);
        groupPane.setPreferredSize(new Dimension(260, 90));
        form.add(groupPane, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("备注"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane notePane = new JScrollPane(noteArea);
        notePane.setPreferredSize(new Dimension(260, 90));
        form.add(notePane, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        buttons.add(okButton);
        buttons.add(cancelButton);

        okButton.addActionListener(e -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isBlank()) {
                JOptionPane.showMessageDialog(this, "姓名不能为空。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String birthdayText = birthdayField.getText() == null ? "" : birthdayField.getText().trim();
            var birthday = DateUtil.parseFlexible(birthdayText);
            if (!birthdayText.isBlank() && birthday == null) {
                JOptionPane.showMessageDialog(this, "生日格式无效，请使用 yyyy-MM-dd。", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ContactDraft created = new ContactDraft();
            created.setName(name);
            created.setPhone(text(phoneField));
            created.setMobile(text(mobileField));
            created.setImType(text(imTypeField));
            created.setImNumber(text(imNumberField));
            created.setEmail(text(emailField));
            created.setWebsite(text(websiteField));
            created.setBirthday(birthday);
            created.setPhotoPath(text(photoField));
            created.setCompany(text(companyField));
            created.setHomeAddress(text(addressField));
            created.setPostalCode(text(postalField));
            created.setPinyinOverride(pinyinOverrideValue == null ? "" : pinyinOverrideValue);
            created.setNote(noteArea.getText() == null ? "" : noteArea.getText().trim());

            Set<String> selectedGroups = new HashSet<>();
            for (Group group : groupList.getSelectedValuesList()) {
                selectedGroups.add(group.getId());
            }
            created.setGroupIds(selectedGroups);

            result = created;
            dispose();
        });

        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });

        setLayout(new BorderLayout());
        add(new JScrollPane(form), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        refreshPhotoPreview();

        setSize(640, 860);
        setLocationRelativeTo(owner);
    }

    public static ContactDraft showDialog(Component owner, ContactDraft initial, List<Group> groups, Path dataDirectory) {
        Window window = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        ContactDialog dialog = new ContactDialog(window, initial, groups, dataDirectory);
        dialog.setVisible(true);
        return dialog.result;
    }

    private JPanel buildPhotoPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createLineBorder(new Color(208, 208, 208)));

        photoPreviewLabel.setOpaque(true);
        photoPreviewLabel.setBackground(Color.WHITE);
        photoPreviewLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        panel.add(photoPreviewLabel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(viewOriginalButton);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void choosePhotoFile() {
        try {
            Path selected = showNativePhotoDialog();
            if (selected == null) {
                return;
            }
            photoField.setText(selected.toAbsolutePath().normalize().toString());
            refreshPhotoPreview();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "打开相片选择器失败：" + ex.getMessage(), "提示", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path showNativePhotoDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        FileDialog dialog;
        if (owner instanceof Dialog dialogOwner) {
            dialog = new FileDialog(dialogOwner, "选择相片", FileDialog.LOAD);
        } else if (owner instanceof Frame frameOwner) {
            dialog = new FileDialog(frameOwner, "选择相片", FileDialog.LOAD);
        } else {
            dialog = new FileDialog((Frame) null, "选择相片", FileDialog.LOAD);
        }

        Path initial = resolvePhotoPath(photoField.getText());
        Path initialDir = null;
        if (initial != null) {
            initialDir = Files.isDirectory(initial) ? initial : initial.getParent();
        }
        if (initialDir == null || !Files.exists(initialDir)) {
            initialDir = Files.isDirectory(dataDirectory) ? dataDirectory : dataDirectory.getParent();
        }
        if (initialDir != null && Files.exists(initialDir)) {
            dialog.setDirectory(initialDir.toAbsolutePath().toString());
        }

        dialog.setFilenameFilter((dir, name) -> {
            String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".png")
                    || lower.endsWith(".gif")
                    || lower.endsWith(".bmp")
                    || lower.endsWith(".webp");
        });

        dialog.setVisible(true);
        String file = dialog.getFile();
        if (file == null || file.isBlank()) {
            return null;
        }

        String dir = dialog.getDirectory();
        if (dir == null || dir.isBlank()) {
            return Path.of(file).toAbsolutePath().normalize();
        }
        return Path.of(dir, file).toAbsolutePath().normalize();
    }

    private void refreshPhotoPreview() {
        Path photoPath = resolvePhotoPath(photoField.getText());
        if (photoPath == null) {
            setPhotoPreviewPlaceholder("未选择相片");
            viewOriginalButton.setEnabled(false);
            return;
        }
        if (!Files.exists(photoPath) || Files.isDirectory(photoPath)) {
            setPhotoPreviewPlaceholder("未找到相片");
            viewOriginalButton.setEnabled(false);
            return;
        }

        try {
            BufferedImage image = ImageIO.read(photoPath.toFile());
            if (image == null) {
                setPhotoPreviewPlaceholder("无法加载相片");
                viewOriginalButton.setEnabled(false);
                return;
            }
            Image scaled = scaleImage(image);
            photoPreviewLabel.setIcon(new ImageIcon(scaled));
            photoPreviewLabel.setText("");
            viewOriginalButton.setEnabled(true);
        } catch (IOException ex) {
            setPhotoPreviewPlaceholder("无法加载相片");
            viewOriginalButton.setEnabled(false);
        }
    }

    private Image scaleImage(BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        if (originalWidth <= 0 || originalHeight <= 0) {
            return image;
        }

        double ratio = Math.min((double) PREVIEW_WIDTH / originalWidth, (double) PREVIEW_HEIGHT / originalHeight);
        int width = Math.max(1, (int) Math.round(originalWidth * ratio));
        int height = Math.max(1, (int) Math.round(originalHeight * ratio));
        return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }

    private void setPhotoPreviewPlaceholder(String text) {
        photoPreviewLabel.setIcon(null);
        photoPreviewLabel.setText(text);
    }

    private Path resolvePhotoPath(String rawPath) {
        String value = rawPath == null ? "" : rawPath.trim();
        if (value.isBlank()) {
            return null;
        }

        try {
            Path path = Path.of(value.replace('\\', '/'));
            if (path.isAbsolute()) {
                return path.normalize();
            }
            return dataDirectory.resolve(path).normalize();
        } catch (Exception ex) {
            return null;
        }
    }

    private void openOriginalPhoto() {
        Path photoPath = resolvePhotoPath(photoField.getText());
        if (photoPath == null || !Files.exists(photoPath) || Files.isDirectory(photoPath)) {
            JOptionPane.showMessageDialog(this, "未找到相片文件。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JOptionPane.showMessageDialog(this, "当前系统不支持打开原图。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(photoPath.toFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "打开原图失败：" + ex.getMessage(), "提示", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void editPinyinOverride() {
        String input = (String) JOptionPane.showInputDialog(
                this,
                "请输入拼音override（可留空）：",
                "拼音Override",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                pinyinOverrideValue
        );
        if (input == null) {
            return;
        }
        pinyinOverrideValue = input.trim();
        refreshPinyinOverrideLabel();
    }

    private void refreshPinyinOverrideLabel() {
        if (pinyinOverrideValue == null || pinyinOverrideValue.isBlank()) {
            pinyinOverrideValueLabel.setText("未设置");
            return;
        }
        pinyinOverrideValueLabel.setText(pinyinOverrideValue);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void selectInitialGroups(JList<Group> groupList, List<Group> groups, Set<String> selectedGroupIds) {
        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            if (selectedGroupIds.contains(groups.get(i).getId())) {
                selectedIndices.add(i);
            }
        }
        int[] indices = selectedIndices.stream().mapToInt(Integer::intValue).toArray();
        groupList.setSelectedIndices(indices);
    }

    private String text(JTextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }
}