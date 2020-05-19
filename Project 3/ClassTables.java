import java.util.LinkedHashMap;

public class ClassTables {

    String className;
    LinkedHashMap<String, Integer> fieldsTable;
    LinkedHashMap<String, Integer> methodsTable;

    public ClassTables(String className) {
        this.className = className;
        fieldsTable = new LinkedHashMap<String,Integer>();
        methodsTable = new LinkedHashMap<String,Integer>();
    }
    
}
