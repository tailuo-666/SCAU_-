package addressbook.ui;

import addressbook.model.Contact;
import addressbook.util.DateUtil;
import addressbook.util.NameSortUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public enum ContactColumn {
    CLASSIFY("分类"),
    NAME("姓名"),
    PHONE("电话"),
    MOBILE("手机"),
    IM("即时通信"),
    EMAIL("电子邮箱"),
    WEBSITE("个人主页"),
    BIRTHDAY("生日"),
    PHOTO("相片"),
    COMPANY("工作单位"),
    ADDRESS("家庭地址"),
    POSTAL_CODE("邮编"),
    GROUPS("所属组"),
    NOTE("备注");

    private final String title;

    ContactColumn(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public Object value(Contact contact, Map<String, String> groupNameMap) {
        return switch (this) {
            case CLASSIFY -> NameSortUtil.classifyLabel(contact);
            case NAME -> safe(contact.getName());
            case PHONE -> safe(contact.getPhone());
            case MOBILE -> safe(contact.getMobile());
            case IM -> formatIm(contact);
            case EMAIL -> safe(contact.getEmail());
            case WEBSITE -> safe(contact.getWebsite());
            case BIRTHDAY -> DateUtil.format(contact.getBirthday());
            case PHOTO -> safe(contact.getPhotoPath());
            case COMPANY -> safe(contact.getCompany());
            case ADDRESS -> safe(contact.getHomeAddress());
            case POSTAL_CODE -> safe(contact.getPostalCode());
            case GROUPS -> formatGroups(contact, groupNameMap);
            case NOTE -> safe(contact.getNote());
        };
    }

    public static List<ContactColumn> defaultColumns() {
        return new ArrayList<>(List.of(
                CLASSIFY,
                NAME,
                PHONE,
                MOBILE,
                EMAIL,
                GROUPS
        ));
    }

    private static String formatIm(Contact contact) {
        String type = safe(contact.getImType());
        String number = safe(contact.getImNumber());
        if (type.isBlank() && number.isBlank()) {
            return "";
        }
        if (type.isBlank()) {
            return number;
        }
        if (number.isBlank()) {
            return type;
        }
        return type + ":" + number;
    }

    private static String formatGroups(Contact contact, Map<String, String> groupNameMap) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String gid : contact.getGroupIds()) {
            String name = groupNameMap.getOrDefault(gid, "");
            if (!name.isBlank()) {
                joiner.add(name);
            }
        }
        return joiner.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
