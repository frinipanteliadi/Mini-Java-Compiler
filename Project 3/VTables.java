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
                retType = "i1";
                break;
            case "int[]":
                retType =  "i32*";
                break;
            default:
                retType = "i8*";
        }

        return retType;
    }

    // Writes the V-Table declarations to an .ll file
    public void writeVTables(FileOutputStream out) throws Exception{

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
                    s = "@." + className + "_vtable = global [0 x i8*] []\n\n";
                    byte b[] = s.getBytes(); // Converting the string to a byte array
                    out.write(b);
                    continue;
                }

                else {
                    totalMethods = currentClass.getInheritedMethods().size() + currentClass.getMethods().size();
                    s = "@." + className + "_vtable = global [" + totalMethods + " x i8*] [";

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

                            if(currentClass.getMethods().contains(methodName))
                                s += ")* @" + className + "." + methodName + " to i8*)";
                            else if(currentClass.getInheritedMethods().contains(methodName))
                                s += ")* @" + currentMethod.getOwner().getName() + "." + methodName + " to i8*)";

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

class Functions {

    public static void declareFunctions(FileOutputStream out) throws Exception{

        String s;
        s = "declare i8* @calloc(i32, i32)\n";
        s += "declare i32 @printf(i8*, ...)\n";
        s += "declare void @exit(i32)\n\n";

        s += "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n";
        s += "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n";
        s += "@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"\n\n";

        s += "define void @print_int(i32 %i) {\n";
        s += "\t%_str = bitcast [4 x i8]* @_cint to i8*\n";
        s += "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n";
        s += "\tret void\n";
        s += "}\n\n";

        s += "define void @throw_oob() {\n";
        s += "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n";
        s += "\tcall i32 (i8*, ...) @printf(i8* %_str)\n";
        s += "\tcall void @exit(i32 1)\n";
        s += "\tret void\n";
        s += "}\n\n";

        s += "define void @throw_nsz() {\n";
        s += "\t%_str = bitcast [15 x i8]* @_cNSZ to i8*\n";
        s += "\tcall i32 (i8*, ...) @printf(i8* %_str)\n";
        s += "\tcall void @exit(i32 1)\n";
        s += "\tret void\n";
        s += "}\n\n";
        
        try {
            byte b[] = s.getBytes(); // Converting the string to a byte array
            out.write(b);
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }
}
