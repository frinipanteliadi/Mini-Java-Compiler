public class FieldInfo extends Info {

    private String type;
    private boolean initialized;

    public FieldInfo(String type, String name, int offset, boolean initialized) {
        super(name, offset);
        setType(type);
        this.initialized = initialized;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setInitialized(boolean initialized) {this.initialized = initialized; }

    public boolean getInitialized() { return initialized; }

    public String getType() { return type; }
}
