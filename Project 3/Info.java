public class Info {

    private String name;
    private int offset;

    public Info(String name, int offset) {
        this.name = name;
        this.offset = offset;
    }

    public String getName() { return name; }

    public int getOffset() { return offset; }

    public void setOffset(int offset) { this.offset = offset; }
}


