import java.io.FileOutputStream;
import java.util.*;

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

    public String returnType(String type) {
        String retType;

        switch (type) {
            case "int":
                retType = "i32";
                break;
            case "boolean":
                retType = "i8";
                break;
            default:
                retType = "i8*";
        }

        return retType;
    }

    // Writes the V-Table declarations to an .ll file
    public void writeTables(FileOutputStream out) throws Exception{

        String s;
        String className;
        String methodName;
        String returnType;
        String argType;
        int totalMethods;
        int totalArgs;
        int argIndex;
        Stack<ClassInfo> stackOfClasses; // Keeps track of the correct sequence for the v-tables
        ClassInfo currentClass;
        MethodInfo currentMethod = null;
        FieldInfo currentArgument;

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
                    s = "@." + className + "_vtable = global[0 x i8*] []\n\n";
                    byte b[] = s.getBytes(); // Converting the string to a byte array
                    out.write(b);
                    continue;
                }

                else {
                    totalMethods = currentClass.getInheritedMethods().size() + currentClass.getMethods().size();
                    s = "@." + className + "_vtable = global[" + totalMethods + " x i8*] [";

                    if(totalMethods != 0) {
                        s += "\n";

                        Set<String> keys = tablesMap.get(className).getPointersTable().keySet();
                        int index = keys.size();
                        int counter = 0;

                        // Iterating through the class's methods
                        for(String k:keys) {

                            // Retrieving the method that we'll be working on
                            methodName = k;
                            if(currentClass.getInheritedMethods().contains(methodName))
                                currentMethod = currentClass.getInheritedMethodMap().get(methodName);
                            else if(currentClass.getMethods().contains(methodName))
                                currentMethod = currentClass.getMethodMap().get(methodName);

                            // Setting the return type
                            returnType = currentMethod.getReturnType();
                            returnType = returnType(returnType);

                            s += "    i8* bitcast (" + returnType + " (i8*";

                            // Iterating through the method's arguments
                            totalArgs = currentMethod.getArguments().size();
                            argIndex = 0;
                            for(int i = 0; i < totalArgs; i++) {

                                s += ",";

                                currentArgument = currentMethod.getArguments().get(i);
                                argType = currentArgument.getType();
                                argType = returnType(argType);

                                s += argType;

                                if(argIndex < totalArgs - 1)
                                    s += ",";

                                argIndex++;
                            }

                            s += ")* @" + className + "." + methodName + " to i8*)";

                            if(counter < index - 1)
                                s += ",\n";

                            counter++;
                        }
                    }
                    else {
                        s += "]\n\n";
                        byte b[] = s.getBytes(); // Converting the string to a byte array
                        out.write(b);
                        continue;
                    }

                    s += "\n";
                }

                s += "]\n\n";

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
