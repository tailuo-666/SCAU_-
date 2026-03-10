package addressbook.model;

public class Group {
    private String id;
    private String name;
    private int order;

    public Group() {
    }

    public Group(String id, String name, int order) {
        this.id = id;
        this.name = name;
        this.order = order;
    }

    public Group(Group other) {
        this.id = other.id;
        this.name = other.name;
        this.order = other.order;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return name;
    }
}
