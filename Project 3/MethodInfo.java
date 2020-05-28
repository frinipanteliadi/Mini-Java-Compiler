import java.util.ArrayList;
import java.util.List;

public class MethodInfo extends Info {

    private String returnType;
    private List<FieldInfo> arguments;
    private List<FieldInfo> variables;
    private ClassInfo owner; // The class that owns the method

    // Constructor
    public MethodInfo (String returnType, String methodName,  int offset, ClassInfo owner) {
        super(methodName, offset);

        this.returnType = returnType;
        arguments = new ArrayList<FieldInfo>();
        variables = new ArrayList<FieldInfo>();
        //statements = new ArrayList<StatementInfo>();
        this.owner = owner;
    }

    public ClassInfo getOwner() { return owner; }

    // Returns the return type of a method
    public String getReturnType () { return returnType; }

    // Returns the list of arguments
    public List<FieldInfo> getArguments() { return arguments; }

    // Returns the list of variables
    public List<FieldInfo> getVariables() {
        return variables;
    }

    // Returns a local certain variable
    public FieldInfo getCertainVariable(String variableName) {
        FieldInfo var = null;

        if(variableNameExists(variableName)) {
            for(int i = 0; i < variables.size(); i++) {
                if(variables.get(i).getName().equals(variableName)){
                    var = variables.get(i);
                    break;
                }
            }
        }

        return var;
    }

    // Adds a new variable(field) to the list of variables
    public void addVariable(String type, String name, int offset, boolean initialized) {
        variables.add(new FieldInfo(type, name, offset, initialized));
    }

    // Adds a new variable to the list of arguments
    public void addParameter(String type, String name,  int offset, boolean initialized) {
        arguments.add(new FieldInfo(type, name, offset, initialized)); }

    // Checks to see if a variable's named has already been used
    public boolean variableNameExists(String variableName) {
        boolean nameExists = false;
        for(int i = 0; i < variables.size(); i++) {
            if(variables.get(i).getName().equals(variableName)) {
                nameExists = true;
                break;
            }
        }
        return nameExists;
    }

    public boolean checkVariableType(String typeToCheck, String varName) {
        boolean flag = false;

        for(int i = 0; i < variables.size(); i++) {
            if(variables.get(i).getName().equals(varName)) {
                if(variables.get(i).getType().equals(typeToCheck))
                    flag = true;
                break;
            }
        }

        return flag;
    }

    public void printMethod(String methodName) {
        System.out.println("    * Name: " + getName() + ", Return Type: " + returnType + " (Offset: " + this.getOffset() + ")");
        System.out.print("    * Variables: ");

        if(variables.size() == 0)
            System.out.println("NONE");
        else {
            System.out.println();
            for (int i = 0; i < variables.size(); i++) {
                System.out.print("      • " + variables.get(i).getType() + " " + variables.get(i).getName() + " (Initialized: " + variables.get(i).getInitialized() + ")");
                if(i < arguments.size())
                    System.out.println(" (argument)");
                else
                    System.out.println();
            }
        }
    }

    public boolean fieldInSuper(String fieldName) {

        boolean found = false;
        ClassInfo parentPtr = this.getOwner().getParent();


        while(parentPtr != null) {

            if(parentPtr.fieldNameExists(fieldName)) {
                found = true;
                break;
            }
            else {
                parentPtr = parentPtr.getParent();
                continue;
            }
        }

        return found;
    }

    public boolean methodInSuper(String methodName, ClassInfo ptr) {

        boolean flag = false;
        ClassInfo parentPtr = ptr.getParent();

        while(parentPtr != null) {

            if(parentPtr.getMethods().contains(methodName)) {
                flag = true;
                break;
            }
            else {
                parentPtr = parentPtr.getParent();
                continue;
            }
        }

        return flag;
    }

    public ClassInfo getSuperMethod(String methodName) {

        ClassInfo parentPtr = this.getOwner().getParent();
        boolean found = false;

        while(parentPtr != null) {

            if(parentPtr.getMethods().contains(methodName)) {
                found = true;
                break;
            }
            else {
                parentPtr = parentPtr.getParent();
                continue;
            }
        }

        return parentPtr;
    }

    public ClassInfo getSuper(String fieldName) {

        ClassInfo superPtr = null;

        boolean found = false;
        ClassInfo parentPtr = this.getOwner().getParent();

        while(parentPtr != null) {

            if(parentPtr.fieldNameExists(fieldName)) {
                found = true;
                superPtr = parentPtr;
                break;
            }
            else {
                parentPtr = parentPtr.getParent();
                continue;
            }
        }

        return superPtr;
    }

    public boolean checkPolymorphism(String identifierType, ClassInfo idClass) {

        System.out.println("Checking " + identifierType);

        boolean flag = false;
        ClassInfo parentPtr = idClass.getParent();

        while(parentPtr != null) {
            System.out.println("Current class name: " + parentPtr.getName());
            if(parentPtr.getName().equals(identifierType)) {
                flag = true;
                break;
            }
            else {
                parentPtr = parentPtr.getParent();
                continue;
            }
        }
        return flag;
    }

    public boolean isArgument(FieldInfo var) {

        String varName;
        FieldInfo currentArg;

        varName = var.getName();

        for(int i = 0; i < arguments.size(); i++) {
            currentArg = arguments.get(i);
            if(currentArg.getName().equals(varName))
                return true;
        }

        return false;
    }

    public void setRegisters() {
        for(int i = 0; i < variables.size(); i++)
            variables.get(i).setRegName("%" + owner.getName() + "_" + this.getName() + "_" + variables.get(i).getName());
    }
}
