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
    public void putClass(String className, int offset, ClassInfo parent) { classMap.put(className, new ClassInfo(className, offset, parent)); }

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

        ClassInfo currentClass;
        FieldInfo currentField;
        MethodInfo currentMethod;
        ClassInfo offsetClass = null;
        ClassInfo parent;
        int increment;
        int offset = 0;
        int flag;

        // Calculating the offsets for every class
        for(int i = 0; i < classes.size(); i++) {

            currentClass = getClass(classes.get(i));

           // Calculating the offsets for the fields
           for(int j = 0; j < currentClass.getFields().size(); j++) {

               currentField = currentClass.getFields().get(j);
               offsetClass = currentClass;
               parent = currentClass.getParent();

               while(parent != null) {
                   offsetClass = parent;
                   parent = parent.getParent();
               }

                if(currentField.getType().equals("int")) {
                    increment = 4;
                }
                else if(currentField.getType().equals("boolean")) {
                    increment = 1;
                }
                else if(currentField.getType().equals("int[]") || currentField.getType().equals("boolean[]")) {
                    increment = 8;
                }
                else {
                    increment = 8;
                }

               currentField.setOffset(offsetClass.getFieldOffset());
               offsetClass.incFieldOffset(increment);
           }

            // Calculating the offsets for the methods and the pointers
            for(int j = 0; j < currentClass.getMethods().size(); j++) {

                currentMethod = currentClass.getClassMethod(currentClass.getMethods().get(j));
                String methodName = currentMethod.getName();

                flag = 0;
                parent = currentMethod.getOwner().getParent();

                while(parent != null) {

                    if(parent.getMethods().contains(methodName)) {
                        // The method was found in one of its superclasses
                        flag = 1;
                        offset = parent.getClassMethod(methodName).getOffset();
                        break;
                    }
                    else {
                        flag = 2;
                        offsetClass = parent;
                        parent = parent.getParent();
                    }
                }

                switch (flag) {
                    case 0:
                        // The class doesn't extend any other classes
                        currentMethod.setOffset(currentMethod.getOwner().getPointerOffset());
                        currentMethod.getOwner().incPointerOffset(8);
                        break;
                    case 1:
                        // This is an overridden method
                        currentMethod.setOffset(/*-1*/offset);
                        currentMethod.getOwner().incPointerOffset(8);
                        break;
                    case 2:
                        // None of the super classes had the method
                        currentMethod.setOffset(offsetClass.getPointerOffset());
                        offsetClass.incPointerOffset(8);
                        break;
                }
            }
        }
    }

    public void setInheritedMethods() {

        for(int i = 0; i < classes.size(); i++)
            classMap.get(classes.get(i)).setInheritedMethods();
    }

    public void printInheritedMethods() {
        for(int i = 0; i < getClasses().size(); i++) {

            String className = getClasses().get(i);
            ClassInfo currentClass = getClass(className);
            String methodName;
            MethodInfo currentMethod;
            HashMap<String, MethodInfo> map;

            System.out.println("Inherited methods of class " + className);

            if(currentClass.getInheritedMethods().isEmpty())
                System.out.println("   * NONE");
            else {
                map = currentClass.getInheritedMethodMap();
                for(int j = 0; j < currentClass.getInheritedMethods().size(); j++) {
                    methodName = currentClass.getInheritedMethods().get(j);
                    currentMethod = map.get(methodName);
                    System.out.println("    * " + methodName + " (Owner: " + currentMethod.getOwner().getName() + ")");
                }
            }
        }
        System.out.println();
    }

    public void setRegisterNames() {
        for(int i = 0; i < classes.size(); i++)
            classMap.get(classes.get(i)).setRegisters();
    }
}

