public class FieldInfo extends Info {

    private String type;
    private String regName; // The register in which we've stored the value"
    private boolean initialized;

    public FieldInfo(String type, String name, int offset, boolean initialized) {
        super(name, offset);
        setType(type);
        this.initialized = initialized;
        regName = null;
    }

    public void printField() {
        System.out.println("\t* Name: " + getName());
        System.out.println("\t* Offset: " + getOffset());
        System.out.println("\t* Type: " + this.type);
        System.out.println("\t* RegName: " + this.regName);
        System.out.println("\t* Initialized: " + this.initialized);
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRegName(String regName) { this.regName = regName; }

    public String getRegName() { return regName; }

    public void setInitialized(boolean initialized) {this.initialized = initialized; }

    public boolean getInitialized() { return initialized; }

    public String getType() { return type; }
}
