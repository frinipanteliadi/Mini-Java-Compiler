import java.util.LinkedHashMap;

public class ClassTables {

    String className;
    LinkedHashMap<String, Integer> fieldsTable;
    LinkedHashMap<String, Integer> pointersTable;

    public ClassTables(String className) {
        this.className = className;
        fieldsTable = new LinkedHashMap<String, Integer>();
        pointersTable = new LinkedHashMap<String, Integer>();
    }
}
