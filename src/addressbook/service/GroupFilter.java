package addressbook.service;

public class GroupFilter {
    public enum Type {
        ALL,
        UNGROUPED,
        GROUP
    }

    private final Type type;
    private final String groupId;

    private GroupFilter(Type type, String groupId) {
        this.type = type;
        this.groupId = groupId;
    }

    public static GroupFilter all() {
        return new GroupFilter(Type.ALL, "");
    }

    public static GroupFilter ungrouped() {
        return new GroupFilter(Type.UNGROUPED, "");
    }

    public static GroupFilter group(String groupId) {
        return new GroupFilter(Type.GROUP, groupId == null ? "" : groupId);
    }

    public Type getType() {
        return type;
    }

    public String getGroupId() {
        return groupId;
    }
}
