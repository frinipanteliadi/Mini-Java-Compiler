import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.List;

public class ClassInfo extends Info {

    private List<FieldInfo> fields;
    private List<String> methods;
    private List<String> inheritedMethods;
    private HashMap<String, MethodInfo> inheritedMethodMap;
    private HashMap<String, MethodInfo> methodMap;
    private int fieldOffset;
    private int pointerOffset;
    ClassInfo parent;


    /* Constructor */
    public ClassInfo(String className, int offset, ClassInfo parent) {
        super(className, offset);

        fields = new ArrayList<FieldInfo>();
        methods = new ArrayList<String>();
        inheritedMethods = new ArrayList<String>();
        methodMap = new HashMap<String, MethodInfo>();
        inheritedMethodMap = new HashMap<String, MethodInfo>();
        fieldOffset = 0;
        pointerOffset = 0;
        this.parent = parent;
    }

    // Returns the size of an object of ClassInfo
    public int getObjectSize() {
        int classSize = 0;
        FieldInfo currentField;
        ClassInfo currentParent;

        for(int i = 0; i < fields.size(); i++) {
            currentField = fields.get(i);

            if(currentField.getType().equals("int"))
                classSize += 4;
            else if(currentField.getType().equals("boolean"))
                classSize += 1;
            else
                classSize += 8;
        }

        currentParent = parent;
        while(currentParent != null) {
            for(int i = 0; i < currentParent.getFields().size(); i++) {
                currentField = currentParent.getFields().get(i);

                if(currentField.getType().equals("int"))
                    classSize += 4;
                else if(currentField.getType().equals("boolean"))
                    classSize += 1;
                else
                    classSize += 8;
            }
            currentParent = currentParent.getParent();
        }

        return classSize;
    }

    public boolean fieldNameExists(String fieldName) {
        boolean exists = false;
        for(int i = 0; i < fields.size(); i++) {
            if(fields.get(i).getName().equals(fieldName)){
                exists = true;
                break;
            }
        }

        return exists;
    }

    public ClassInfo getParent() { return parent; }

    public boolean hasParent() {
        if(parent != null)
            return true;
        else
            return false;
    }

    public FieldInfo getCertainField(String fieldName) {
        FieldInfo field = null;
        for(int i = 0; i < fields.size(); i++) {
            if(fields.get(i).getName().equals(fieldName)) {
                field = fields.get(i);
                break;
            }
        }
        return field;
    }

    public void incFieldOffset(int increment) { fieldOffset += increment; }

    public void incPointerOffset(int increment) { pointerOffset += increment; }

    public int getFieldOffset() { return fieldOffset; }

    public int getPointerOffset() { return pointerOffset; }

    /* Setters */
    public void setParent(ClassInfo parent) {
        this.parent = parent;
    }

    /* Methods for the Lists */
    public void addField(String fieldDecl, int offset) {
        String[] parts = fieldDecl.split(" ");
        String type = parts[0];
        String name = parts[1];

        fields.add(new FieldInfo(type, name, offset, true));
    }

    public void addMethod(String methodName) {
        methods.add(methodName);
    }

    public List<String> getMethods() {
        return methods;
    }

    public HashMap<String, MethodInfo> getMethodMap() { return methodMap; }

    public HashMap<String, MethodInfo> getInheritedMethodMap() { return inheritedMethodMap; }

    public List<String> getInheritedMethods() { return inheritedMethods; }

    public void setInheritedMethods() {

        Stack<ClassInfo> stackOfClasses = new Stack<ClassInfo>();
        ClassInfo currentParent = parent;
        String methodName;
        MethodInfo currentMethod;

        while(currentParent != null) {
            stackOfClasses.push(currentParent);
            currentParent = currentParent.getParent();
        }

        if(!stackOfClasses.isEmpty()) {
            while(!stackOfClasses.isEmpty()) {

                currentParent = stackOfClasses.pop();

                for(int i = 0; i < currentParent.getMethods().size(); i++) {
                    methodName = currentParent.getMethods().get(i);
                    currentMethod = currentParent.getClassMethod(methodName);

                    if(!methods.contains(methodName)) {

                        if(inheritedMethods.contains(methodName)) {
                            inheritedMethodMap.remove(methodName);
                            inheritedMethodMap.put(methodName, currentMethod);
                        }

                        else {
                            inheritedMethods.add(methodName);
                            inheritedMethodMap.put(methodName, currentMethod);
                        }
                    }
                }
            }
        }
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public MethodInfo findMethod(String methodName) {
        if(methods.contains(methodName))
            return methodMap.get(methodName);
        else if(inheritedMethods.contains(methodName))
            return inheritedMethodMap.get(methodName);
        else
            return null;
    }

    /* Methods for the HashMap */
    public void putMethod(String methodName, String returnType, int offset, ClassInfo owner) {
        methodMap.put(methodName, new MethodInfo(returnType, methodName, offset, owner));
    }

    public MethodInfo getClassMethod(String methodName) {
        return methodMap.get(methodName);
    }

    /* Methods for printing */
    public void printClassFields() {
        System.out.print("  - Fields:");

        if(fields.size() == 0)
            System.out.println(" NONE");
        else{
            System.out.println();
            for(int i = 0; i < fields.size(); i++)
                System.out.println("    * " + (fields.get(i)).getType() + " " + (fields.get(i)).getName() + " (Offset = " + (fields.get(i)).getOffset() + " Initialized: " + fields.get(i).getInitialized() + ")");
        }
    }

    public void printClassMethods() {
        System.out.print("  - Methods:");

        if(methods.size() == 0)
            System.out.println(" NONE");
        else {
            System.out.println();
            for (int i = 0; i < methods.size(); i++) {
                MethodInfo currentMethod = methodMap.get(methods.get(i));
                currentMethod.printMethod(currentMethod.getName());
            }
        }

        System.out.print("  - Inherited Methods:");

        if(inheritedMethods.size() == 0)
            System.out.println(" NONE");
        else {
            System.out.println();
            for(int i = 0; i < inheritedMethods.size(); i++) {
                MethodInfo currentMethod = inheritedMethodMap.get(inheritedMethods.get(i));
                System.out.print("    * Name: " + currentMethod.getName() + ", Return Type: " + currentMethod.getReturnType() + " (Offset: " + currentMethod.getOffset() + ")");
                System.out.println(" (Owner: Class " + currentMethod.getOwner().getName() + ")");
            }
        }
    }

    public void printClassInfo() {
        if(parent != null)
            System.out.println("  - Parent Class: " + parent.getName());
        else
            System.out.println("  - Parent Class: NONE");
    }

    public void setRegisters() {

        // Setting the register names for the fields of the class
        for(int i = 0; i < fields.size(); i++)
            fields.get(i).setRegName(this.getName() + "_" + fields.get(i).getName());

        // Setting the register names for the local variables
        for(int i = 0; i < methods.size(); i++)
            methodMap.get(methods.get(i)).setRegisters();
    }

    public boolean inheritedField(String fieldName) {
        boolean flag = false;
        ClassInfo currentParent = parent;

        while(currentParent != null) {

            if(currentParent.fieldNameExists(fieldName)) {
                flag = true;
                break;
            }

            currentParent = currentParent.getParent();
        }

        return flag;
    }

    public FieldInfo getInheritedField(String fieldName) {
        ClassInfo currentParent = parent;
        FieldInfo inheritedField = null;

        while (parent != null) {
            if(currentParent.fieldNameExists(fieldName)) {
                inheritedField = currentParent.getCertainField(fieldName);
                break;
            }
            currentParent = currentParent.getParent();
        }

        return inheritedField;
    }
}