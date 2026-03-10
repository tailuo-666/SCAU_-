package addressbook.ui;

public class GroupListItem {
    public enum Kind {
        ALL,
        UNGROUPED,
        GROUP
    }

    private final Kind kind;
    private final String groupId;
    private final String label;

    private GroupListItem(Kind kind, String groupId, String label) {
        this.kind = kind;
        this.groupId = groupId;
        this.label = label;
    }

    public static GroupListItem all(String label) {
        return new GroupListItem(Kind.ALL, "", label);
    }

    public static GroupListItem ungrouped(String label) {
        return new GroupListItem(Kind.UNGROUPED, "", label);
    }

    public static GroupListItem group(String groupId, String label) {
        return new GroupListItem(Kind.GROUP, groupId, label);
    }

    public Kind kind() {
        return kind;
    }

    public String groupId() {
        return groupId;
    }

    @Override
    public String toString() {
        return label;
    }
}
