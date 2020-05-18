public class StatementInfo extends Info {
    MethodInfo owner; // The method that called the function
    FieldInfo var;
    MethodInfo function; // The function that was called
    int argument;
    ClassInfo owningClass; // The class that owns the method
    // (Inherited) String name;

    // Constructor
    public StatementInfo(MethodInfo owner, FieldInfo var) {
        super(null, 0);
        this.owner = owner;
        this.var = var;
        this.function = null;
        this.argument = 0;
        this.owningClass = null;
    }

    public MethodInfo getOwner() { return this.owner; }

    public ClassInfo getOwningClass() { return this.owningClass; }

    public void setVar(FieldInfo var) { this.var = var; }

    public void setOwningClass(ClassInfo owningClass) { this.owningClass = owningClass; }

    public void setFunction(MethodInfo function) { this.function = function; }

    public MethodInfo getFunction() { return this.function; }

    public int getArgument() { return this.argument; }

    public void increaseArgument() { argument++; }
}
