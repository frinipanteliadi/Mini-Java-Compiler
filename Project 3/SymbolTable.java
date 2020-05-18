import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {

    private List<String> validTypes; // A list of all the valid data types
    private List<String> classes; // A list with the names of all classes
    private HashMap<String, ClassInfo> classMap; // Where we'll keep information about every class

    public SymbolTable() {
        validTypes = new ArrayList<String>();
        classes = new ArrayList<String>();
        classMap = new HashMap<String, ClassInfo>();

        addDataType("int");
        addDataType("boolean");
        addDataType("int[]");
        addDataType("boolean[]");
    }

    public void addDataType(String dataType) { validTypes.add(dataType); }

    public void checkDataTypes() {
        ClassInfo currentClass;

        for(int i = 0; i < classes.size(); i++) {
            currentClass = classMap.get(classes.get(i));

            // Checking the data types of the class's fields
            List<FieldInfo> classFields = currentClass.getFields();
            for(int j = 0; j < classFields.size(); j++) {
                if(!validTypes.contains(classFields.get(j).getType())) {
                    System.out.println("Error: Invalid Type " + classFields.get(j).getType());
                    System.exit(1);
                }
            }

            // Checking the return types of the class's methods
            List<String> classMethods = currentClass.getMethods();
            for(int j = 0; j < classMethods.size(); j++) {
                if("main".equals(classMethods.get(j)))
                    continue;
                else {
                    if(!validTypes.contains(currentClass.getClassMethod(classMethods.get(j)).getReturnType())){
                        System.out.println("Error: Method " + classMethods.get(j) + "() has an invalid return type " + currentClass.getClassMethod(classMethods.get(j)).getReturnType());
                        System.exit(1);
                    }
                }
            }
        }
    }

    // Create a new mapping for the class named "className"
    public void putClass(String className, int offset, ClassInfo parent) {

        classMap.put(className, new ClassInfo(className, offset, parent));
    }

    public ClassInfo getClass(String className) {
        if(classes.contains(className))
            return classMap.get(className);
        else
            return null;
    }

    public void printClass(String className) {
        classMap.get(className).printClassInfo();
    }

    public void printClassFields(String className) {
        (classMap.get(className)).printClassFields();
    }

    public void setParentClass(String className, String superName) {
        (classMap.get(className)).setParent(classMap.get(superName));
    }

    public void putField(String className, String fieldDecl, int offset) {
        (classMap.get(className)).addField(fieldDecl, offset);
    }

    public void putMethod(String className, String methodName) {
        (classMap.get(className)).addMethod(methodName);
    }

    /* Methods for the Lists */
    public List<String> getClasses() { return classes; }

    public List<String> getClassMethods(String className) {
        return (classMap.get(className)).getMethods();
    }

    public List<FieldInfo> getClassFields(String className) {
        return (classMap.get(className)).getFields();
    }

    public boolean classExists(String className) {
        boolean returnValue = false;

        if (classes.contains(className))
            returnValue = true;

        return returnValue;
    }

    // Returns the list that holds all of the valid data types
    public List<String> getValidTypes() { return validTypes; }

    public void printSymbolTable() {

        for(int i = 0; i < getClasses().size(); i++) {
            ClassInfo currentClass = getClass(getClasses().get(i));
            System.out.println("Information about class " + currentClass.getName() + ":");

            // General Information
            currentClass.printClassInfo();

            // Fields
            currentClass.printClassFields();

            // Methods
            currentClass.printClassMethods();

            System.out.println();
        }
    }

    // Source: StackOverflow
    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }

    public void setOffsets() {
        ClassInfo currentClass = null;
        FieldInfo currentField = null;
        MethodInfo currentMethod = null;

        // Calculating the offsets for every class
        for(int i = 0; i < classes.size(); i++) {
           currentClass = getClass(classes.get(i));

           // Calculating the offsets for the fields
           for(int j = 0; j < currentClass.getFields().size(); j++) {
                currentField = currentClass.getFields().get(j);

                if(currentField.getType().equals("int")) {

                    if(currentClass.hasParent()) {
                        currentField.setOffset(currentClass.getParent().getFieldOffset());
                        currentClass.getParent().incFieldOffset(4);
                    }
                    else {
                        currentField.setOffset(currentClass.getFieldOffset());
                        currentClass.incFieldOffset(4);
                    }
                }
                else if(currentField.getType().equals("boolean")) {

                    if(currentClass.hasParent()) {
                        currentField.setOffset(currentClass.getParent().getFieldOffset());
                        currentClass.getParent().incFieldOffset(1);
                    }
                    else {
                        currentField.setOffset(currentClass.getFieldOffset());
                        currentClass.incFieldOffset(1);
                    }
                }
                else if(currentField.getType().equals("int[]") || currentField.getType().equals("boolean[]")) {

                    if(currentClass.hasParent()) {
                        currentField.setOffset(currentClass.getParent().getFieldOffset());
                        currentClass.getParent().incFieldOffset(8);
                    }
                    else {
                        currentField.setOffset(currentClass.getFieldOffset());
                        currentClass.incFieldOffset(8);
                    }
                }
                else {
                    if(currentClass.hasParent()) {
                        currentField.setOffset(currentClass.getParent().getFieldOffset());
                        currentClass.getParent().incFieldOffset(8);
                    }
                    else {
                        currentField.setOffset(currentClass.getFieldOffset());
                        currentClass.incFieldOffset(8);
                    }
                }
           }

            // Calculating the offsets for the methods and the pointers
            for(int j = 0; j < currentClass.getMethods().size(); j++) {
                currentMethod = currentClass.getClassMethod(currentClass.getMethods().get(j));

                String methodName = currentMethod.getName();

                if(currentMethod.getOwner().hasParent()) {
                    if(!currentMethod.getOwner().getParent().getMethods().contains(methodName)) {
                       currentMethod.setOffset(currentMethod.getOwner().getParent().getPointerOffset());
                       currentMethod.getOwner().getParent().incPointerOffset(8);
                    }
                    else
                        currentMethod.setOffset(-1);
                }
                else {
                    currentMethod.setOffset(currentMethod.getOwner().getPointerOffset());
                    currentMethod.getOwner().incPointerOffset(8);
                }
            }
        }
    }
}

