import syntaxtree.*;
import visitor.GJDepthFirst;

import java.io.FileOutputStream;
import java.lang.reflect.Field;

public class Translator extends GJDepthFirst<Info, Info> {

    private int registers;
    private VTables vTables;
    private FileOutputStream out;
    private ClassInfo currentClass;
    private SymbolTable symbolTable;
    private MethodInfo currentMethod;
    private FieldInfo currentVariable;

    // Constructor
    public Translator(VTables vTables) {

        registers = 0;
        this.vTables = vTables;
        out = vTables.getOutFile();
        currentClass = null;
        symbolTable = vTables.getSymbolTable();
        currentMethod = null;
        currentVariable = null;
    }

    // Writes a string to the .ll file
    public void writeOutput(String s) {
        try{
            byte b[] = s.getBytes();
            out.write(b);
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public Info visit(MainClass n, Info argu) {

        String type;
        String registerName;
        String mainClassName;

        mainClassName = symbolTable.getClasses().get(0);
        currentClass = symbolTable.getClass(mainClassName);
        currentMethod = currentClass.getClassMethod("main");

        writeOutput("define i32 @main() {\n");

        // ** VarDeclaration **
        // The variables have already been stored in the MethodInfo class
        for(int i = 0; i < currentMethod.getVariables().size(); i++) {

            currentVariable = currentMethod.getVariables().get(i);
            if(currentMethod.isArgument(currentVariable))
                continue;

            type = vTables.setType(currentVariable.getType());
            // Register Name: %<Class_Name>_<Method_Name>_<Variable_Name>
            registerName = "%" + mainClassName + "_" + currentMethod.getName() + "_" + currentVariable.getName();
            currentVariable.setRegName(registerName);

            writeOutput("\t" + registerName + " = alloca " + type + "\n\n");
        }

        currentVariable = null; // We're done with this (for now)

        // ** Statement **
        if(n.f15.present())
            n.f15.accept(this, null);

        writeOutput("\tret i32 0\n}\n\n");

        currentClass = null;
        currentMethod = null;

        return null;
    }

    /**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public Info visit(Statement n, Info argu) {
        System.out.println("Statement Starts");
        FieldInfo statement = (FieldInfo)n.f0.accept(this, null);
        System.out.println("Statement ends");
        return statement;
        //return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public Info visit(PrintStatement n, Info argu) {

        System.out.println("PrintStatement starts");

        FieldInfo expression;
        String registerName;
        String type;

        expression = (FieldInfo)n.f2.accept(this, null);

        if(expression.getType().equals("identifier")) {
            String identifierName;

            // Case 1: Local variable of the method
            identifierName = expression.getName();

            if(currentMethod.variableNameExists(identifierName)) {
                expression = currentMethod.getCertainVariable(identifierName);
                type = vTables.setType(expression.getType());
                registerName = "%_" + registers;
                writeOutput("\t" + registerName + " = load " + type + ", " + type + "* " + expression.getRegName() + "\n\n");

                writeOutput("\tcall void(i32) @print_int(i32 " + registerName + ")\n\n");
            }

            registers++;
        }

        System.out.println("PrintStatement ends");
        return /*null*/expression;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public Info visit(AssignmentStatement n, Info argu) {

        System.out.println("AssignmentStatement starts");

        FieldInfo identifier;
        FieldInfo expression;

        identifier = (FieldInfo)n.f0.accept(this, null); // Changing the currentVariable field

        // Case 1: The identifier is a local variable of the method
        if(currentMethod.variableNameExists(identifier.getName()))
            identifier = currentMethod.getCertainVariable(identifier.getName());


        // In the field called 'name' we (might) have stored a value
        expression = (FieldInfo) n.f2.accept(this, null);

        if(expression.getType().equals("newExpr")) {
            System.out.println("Expr: " + expression.getType());
            writeOutput(identifier.getRegName() + "\n\n");
            return null;
        }

        switch (expression.getType()) {
            case "int":
                writeOutput("\tstore i32 " + expression.getName() + ", i32* " + identifier.getRegName() + "\n\n");
                break;
            case "boolean":
                writeOutput("\tstore i1 " + expression.getName() + ", i1* " + identifier.getRegName() + "\n\n");
                break;
            default:
                writeOutput("\tstore i8* " + expression.getName() + ", i8** " + identifier.getRegName() + "\n\n");
        }

        System.out.println("AssignmentStatement ends");
        return null;
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | Clause()
     */
    public Info visit(Expression n, Info argu) {
        System.out.println("Expression starts");
        FieldInfo expression = (FieldInfo)n.f0.accept(this, null);
        System.out.println("Expression ends");
        return expression;
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public Info visit(Clause n, Info argu) {
        System.out.println("Clause starts");
        FieldInfo expression = (FieldInfo)n.f0.accept(this, null);
        System.out.println("Clause ends");
        return expression;
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | BracketExpression()
     */
    public Info visit(PrimaryExpression n, Info argu) {
        System.out.println("PrimaryExpression starts");
        FieldInfo expression = (FieldInfo) n.f0.accept(this, null);
        System.out.println("PrimaryExpression ends");
        return expression;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public Info visit(AllocationExpression n, Info argu) {

        System.out.println("AllocationExpression starts");

        int size;
        int pointersTableSize;
        FieldInfo identifier;
        String[] registerName;
        ClassInfo newClass;

        registerName = new String[3];
        identifier = (FieldInfo) n.f1.accept(this, null);
        newClass = symbolTable.getClass(identifier.getName());
        System.out.println("Identifier: " + newClass.getName());

        size = (newClass.getObjectSize()) + 8;
        registerName[0] = "%_" + registers;
        registers++;
        writeOutput("\t" + registerName[0] + " = call i8* @calloc(i32 1, i32 " + size + ")\n\n");

        registerName[1] = "%_" + registers;
        registers++;
        writeOutput("\t" + registerName[1] + " = bitcast i8* " + registerName[0] + " to i8***\n\n");

        pointersTableSize = vTables.getClassTables(identifier.getName()).getPointersTable().size();
        registerName[2] = "%_" + registers;
        registers++;
        writeOutput("\t" + registerName[2] + " = getelementptr [" + pointersTableSize + " x i8*], [");
        writeOutput(pointersTableSize + " x i8*]* " + vTables.getClassTables(identifier.getName()).getVTableName());
        writeOutput(", i32 0, i32 0\n\n");

        writeOutput("\tstore i8** " + registerName[2] + ", i8*** " + registerName[1] + "\n\n");
        writeOutput("\tstore i8* " + registerName[0] + ", i8** ");

        return (new FieldInfo("newExpr", identifier.getName(), -1, false));
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public Info visit(IntegerLiteral n, Info argu) {
        String intValue;

        intValue = n.f0.toString();

        return (new FieldInfo("int", intValue, -1, false));
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public Info visit(Identifier n, Info argu) {

        String identifierName;
        FieldInfo identifier;
        identifierName = n.f0.toString();

        //currentVariable = currentMethod.getCertainVariable(identifierName);
        return /*null*/new FieldInfo("identifier", identifierName, -1, false);
    }
}
