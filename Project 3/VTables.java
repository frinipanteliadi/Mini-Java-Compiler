import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Stack;
import java.util.LinkedHashMap;

public class VTables {

    private HashMap<String, ClassTables> tablesMap;
    private SymbolTable symbolTable;

    public VTables(SymbolTable symbolTable) {
        tablesMap = new HashMap<String, ClassTables>();
        this.symbolTable = symbolTable;
        putVTables();
    }

    public void putVTables() {
        for(int i = 0; i < symbolTable.getClasses().size(); i++)
            tablesMap.put(symbolTable.getClasses().get(i), new ClassTables(symbolTable.getClasses().get(i), symbolTable));
    }

    public void createClassTables() {
        for(int i = 0; i < symbolTable.getClasses().size(); i++)
            tablesMap.get(symbolTable.getClasses().get(i)).createPointersTable();
    }

    public void printClassTables() {
        for(int i = 0; i < symbolTable.getClasses().size(); i++)
            tablesMap.get(symbolTable.getClasses().get(i)).printPointersTable();
    }

    public void pushParent(ClassInfo currentClass, Stack<ClassInfo> stackOfClasses) {

        ClassInfo parent;
        parent = currentClass.getParent();

        while(parent != null) {
            pushParent(parent, stackOfClasses);
            parent = parent.getParent();
        }

        if(!stackOfClasses.contains(currentClass))
            stackOfClasses.push(currentClass);

    }

    public void writeTables(FileOutputStream out) throws Exception{

        Stack<ClassInfo> stackOfClasses; // Keeps track of the correct sequence for the v-tables
        ClassInfo currentClass;
        String className;
        String s;
        List<String> inheritedMethods;
        int totalMethods;

        stackOfClasses = new Stack<ClassInfo>();

        // Adding the MainClass to the bottom of the stack
        stackOfClasses.push(symbolTable.getClass(symbolTable.getClasses().get(0)));

        // Starting from the end of the list that has the names of the classes
        for(int i = symbolTable.getClasses().size() - 1; i > 0; i--) {

            currentClass = symbolTable.getClass(symbolTable.getClasses().get(i));
            pushParent(currentClass, stackOfClasses);
        }

        while(!stackOfClasses.isEmpty()) {
            currentClass = stackOfClasses.pop();
            className = currentClass.getName();

            try {

                if(currentClass.getMethods().contains("main")) {
                    totalMethods = 0;
                    s = "@." + className + "_vtable = global[" + totalMethods + " x i8*] []\n\n";
                }
                else {
                    totalMethods = currentClass.getInheritedMethods().size() + currentClass.getMethods().size();
                    s = "@." + className + "_vtable = global[" + totalMethods + " x i8*] [\n";
                    s += "    i8* bitcast(\n";


                }

                byte b[] = s.getBytes(); // Converting the string to a byte array
                out.write(b);
            }
            catch (Exception e) {
                System.out.println(e);
                System.exit(1);
            }
        }
    }
}
