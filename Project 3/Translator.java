import syntaxtree.*;
import visitor.GJDepthFirst;

import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Translator extends GJDepthFirst<Info, Info> {

    private int ifCounter;
    private int registers;
    private int arrayCounter;
    private int labelCounter;
    private int ArrayAssignmentCounter;
    private VTables vTables;
    private FileOutputStream out;
    private ClassInfo currentClass;
    private SymbolTable symbolTable;
    private MethodInfo currentMethod;
    private FieldInfo currentVariable;
    private List<FieldInfo> methodArguments;

    // Constructor
    public Translator(VTables vTables) {

        ifCounter = 0;
        registers = 0;
        arrayCounter = 0;
        labelCounter = 0;
        ArrayAssignmentCounter = 0;
        this.vTables = vTables;
        out = vTables.getOutFile();
        currentClass = null;
        symbolTable = vTables.getSymbolTable();
        currentMethod = null;
        currentVariable = null;
        methodArguments = new ArrayList<FieldInfo>();
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

    // Locating an identifier
    public VariableType findLocation(FieldInfo value) {

        VariableType variableType;
        variableType = new VariableType(null, null);

        if(currentMethod.variableNameExists(value.getName())) {
            // Case 1: Local variable of the method
            variableType.setVariable(currentMethod.getCertainVariable(value.getName()));
            variableType.setType("local");
        }
        else if(currentMethod.getOwner().fieldNameExists(value.getName())) {
            // Case 2: Field of the class that owns the method
            variableType.setVariable(currentMethod.getOwner().getCertainField(value.getName()));
            variableType.setType("parent");
        }
        else if(currentMethod.getOwner().inheritedField(value.getName())) {
            // Case 3: Field of a super class of the class that owns the method
            variableType.setVariable(currentMethod.getOwner().getInheritedField(value.getName()));
            variableType.setType("super");
        }

        return variableType;
    }

    public String getTempVariable() { return ("%_" + registers++); }

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

        System.out.println("MainClass starts");

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
            if(!currentMethod.isArgument(currentVariable)) {

                type = vTables.setType(currentVariable.getType());
                // Register Name: %<Class_Name>_<Method_Name>_<Variable_Name>
                registerName = "%" + mainClassName + "_" + currentMethod.getName() + "_" + currentVariable.getName();
                currentVariable.setRegName(registerName);

                // Allocating space on the stack for the reference
                writeOutput("\t" + registerName + " = alloca " + type + "\n");
            }
        }

        writeOutput("\n");

        currentVariable = null; // We're done with this (for now)

        // ** Statement **
        if(n.f15.present())
            n.f15.accept(this, null);

        writeOutput("\n\tret i32 0\n}\n\n");

        currentClass = null;
        currentMethod = null;
        registers = 0;

        System.out.println("MainClass ends");

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
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public Info visit(ArrayAssignmentStatement n, Info argu) {
        System.out.println("ArrayAssignmentStatement starts");

        String[] registerNames;
        String type;
        String okLabel, errorLabel;
        FieldInfo identifier;
        FieldInfo expression;
        FieldInfo secondExpression;
        String result = null;

        okLabel = "oob_ok_" + ArrayAssignmentCounter;
        errorLabel = "oob_err_" + ArrayAssignmentCounter;
        ArrayAssignmentCounter++;

        registerNames = new String[7];

        identifier = (FieldInfo)n.f0.accept(this, null);
        expression = (FieldInfo)n.f2.accept(this, null);
        secondExpression = (FieldInfo)n.f5.accept(this, null);

        // ** Locating the identifier **
        if(currentMethod.variableNameExists(identifier.getName())) {
            // Case 1: Local variable of the method
            identifier = currentMethod.getCertainVariable(identifier.getName());
            type = vTables.setType(identifier.getType());


            // Loading the address of the array
            registerNames[0] = getTempVariable();
            writeOutput("\t" + registerNames[0] + " = load " + type + ", " + type + "* " + identifier.getRegName() + "\n");

            // Loading the size of the array
            registerNames[1] = getTempVariable();
            writeOutput("\t" + registerNames[1] + " = load i32, " + type + " " + registerNames[0] + "\n");
        }
        else if(currentMethod.getOwner().fieldNameExists(identifier.getName())) {
            // Case 2: Field of the owning class
            identifier = currentMethod.getOwner().getCertainField(identifier.getName());
        }
        else if(currentMethod.getOwner().inheritedField(identifier.getName())) {
            // Case 3: Field of a super class
            identifier = currentMethod.getOwner().getInheritedField(identifier.getName());
        }

        if(expression.getType().equals("int")) {
            registerNames[2] = getTempVariable();

            // Checking tha the index is greater than zero
            writeOutput("\t" + registerNames[2] + " = icmp sge i32 " + expression.getName() + ", 0\n");

            // Checking that the index is less than the size of the array
            registerNames[3] = getTempVariable();
            writeOutput("\t" +  registerNames[3] + " = icmp slt i32 " + expression.getName() + ", " + registerNames[1] + "\n");

            // Both of the previous conditions must hold
            registerNames[4] = getTempVariable(); // %_8
            writeOutput("\t" + registerNames[4] + " = and i1 " + registerNames[2] + ", " + registerNames[3] + "\n");
            writeOutput("\tbr i1 " + registerNames[4] + ", label %" + okLabel + ", label %" + errorLabel + "\n\n");

            // If not, throw an out of bounds exception
            writeOutput("\t" + errorLabel + ":\n");
            writeOutput("\tcall void @throw_oob()\n\tbr label %" + okLabel + "\n\n");

            // If everything's ok, we can safely index the array
            writeOutput("\t" + okLabel + ":\n");

            // Add one to the index, since the first element holds the size
            registerNames[5] = getTempVariable();
            writeOutput("\t" + registerNames[5] + " = add i32 1, " + expression.getName() + "\n");

            // Get a pointer to the i+1 element of the array
            registerNames[6] = getTempVariable();
            writeOutput("\t" + registerNames[6] + " = getelementptr i32, i32* " + registerNames[0]);
            writeOutput(", i32 " + registerNames[5] + "\n");

            result = registerNames[6];
        }

        if(secondExpression.getType().equals("int"))
            writeOutput("\tstore i32 " + secondExpression.getName() + ", i32* " + result + "\n\n");

        System.out.println("ArrayAssignmentStatement ends");
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public Info visit(IfStatement n, Info argu) {

        System.out.println("IfStatement starts");

        FieldInfo expression;
        String ifLabel, elseLabel, endLabel;

        ifLabel = "if_then_" + ifCounter;
        elseLabel = "if_else_" + ifCounter;
        endLabel = "if_end_" + ifCounter;
        ifCounter++;

        expression = (FieldInfo)n.f2.accept(this, null);

        if(expression.getType().equals("compareExpr")) {
            writeOutput("\tbr i1 " + expression.getName() + ", label %" + ifLabel + ", ");
            writeOutput("label %" + elseLabel + "\n\n");
        }
        else if(expression.getType().equals("andExpr")) {
            writeOutput("\tbr i1 " + expression.getName() + ", label %" + ifLabel + ", ");
            writeOutput("label %" + elseLabel + "\n\n");
        }

        writeOutput("\t" + elseLabel + ":\n");
        n.f6.accept(this, null);
        writeOutput("\tbr label %" + endLabel + "\n\n");

        writeOutput("\t" + ifLabel + ":\n");
        n.f4.accept(this, null);
        writeOutput("\tbr label %" + endLabel + "\n\n");

        writeOutput("\t" + endLabel + ":\n");

        System.out.println("IfStatement ends");
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public Info visit(ClassDeclaration n, Info argu) {

        System.out.println("ClassDeclaration starts");

        String className;
        FieldInfo identifier;

        // Setting the current class
        identifier = (FieldInfo)n.f1.accept(this, null);
        className = identifier.getName();
        currentClass = symbolTable.getClass(className);

        if(n.f4.present())
            n.f4.accept(this, null);

        currentClass = null;
        currentMethod = null;

        System.out.println("ClassDeclaration ends");
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public Info visit(ClassExtendsDeclaration n, Info argu) {
        System.out.println("ClassExtendsDeclaration starts");

        String className;
        FieldInfo identifier;

        identifier = (FieldInfo)n.f1.accept(this, null);
        className = identifier.getName();
        currentClass = symbolTable.getClass(className);

        if(n.f6.present())
            n.f6.accept(this, null);

        currentClass = null;
        currentMethod = null;

        System.out.println("ClassExtendsDeclaration ends");
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public Info visit(MethodDeclaration n, Info argu) {

        System.out.println("MethodDeclaration starts");

        String type = null;
        String regName;
        String methodName;
        String returnType;
        FieldInfo identifier;
        FieldInfo returnStatement;
        String returnRegister = null;
        String[] registerNames = new String[2];

        identifier = (FieldInfo)n.f2.accept(this, null);
        methodName = identifier.getName();
        currentMethod = currentClass.getClassMethod(methodName);
        returnType = vTables.setType(currentMethod.getReturnType());

        writeOutput("define " + returnType + " @" + currentMethod.getOwner().getName() + "." + methodName);
        writeOutput("(i8* %this");

        // Working on the method's arguments
        for(int i = 0; i < currentMethod.getArguments().size(); i++) {

            String[] str_array = currentMethod.getVariables().get(i).getRegName().split("%");
            regName = "%." + str_array[1];
            type = vTables.setType(currentMethod.getVariables().get(i).getType());
            writeOutput(", " + type + " " + regName);
        }

        writeOutput(") {\n");

        // ** VarDeclaration **
        // The variables have already been stored in the MethodInfo class
        for(int i = 0; i < currentMethod.getVariables().size(); i++) {
            currentVariable = currentMethod.getVariables().get(i);
            type = vTables.setType(currentVariable.getType());
            writeOutput("\t" + currentVariable.getRegName() + " = alloca " + type + "\n");

            if(currentMethod.isArgument(currentVariable)) {
                String[] str_array = currentVariable.getRegName().split("%");
                regName = "%." + str_array[1];
                writeOutput("\tstore " + type + " " + regName + ", " + type + "* " + currentVariable.getRegName() + "\n\n");
            }
        }

        writeOutput("\n");

        currentVariable = null;

        // ** Statement **
        if(n.f8.present())
            n.f8.accept(this, null);

        writeOutput("\n");

        // ** Return Statement **
        returnStatement = (FieldInfo)n.f10.accept(this, null);

        if(returnStatement.getType().equals("identifier")) {
            if(currentMethod.variableNameExists(returnStatement.getName())) {
                // Case 1: Local variable of the method
                returnStatement = currentMethod.getCertainVariable(returnStatement.getName());

                returnRegister =  returnStatement.getRegName();
                type = returnStatement.getType();
            }
            else if(currentMethod.getOwner().fieldNameExists(returnStatement.getName())) {
                // Case 2: Field of the owning class
                returnStatement = currentMethod.getOwner().getCertainField(returnStatement.getName());

                // Getting a pointer to the data field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (returnStatement.getOffset()+8) + "\n");

                // Performing the necassary bitcasts
                String temp = getTempVariable();
                writeOutput("\t" + temp + " = bitcast i8* " + ptr + " to " + vTables.setType(returnStatement.getType()) + "*\n");

                returnRegister = getTempVariable();
                writeOutput("\t" + returnRegister + " load " + vTables.setType(returnStatement.getType()) + ", ");
                writeOutput(vTables.setType(returnStatement.getType()) + "* " + temp + "\n\n");

                type = returnStatement.getType();
            }
            else if(currentMethod.getOwner().inheritedField(returnStatement.getName())) {
                // Case 3: Field of a super class
                returnStatement = currentMethod.getOwner().getInheritedField(returnStatement.getName());

                registerNames[0] = getTempVariable();

                writeOutput("\t" + registerNames[0] + " = getelementptr i8, i8* %this, ");
                writeOutput("i32 " + (returnStatement.getOffset() + 8) + "\n");

                registerNames[1] = getTempVariable();

                type = vTables.setType(returnStatement.getType());

                writeOutput("\t" + registerNames[1] + " = bitcast i8* " + registerNames[0] + " to ");
                writeOutput(type + "*\n");

                returnRegister = getTempVariable();

                writeOutput("\t" + returnRegister + " = load " + type + ", " + type + "* " + registerNames[1] + "\n\n");
            }

            writeOutput("\n\tret " + type + " " + returnRegister);
        }
        else if(returnStatement.getType().equals("int")) {
            writeOutput("\n\tret i32 " + returnStatement.getName());
        }


        currentMethod = null;
        registers = 0;

        writeOutput("\n}\n\n");
        System.out.println("MethodDeclaration ends");
        return null;
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

            identifierName = expression.getName();

            if(currentMethod.variableNameExists(identifierName)) {
                // Case 1: Local variable of the method
                expression = currentMethod.getCertainVariable(identifierName);
                type = vTables.setType(expression.getType());
                registerName = getTempVariable();
                writeOutput("\t" + registerName + " = load " + type + ", " + type + "* " + expression.getRegName() + "\n");

                writeOutput("\tcall void(i32) @print_int(i32 " + registerName + ")\n");
            }
            else if(currentMethod.getOwner().fieldNameExists(identifierName)) {
                // Case 2: Field of the owning class
                expression = currentMethod.getOwner().getCertainField(identifierName);

                // Getting a pointer to the field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (expression.getOffset()+8) + "\n");

                // Performing the necessary bitcasts
                registerName = getTempVariable();
                writeOutput("\t" + registerName + " = bitcast i8* " + ptr + " to " + vTables.setType(expression.getType()) + "*\n");
                writeOutput("\tcall void(i32) @print_int(i32 " + registerName + ")\n");


            }
            else if(currentMethod.getOwner().inheritedField(identifierName)) {
                // Case 3: Field of a super class
                expression = currentMethod.getOwner().getInheritedField(identifierName);

                // Getting a pointer to the field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (expression.getOffset()+8) + "\n");

                // Performing the necessary bitcasts
                registerName = getTempVariable();
                writeOutput("\t" + registerName + " = bitcast i8* " + ptr + " to " + vTables.setType(expression.getType()) + "*\n");
                writeOutput("\tcall void(i32) @print_int(i32 " + registerName + ")\n");
            }

        }
        else if(expression.getType().equals("messageSend") || expression.getType().equals("int") ||
                expression.getType().equals("add") || expression.getType().equals("sub"))
            writeOutput("\tcall void (i32) @print_int(i32 " + expression.getName() + ")\n");
//        else if(expression.getType().equals("andExpr")) {
//            registerName = "%_" + registers++;
//
//            writeOutput("\t" + registerName + " = bitcast i1 " + expression.getName() + " to i32\n");
//            writeOutput("\tcall void (i32) @print_int(i32 " + registerName + ")\n");
//        }

        System.out.println("PrintStatement ends");
        return expression;
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
        VariableType variableType;
        String type1 = null, arg1 = null, type2 = null, arg2 = null;

        // Getting the expression
        expression = (FieldInfo)n.f2.accept(this, null);

        if(expression.getType().equals("identifier")) {
            variableType = findLocation(expression);
            expression = variableType.getVariable();

            if(variableType.getType().equals("local")) {

                // Loading the local variable
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = load " + vTables.setType(expression.getType()));
                writeOutput(", " + vTables.setType(expression.getType()) + "* ");
                writeOutput(expression.getRegName() + "\n");

                type1 = vTables.setType(expression.getType());
                arg1 = ptr;
            }

        }
        else if(expression.getType().equals("newIntArrayExpr")) {
            type1 = "i32*";
            arg1 = expression.getName();
        }
        else if(expression.getType().equals("newExpression")) {
            type1 = "i8*";
            arg1 = expression.getName();
        }
        else{
            type1 = "i32";
            arg1 = expression.getName();
        }

        // Getting the identifier
        identifier = (FieldInfo)n.f0.accept(this, null);
        variableType = findLocation(identifier);
        identifier = variableType.getVariable();

        if(!variableType.getType().equals("local")) {
            // Getting a pointer to the field of the class
            String ptr = getTempVariable();
            writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (identifier.getOffset() + 8) + "\n");

            type2 = vTables.setType(identifier.getType()) + "*"; // Pointer to the identifier
            arg2 = getTempVariable();

            writeOutput("\t" + arg2 + " = bitcast i8* " + ptr + " to " + type2 + "\n");
        }
        else {
            type2 = vTables.setType(identifier.getType()) + "*"; // Pointer to the identifier
            arg2 = identifier.getRegName();
        }

        writeOutput("\tstore " + type1 + " " + arg1 + ", " + type2 + " " + arg2 + "\n\n");

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
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public Info visit(PlusExpression n, Info argu) {
        System.out.println("PlusExpression starts");

        VariableType variableType;
        FieldInfo firstPrimaryExpression;
        FieldInfo secondPrimaryExpression;
        String addend_1 = null, addend_2 = null, sum = null;

        firstPrimaryExpression = (FieldInfo)n.f0.accept(this, null);
        secondPrimaryExpression = (FieldInfo)n.f2.accept(this, null);

        if(firstPrimaryExpression.getType().equals("identifier")) {
            variableType = findLocation(firstPrimaryExpression);
            firstPrimaryExpression = variableType.getVariable();

            if(variableType.getType().equals("local")) {
                addend_1 = getTempVariable();
                String type = vTables.setType(firstPrimaryExpression.getType());

                writeOutput("\t" + addend_1 + " = load " + type + ", " + type + "* ");
                writeOutput(firstPrimaryExpression.getRegName() + "\n");
            }
            else {
                // Getting a pointer to the field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (firstPrimaryExpression.getOffset()+8) + "\n");

                // Performing the necessary bitcasts
                String bitcast = getTempVariable();
                writeOutput("\t" + bitcast + " = bitcast i8* " + ptr + " to " + vTables.setType(firstPrimaryExpression.getType()) + "*\n");

                // Loading the value
                addend_1 = getTempVariable();
                writeOutput("\t" + addend_1 + " = load i32, i32* " + bitcast + "\n");
            }
        }
        else if(firstPrimaryExpression.getType().equals("int"))
            addend_1 = firstPrimaryExpression.getName();

        if(secondPrimaryExpression.getType().equals("identifier")) {
            variableType = findLocation(secondPrimaryExpression);
            secondPrimaryExpression = variableType.getVariable();

            if(variableType.getType().equals("local")) {
                addend_2 = getTempVariable();
                String type = vTables.setType(secondPrimaryExpression.getType());
                writeOutput("\t" + addend_2 + " = load " + type);
                writeOutput(", " + type + "* ");
                writeOutput(secondPrimaryExpression.getRegName() + "\n");
            }
            else {
                // Getting a pointer to the field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (secondPrimaryExpression.getOffset()+8) + "\n");

                // Performing the necessary bitcasts
                String bitcast = getTempVariable();
                writeOutput("\t" + bitcast + " = bitcast i8* " + ptr + " to " + vTables.setType(firstPrimaryExpression.getType()) + "*\n");

                // Loading the value
                addend_2 = getTempVariable();
                writeOutput("\t" + addend_2 + " = load i32, i32* " + bitcast + "\n");
            }
        }
        else if(secondPrimaryExpression.getType().equals("int"))
            addend_2 = secondPrimaryExpression.getName();

        sum = getTempVariable();
        writeOutput("\t" + sum + " = add i32 " + addend_1 + ", " + addend_2 + "\n");

        return new FieldInfo("add", sum, -1, false);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public Info visit(MinusExpression n, Info argu) {

        VariableType variableType;
        FieldInfo firstPrimaryExpression;
        FieldInfo secondPrimaryExpression;
        String minuend = null, subtrahend = null, difference = null;

        firstPrimaryExpression = (FieldInfo)n.f0.accept(this, null);
        secondPrimaryExpression = (FieldInfo)n.f2.accept(this, null);

        if(firstPrimaryExpression.getType().equals("identifier")) {

            variableType = findLocation(firstPrimaryExpression);
            firstPrimaryExpression = variableType.getVariable();

            if(variableType.getType().equals("local")) {

                minuend = getTempVariable();

                writeOutput("\t" + minuend + " = load " + vTables.setType(firstPrimaryExpression.getType()));
                writeOutput(", " + vTables.setType(firstPrimaryExpression.getType()) + "* ");
                writeOutput(firstPrimaryExpression.getRegName() + "\n");
            }
            else {
                // Getting a pointer to the field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (firstPrimaryExpression.getOffset()+8) + "\n");

                // Performing the necessary bitcasts
                String bitcast = getTempVariable();
                writeOutput("\t" + bitcast + " = bitcast i8* " + ptr + " to " + vTables.setType(firstPrimaryExpression.getType()) + "*\n");

                minuend = getTempVariable();
                writeOutput("\t" + minuend + " = load i32, i32* " + bitcast + "*\n");
            }
        }
        else if(firstPrimaryExpression.getType().equals("int"))
            minuend = firstPrimaryExpression.getName();

        if(secondPrimaryExpression.getType().equals("identifier")) {
            variableType = findLocation(secondPrimaryExpression);
            secondPrimaryExpression = variableType.getVariable();

            if(variableType.getType().equals("local")) {
                subtrahend = getTempVariable();
                writeOutput("\t" + subtrahend + " = load " + vTables.setType(secondPrimaryExpression.getType()));
                writeOutput(", " + vTables.setType(secondPrimaryExpression.getType()) + "* ");
                writeOutput(secondPrimaryExpression.getRegName() + "\n");
            }
            else {
                // Getting a pointer to the field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (secondPrimaryExpression.getOffset()+8) + "\n");

                // Performing the necessary bitcasts
                String bitcast = getTempVariable();
                writeOutput("\t" + bitcast + " = bitcast i8* " + ptr + " to " + vTables.setType(firstPrimaryExpression.getType()) + "*\n");

                subtrahend = getTempVariable();
                writeOutput("\t" + subtrahend + " = load i32, i32* " + bitcast + "*\n");
            }
        }
        else if(secondPrimaryExpression.getType().equals("int"))
            subtrahend = secondPrimaryExpression.getName();

        difference = getTempVariable();
        writeOutput("\t" + difference + " = sub i32 " + minuend + ", " + subtrahend + "\n");

        return new FieldInfo("sub", difference, -1, false);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public Info visit(AndExpression n, Info argu) {

        System.out.println("AndExpression starts");

        String result;
        String left = null, right = null;
        String label_0, label_1, label_2, label_3;
        FieldInfo firstClause, secondClause;

        firstClause = (FieldInfo)n.f0.accept(this, null);

        if(firstClause.getType().equals("identifier")) {

            if(currentMethod.variableNameExists(firstClause.getName())) {
                // Case 1: Local variable of the method
                firstClause = currentMethod.getCertainVariable(firstClause.getName());

                left = getTempVariable();
                writeOutput("\t" + left + " = load i1, i1* " + firstClause.getRegName() + "\n");
            }
        }

        label_0 = "exp_res_" + labelCounter++;
        label_1 = "exp_res_" + labelCounter++;
        label_2 = "exp_res_" + labelCounter++;
        label_3 = "exp_res_" + labelCounter++;

        writeOutput("\tbr i1 " + left + ", label %" + label_1 + ", label %" + label_0 + "\n\n");

        writeOutput("\t" + label_0 + ":\n");
        writeOutput("\tbr label %" + label_2 + "\n\n");

        writeOutput("\t" + label_1 + ":\n");
        secondClause = (FieldInfo)n.f2.accept(this, null);

        if(secondClause.getType().equals("identifier")) {
            if(currentMethod.variableNameExists(secondClause.getName())) {
                // Case 1: Local variable of the method
                secondClause = currentMethod.getCertainVariable(secondClause.getName());

                right = getTempVariable();
                writeOutput("\t" + right + " = load i1, i1* " + secondClause.getRegName() + "\n\n");
            }
        }

        writeOutput("\tbr label %" + label_2 + "\n\n");


        writeOutput("\t" + label_2 + ":\n");
        writeOutput("\tbr label %" + label_3 + "\n\n");

        writeOutput("\t" + label_3 + ":\n");
        result = getTempVariable();
        writeOutput("\t" + result + " = phi i1 [0, %" + label_0 + "], ");
        writeOutput("[" + right + ", %" + label_2 + "]\n\n");

        System.out.println("AndExpression end");
        return new FieldInfo("andExpr", result, -1, false);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public Info visit(CompareExpression n, Info argu) {

        System.out.println("CompareExpression starts");

        String result;
        String left = null, right = null;
        FieldInfo firstPrimaryExpression, secondPrimaryExpression;

        firstPrimaryExpression = (FieldInfo)n.f0.accept(this, null);
        secondPrimaryExpression = (FieldInfo)n.f2.accept(this, null);

        if(firstPrimaryExpression.getType().equals("identifier")) {
            // ** Locating the identifier **
            if(currentMethod.variableNameExists(firstPrimaryExpression.getName())) {
                // Case 1: Local variable of the method
                firstPrimaryExpression = currentMethod.getCertainVariable(firstPrimaryExpression.getName());

                left = getTempVariable();

                writeOutput("\t" + left + " = load " + vTables.setType(firstPrimaryExpression.getType()));
                writeOutput(", " + vTables.setType(firstPrimaryExpression.getType()) + "* ");
                writeOutput(firstPrimaryExpression.getRegName() + "\n");

            }
            else if(currentMethod.getOwner().fieldNameExists(firstPrimaryExpression.getName())) {
                // Case 2: Field of the owning method
                firstPrimaryExpression = currentMethod.getOwner().getCertainField(firstPrimaryExpression.getName());
            }
            else if(currentMethod.getOwner().inheritedField(firstPrimaryExpression.getName())) {
                // Case 3: Field of a super class
                firstPrimaryExpression = currentMethod.getOwner().getInheritedField(firstPrimaryExpression.getName());
            }
        }

        if(secondPrimaryExpression.getType().equals("int"))
            right = secondPrimaryExpression.getName();

        result = getTempVariable();

        writeOutput("\t" + result + " = icmp slt i32 " + left + ", " + right + "\n");

        System.out.println("CompareExpression ends");
        return new FieldInfo("compareExpr", result, -1, false);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public Info visit(TimesExpression n, Info argu) {
        System.out.println("TimesExpression starts");

        VariableType variableType;
        FieldInfo firstPrimaryExpression;
        FieldInfo secondPrimaryExpression;
        String factor_1 = null, factor_2 = null, product;

        firstPrimaryExpression = (FieldInfo)n.f0.accept(this, null);
        secondPrimaryExpression = (FieldInfo)n.f2.accept(this, null);

        if(firstPrimaryExpression.getType().equals("identifier")) {
            variableType = findLocation(firstPrimaryExpression);
            firstPrimaryExpression = variableType.getVariable();

            if(variableType.getType().equals("local")) {
                factor_1 = getTempVariable();
                String type = vTables.setType(firstPrimaryExpression.getType());

                writeOutput("\t" + factor_1 + " = load " + type + ", ");
                writeOutput(type + "* " + firstPrimaryExpression.getRegName() + "\n");
            }
            else {
                // Getting a pointer to the field
                String ptr = getTempVariable();
                writeOutput("\t" + ptr + " = getelementptr i8, i8* %this, i32 " + (firstPrimaryExpression.getOffset()+8) + "\n");

                // Performing the necessary bitcasts
                String bitcast = getTempVariable();
                writeOutput("\t" + bitcast + " = bitcast i8* " + ptr + " to " + vTables.setType(firstPrimaryExpression.getType()) + "*\n");

                factor_1 = getTempVariable();
                writeOutput("\t" + factor_1 + " = load i32, i32* " + bitcast + "\n");
            }
        }
        else if(firstPrimaryExpression.getType().equals("int"))
            factor_1 = firstPrimaryExpression.getName();

        if(secondPrimaryExpression.getType().equals("int"))
            factor_2 = secondPrimaryExpression.getName();

        product = getTempVariable();

        writeOutput("\t" + product + " = mul i32 " + factor_1 + ", " + factor_2 + "\n\n");

        System.out.println("TimesExpression ends");
        return new FieldInfo("mult", product, -1, false);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public Info visit(MessageSend n, Info argu) {

        System.out.println("MessageSend starts");

        int index;
        String type;
        String methodName;
        FieldInfo primaryExpression;
        MethodInfo calledMethod;
        String[] registerName;
        FieldInfo messageSend = null;

        registerName = new String[7];
        primaryExpression = (FieldInfo)n.f0.accept(this, null);

        if(primaryExpression.getType().equals("identifier")) {
            // The primary expression is an object of a class

            // Case 1: Local variable of the method
            if(currentMethod.variableNameExists(primaryExpression.getName())) {
                //System.out.println("\tCase 1");
                primaryExpression = currentMethod.getCertainVariable(primaryExpression.getName());

                // Load the object pointer
                registerName[0] = getTempVariable();
                writeOutput("\t" + registerName[0] + " = load i8*, i8** " + primaryExpression.getRegName() + "\n\n");

                // Doing the required bitcasts so that we can access the v-table
                registerName[1] = getTempVariable();
                writeOutput("\t" + registerName[1] + " = bitcast i8* " + registerName[0] + " to i8***\n\n");

                // Loading the v-table pointer
                registerName[2] = getTempVariable();
                writeOutput("\t" + registerName[2] + " = load i8**, i8*** " + registerName[1] + "\n\n");

                // Getting the name of the method that's being called
                methodName = ((FieldInfo)n.f2.accept(this, null)).getName();
                calledMethod = symbolTable.getClass(primaryExpression.getType()).getClassMethod(methodName);
                if(calledMethod == null) {
                    System.out.print("Error: Method " + calledMethod + " doesn't exist in class " + primaryExpression.getType());
                    System.out.println(" or any of its superclasses");
                    System.exit(1);
                }

                index = calledMethod.getOffset();

                // Getting a pointer to the first entry of the v-table
                registerName[3] = getTempVariable();
                writeOutput("\t" + registerName[3] + " = getelementptr i8*, i8** " + registerName[2] + ", i32 " + index + "\n");

                // Getting the actual function pointer
                registerName[4] = getTempVariable();
                writeOutput("\t" + registerName[4] + " = load i8*, i8** " + registerName[3] + "\n");

                // Casting the function pointer from i8* to a function ptr type that matches its signature
                registerName[5] = getTempVariable();
                type = vTables.setType(calledMethod.getReturnType());
                writeOutput("\t" + registerName[5] + " = bitcast i8* " + registerName[4] + " to " + type);
                writeOutput(" (i8*");
                for(int i = 0; i < calledMethod.getArguments().size(); i++) {
                    writeOutput(", ");
                    type = vTables.setType(calledMethod.getArguments().get(i).getType());
                    writeOutput(type);
                }
                writeOutput(")*\n\n");

                if(n.f4.present()) {
                    n.f4.accept(this, null);

                    registerName[6] = getTempVariable();
                    type = vTables.setType(calledMethod.getReturnType());
                    writeOutput("\t" + registerName[6] + " = call " + type + " " + registerName[5] + "(i8* " + registerName[0]);

                    for(int i = 0; i < methodArguments.size(); i++) {
                        writeOutput(", ");
                        writeOutput(vTables.setType(methodArguments.get(i).getType()) + " " + methodArguments.get(i).getName());
                    }

                    methodArguments.clear();
                    writeOutput(")\n\n");
                }

                messageSend = new FieldInfo("messageSend", registerName[6], -1, false);
            }
        }

        System.out.println("MessageSend ends");
        return messageSend;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public Info visit(ExpressionList n, Info argu) {

        System.out.println("ExpressionList starts");

        FieldInfo expression;
        FieldInfo expressionTail;

        expression = (FieldInfo)n.f0.accept(this, null);
        methodArguments.add(expression);

        expressionTail = (FieldInfo)n.f1.accept(this, null);
        if(expressionTail == null) {
            /* do nothing */
        }

        System.out.println("ExpressionList ends");
        return null;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public Info visit(ExpressionTail n, Info argu) {
        System.out.println("ExpressionTerm starts");
        FieldInfo expressionTerm = null;
        if(n.f0.present())
            expressionTerm = (FieldInfo)n.f0.accept(this, null);
        System.out.println("ExpressionTerm ends");
        return expressionTerm;
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public Info visit(Clause n, Info argu) {
        System.out.println("Clause starts");
        FieldInfo clause = (FieldInfo)n.f0.accept(this, null);
        System.out.println("Clause ends");
        return clause;
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
        FieldInfo primaryExpression = (FieldInfo) n.f0.accept(this, null);
        System.out.println("PrimaryExpression ends");
        return primaryExpression;
    }

    /**
     * f0 -> BooleanArrayAllocationExpression()
     *       | IntegerArrayAllocationExpression()
     */
    public Info visit(ArrayAllocationExpression n, Info argu) {
        System.out.println("ArrayAllocationExpression starts");

        FieldInfo arrayAllocationExpression = (FieldInfo)n.f0.accept(this, null);

        System.out.println("ArrayAllocationExpression ends");
        return arrayAllocationExpression;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public Info visit(IntegerArrayAllocationExpression n, Info argu) {
        System.out.println("IntegerArrayAllocationExpression starts");

        int size;
        String arraySize;
        String okLabel, errorLabel;
        String[] tempVariables;
        FieldInfo expression = null;
        VariableType variableType;

        tempVariables = new String[4];
        for(int i = 0; i < 4; i++)
            tempVariables[i] = getTempVariable();

        okLabel = "nsz_ok_" + arrayCounter;
        errorLabel = "nsz_err_" + arrayCounter;

        expression = (FieldInfo)n.f3.accept(this, null);

        if(expression.getName().equals("identifier"))
            variableType = findLocation(expression);

        arraySize = expression.getName();
        size = Integer.parseInt(arraySize);

        System.out.println("Array size: " + size);

        writeOutput("\t" + tempVariables[0] + " = add i32 1, " + arraySize + "\n");
        writeOutput("\t" + tempVariables[1] + " = icmp sge i32 " + tempVariables[0] + ", 1\n");
        writeOutput("\tbr i1 " + tempVariables[1] + ", label %" + okLabel + ", label %" + errorLabel + "\n\n");

        writeOutput("\t" + errorLabel + ":\n");
        writeOutput("\tcall void @throw_nsz()\n");
        writeOutput("\tbr label %" + okLabel + "\n\n");

        writeOutput("\t" + okLabel + ":\n");
        writeOutput("\t" + tempVariables[2] + " = call i8* @calloc(i32 " + tempVariables[0] + ", i32 4)\n");
        writeOutput("\t" + tempVariables[3] + " = bitcast i8* " + tempVariables[2] + " to i32*\n");
        writeOutput("\tstore i32 " + arraySize + ", i32* " + tempVariables[3] + "\n\n");

        System.out.println("IntegerArrayAllocationExpression ends");
        return new FieldInfo("newIntArrayExpr", tempVariables[3], -1, false);
    }

    /**
     * f0 -> "false"
     */
    public Info visit(FalseLiteral n, Info argu) {
        System.out.println("FalseLiteral starts");

        String booleanValue = n.f0.toString();

        System.out.println("FalseLiteral ends");
        return new FieldInfo("boolean", booleanValue, -1, false);
    }

    /**
     * f0 -> "true"
     */
    public Info visit(TrueLiteral n, Info argu) {
        System.out.println("TrueLiteral starts");

        String booleanValue = n.f0.toString();

        System.out.println("TrueLiteral ends");
        return new FieldInfo("boolean", booleanValue, -1, false);
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
        identifier = (FieldInfo) n.f1.accept(this, null); // Name of a class

        newClass = symbolTable.getClass(identifier.getName());

        size = (newClass.getObjectSize()) + 8;
        registerName[0] = getTempVariable();
        writeOutput("\t" + registerName[0] + " = call i8* @calloc(i32 1, i32 " + size + ")\n");

        registerName[1] = getTempVariable();
        writeOutput("\t" + registerName[1] + " = bitcast i8* " + registerName[0] + " to i8***\n");

        pointersTableSize = vTables.getClassTables(identifier.getName()).getPointersTable().size();
        registerName[2] = getTempVariable();
        writeOutput("\t" + registerName[2] + " = getelementptr [" + pointersTableSize + " x i8*], [");
        writeOutput(pointersTableSize + " x i8*]* " + vTables.getClassTables(identifier.getName()).getVTableName());
        writeOutput(", i32 0, i32 0\n");

        writeOutput("\tstore i8** " + registerName[2] + ", i8*** " + registerName[1] + "\n");

        System.out.println("AllocationExpression ends");
        return (new FieldInfo("newExpression", registerName[0], -1, false));
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public Info visit(IntegerLiteral n, Info argu) {

        System.out.println("IntegerLiteral starts");
        String intValue;

        intValue = n.f0.toString();

        System.out.println("IntegerLiteral ends");
        return (new FieldInfo("int", intValue, -1, false));
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public Info visit(Identifier n, Info argu) {

        System.out.println("Identifier starts");

        String identifierName;
        identifierName = n.f0.toString();

        System.out.println("Identifier ends");
        return new FieldInfo("identifier", identifierName, -1, false);
    }
}
