import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClassInfo extends Info {

    private List<FieldInfo> fields;
    private List<String> methods;
    private List<String> inheritedMethods;
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
        fieldOffset = 0;
        pointerOffset = 0;
        this.parent = parent;
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

    public List<String> getInheritedMethods() { return inheritedMethods; }

    public List<FieldInfo> getFields() {
        return fields;
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
                //System.out.println("    * Name: " + currentMethod.getName() + ", Return Type: " + currentMethod.getReturnType());
            }
        }
    }

    public void printClassInfo() {
        if(parent != null)
            System.out.println("  - Parent Class: " + parent.getName());
        else
            System.out.println("  - Parent Class: NONE");
    }
}