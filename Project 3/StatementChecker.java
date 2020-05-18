import syntaxtree.*;
import visitor.GJDepthFirst;

import javax.swing.plaf.nimbus.State;
import java.util.List;

public class StatementChecker extends GJDepthFirst<String, Info> {
    private SymbolTable symbolTable;

    // Constructor
    public StatementChecker(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, Info argu) {
        if (n.f1.present())
            n.f1.accept(this, null);
        return null;
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
    public String visit(MainClass n, Info argu) {
        String className = n.f1.accept(this, null);
        ClassInfo currentClass = symbolTable.getClass(className);

        if (n.f15.present())
            n.f15.accept(this, currentClass);

        return null;
    }

    /**
     * f0 -> ClassDeclaration()
     * | ClassExtendsDeclaration()
     */
    public String visit(TypeDeclaration n, Info argu) {
        n.f0.accept(this, null);
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
    public String visit(ClassDeclaration n, Info argu) {
        String className = n.f1.accept(this, null);
        ClassInfo currentClass = symbolTable.getClass(className);

        if (n.f4.present())
            n.f4.accept(this, currentClass);

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
    public String visit(ClassExtendsDeclaration n, Info argu) {
        String className = n.f1.accept(this, null);
        ClassInfo currentClass = symbolTable.getClass(className);

        if (n.f6.present())
            n.f6.accept(this, currentClass);

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
    public String visit(MethodDeclaration n, Info argu) {

        String methodName = n.f2.accept(this, null);
        MethodInfo currentMethod = ((ClassInfo) argu).getClassMethod(methodName);

        if (n.f8.present())
            n.f8.accept(this, currentMethod);

        n.f9.accept(this, null);

        String returnType = n.f10.accept(this, currentMethod);
        if (!currentMethod.getReturnType().equals(returnType)) {
            System.out.print("Line: " + n.f11.beginLine + " Error: Incompatible types: ");
            System.out.println("cannot convert return type " + returnType + " to " + currentMethod.getReturnType());
            System.exit(1);
        }

        StatementInfo statement = new StatementInfo(currentMethod, null);
        n.f10.accept(this, statement);

        return null;
    }

    /**
     * f0 -> Block()
     * | AssignmentStatement()
     * | ArrayAssignmentStatement()
     * | IfStatement()
     * | WhileStatement()
     * | PrintStatement()
     */
    public String visit(Statement n, Info argu) {
        n.f0.accept(this, argu);
        return null;
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
    public String visit(ArrayAssignmentStatement n, Info argu) {

        String identifier = n.f0.accept(this, null);
        String arrayType = null;
        ClassInfo superPtr = null;

        // -- Checking if the identifier's name exists --
        // Case 1: Local variable of the method
        if (((MethodInfo) argu).variableNameExists(identifier)) {
            arrayType = ((MethodInfo) argu).getCertainVariable(identifier).getType();
        }

        // Case 2: Field of the class that owns the method
        else if (((MethodInfo) argu).getOwner().fieldNameExists(identifier)) {
            arrayType = ((MethodInfo) argu).getOwner().getCertainField(identifier).getType();
        }

        // Case 3: Field of a super class of the class that owns  the method
        else if (((MethodInfo) argu).fieldInSuper(identifier)) {
            superPtr = ((MethodInfo) argu).getSuper(identifier);
            arrayType = superPtr.getCertainField(identifier).getType();
        } else {
            System.out.println("Line: " + n.f1.beginLine + " Error: Unknown symbol " + identifier);
            System.exit(1);
        }

        // Checking if we're working with an array
        if (!(arrayType.equals("int[]")) ^ (arrayType.equals("boolean[]"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Array required but " + arrayType + " found");
            System.exit(1);
        }

        n.f1.accept(this, null);

        // Checking the type of the expression that refers to the index
        if (!n.f2.accept(this, argu).equals("int")) {
            System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types");
            System.exit(1);
        }

        n.f3.accept(this, null);
        n.f4.accept(this, null);

        // Checking the type of the expression that is to be assigned
        if (arrayType.equals("int[]") && !n.f5.accept(this, argu).equals("int")) {
            System.out.println("Line: " + n.f6.beginLine + " Error: Incompatible types");
            System.exit(1);
        } else if (arrayType.equals("boolean[]") && !n.f5.accept(this, argu).equals("boolean")) {
            System.out.println("Line: " + n.f6.beginLine + " Error: Incompatible types");
            System.exit(1);
        }

        // Checking whether the expression that is to be assigned has been initialized
        StatementInfo statement = new StatementInfo((MethodInfo) argu, null);
        n.f5.accept(this, statement);

        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, Info argu) {
        n.f0.accept(this, null);
        n.f1.accept(this, null);

        // Checking if the expression is initialized
        StatementInfo statement = new StatementInfo(((MethodInfo) argu), null);
        n.f2.accept(this, statement);

        n.f2.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public String visit(Block n, Info argu) {
        n.f0.accept(this, null);
        if (n.f1.present())
            n.f1.accept(this, argu);
        n.f2.accept(this, null);

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
    public String visit(IfStatement n, Info argu) {

        n.f0.accept(this, null);
        n.f1.accept(this, null);

        String expression = n.f2.accept(this, argu);
        if (!expression.equals("boolean")) {
            System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible type in if statement");
            System.exit(1);
        }

        n.f3.accept(this, null);
        n.f4.accept(this, argu);
        n.f5.accept(this, null);
        n.f6.accept(this, argu);

        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, Info argu) {
        n.f0.accept(this, null);
        n.f1.accept(this, null);

        String expressionType = n.f2.accept(this, argu);
        if (!expressionType.equals("boolean")) {
            System.out.println("Line: " + n.f3.beginLine + " Error: Invalid type in while loop");
            System.exit(1);
        }

        n.f4.accept(this, argu);

        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, Info argu) {

        String identifier = n.f0.accept(this, null);
        String identifierType;
        String expressionType;

        // Case 1: the identifier refers to a local variable of the method
        if (((MethodInfo) argu).variableNameExists(identifier)) {

            identifierType = ((MethodInfo) argu).getCertainVariable(identifier).getType();
            expressionType = n.f2.accept(this, argu);

            if (!identifierType.equals(expressionType)) {

                // Checking the case of polymorphism
                if (symbolTable.getClass(expressionType) != null) {
                    if (!((MethodInfo) argu).checkPolymorphism(identifierType, symbolTable.getClass(expressionType))) {
                        System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types (" + identifierType + " = " + expressionType + ")");
                        System.exit(1);
                    }
                } else {
                    System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types (" + identifierType + " = " + expressionType + ")");
                    System.exit(1);
                }
            }

            StatementInfo statement = new StatementInfo((MethodInfo) argu, null);
            n.f2.accept(this, statement);

            // Mark the local variable as initialized
            ((MethodInfo) argu).getCertainVariable(identifier).setInitialized(true);
        }

        // Case 2: the identifier refers to a field of the class that owns the method
        else if (((MethodInfo) argu).getOwner().fieldNameExists(identifier)) {

            identifierType = ((MethodInfo) argu).getOwner().getCertainField(identifier).getType();
            expressionType = n.f2.accept(this, argu);

            if (!identifierType.equals(expressionType)) {

                // Checking the case of polymorphism
                if (symbolTable.getClass(expressionType) != null) {
                    if (!((MethodInfo) argu).checkPolymorphism(identifierType, symbolTable.getClass(expressionType))) {
                        System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types (" + identifierType + " = " + expressionType + ")");
                        System.exit(1);
                    }
                } else {
                    System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types (" + identifierType + " = " + expressionType + ")");
                    System.exit(1);
                }
            }

            StatementInfo statement = new StatementInfo((MethodInfo) argu, null);
            n.f2.accept(this, statement);

            ((MethodInfo) argu).getOwner().getCertainField(identifier).setInitialized(true);
        }

        // Case 3: if the identifier refers to a field of the superclass of the method (if it exists)
        else if (((MethodInfo) argu).fieldInSuper(identifier)) {

            identifierType = ((MethodInfo) argu).getSuper(identifier).getCertainField(identifier).getType();
            expressionType = n.f2.accept(this, argu);

            if (!identifierType.equals(expressionType)) {

                // Checking the case of polymorphism
                if (symbolTable.getClass(expressionType) != null) {
                    if (!((MethodInfo) argu).checkPolymorphism(identifierType, symbolTable.getClass(expressionType))) {
                        System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types (" + identifierType + " = " + expressionType + ")");
                        System.exit(1);
                    }
                } else {
                    System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types (" + identifierType + " = " + expressionType + ")");
                    System.exit(1);
                }
            }

            StatementInfo statement = new StatementInfo((MethodInfo) argu, null);
            n.f2.accept(this, statement);

            // Mark the field of the superclass as initialized
            ((MethodInfo) argu).getSuper(identifier).getCertainField(identifier).setInitialized(true);
        }
        // If none of the above worked, print an error
        else {
            System.out.println("Line: " + n.f1.beginLine + " Error: Unknown name " + identifier);
            System.exit(1);
        }

        return null;
    }

    /**
     * f0 -> AndExpression()
     * | CompareExpression()
     * | PlusExpression()
     * | MinusExpression()
     * | TimesExpression()
     * | ArrayLookup()
     * | ArrayLength()
     * | MessageSend()
     * | Clause()
     */
    public String visit(Expression n, Info argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, Info argu) {

        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            n.f2.accept(this, argu);
            n.f3.accept(this, argu);
            return null;
        }

        String primaryExpression = n.f0.accept(this, null);
        String primaryExpressionType = null;
        String identifier = null; /* The name of the method */
        String returnType = null; /* The return type of the method */
        MethodInfo calledMethod = null;
        boolean flag = true; // The class owns the method
        boolean inParent = false; // The parent owns the method
        StatementInfo statement = null;
        ClassInfo owningClass = null;

        if (primaryExpression.equals("this")) {
            primaryExpressionType = ((MethodInfo) argu).getOwner().getName();
        }
        // Case 2: An object of a class that is also a local variable of the method
        else if (((MethodInfo) argu).variableNameExists(primaryExpression)) {
            primaryExpressionType = ((MethodInfo) argu).getCertainVariable(primaryExpression).getType();
        }

        // Case 3: An object of a class that's also a field of the class that owns the method
        else if (((MethodInfo) argu).getOwner().fieldNameExists(primaryExpression)) {
            primaryExpressionType = ((MethodInfo) argu).getOwner().getCertainField(primaryExpression).getType();
        }

        // Case 4: An object of a class that's also a field of a superclass of the class that owns the method
        else if (((MethodInfo) argu).fieldInSuper(primaryExpression)) {
            primaryExpressionType = ((MethodInfo) argu).getSuper(primaryExpression).getCertainField(primaryExpression).getType();
        } else {
            System.out.println("Line: " + n.f1.beginLine + " Error: Incompatible primary expression for . operator");
            System.exit(1);
        }


        // Checking the type of the primary expression
        if (!symbolTable.classExists(primaryExpressionType)) {
            System.out.print("Line: " + n.f1.beginLine + " Error: " + primaryExpressionType);
            System.out.println(" cannot be dereferenced");
            System.exit(1);
        }

        n.f1.accept(this, null);

        identifier = n.f2.accept(this, null);

        // Checking whether the method exists
        if (!symbolTable.getClass(primaryExpressionType).getMethods().contains(identifier)) {
            // The method does not belong to the calling class

            if (!((MethodInfo) argu).methodInSuper(identifier, symbolTable.getClass(primaryExpressionType))) {
                flag = false; // The calling class does not own the method
                inParent = false; // No superclass owns the method
            } else {
                flag = false; // The calling class does not own the method
                inParent = true; // One of the superclasses owns the method
            }
        }

        if (!flag && !inParent) {
            System.out.print("Line: " + n.f3.beginLine + " Error: Method " + identifier + "() does not exist in the calling class ");

            if (((MethodInfo) argu).getOwner().hasParent())
                System.out.println("and its superclass(es)");
            else
                System.out.println();
            System.exit(1);
        }

        n.f3.accept(this, null);


        if (inParent) {
            calledMethod = ((MethodInfo) argu).getSuperMethod(identifier).getClassMethod(identifier);
            owningClass = ((MethodInfo) argu).getSuper(identifier);
        } else {
            calledMethod = symbolTable.getClass(primaryExpressionType).getClassMethod(identifier);
            owningClass = ((MethodInfo) argu).getOwner();
        }
        // Working on the arguments
        if (n.f4.present()) {

            // Checking whether the method takes any arguments
            if (calledMethod.getArguments().isEmpty()) {
                System.out.println("Line: " + n.f5.beginLine + " Error: Method " + identifier + "() does not take arguments");
                System.exit(1);
            }

            statement = new StatementInfo(((MethodInfo) argu), null);
            statement.setFunction(calledMethod);
            statement.setOwningClass(owningClass);

            n.f4.accept(this, statement);
        }

        returnType = calledMethod.getReturnType();
        return returnType;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, Info argu) {

        boolean flag = false;

        // Find the data type of the argument that was provided
        String firstType = n.f0.accept(this, ((StatementInfo) argu).getOwner());
        String actualFirstType = ((StatementInfo) argu).getFunction().getArguments().get(0).getType();

        if (!((StatementInfo) argu).getFunction().getArguments().get(0).getType().equals(firstType)) {

            if (symbolTable.classExists(firstType)) {
                ClassInfo currentClass = symbolTable.getClass(firstType);
                ClassInfo parentClass = currentClass.getParent();

                while (parentClass != null) {
                    if (parentClass.getName().equals(actualFirstType)) {
                        flag = true;
                        break;
                    } else {
                        parentClass = parentClass.getParent();
                        continue;
                    }
                }
            }

            if (!flag) {
                System.out.print("Error: Incompatible types for first argument in method " + ((StatementInfo) argu).getFunction().getName() + "(). ");
                System.out.println(firstType + " cannot be converted to " + ((StatementInfo) argu).getFunction().getArguments().get(0).getType());
                System.exit(1);
            }
        }

        ((StatementInfo) argu).increaseArgument();

        // Checking whether the expression has been initialized
        StatementInfo statement = new StatementInfo(((StatementInfo) argu).getOwner(), null);
        n.f0.accept(this, statement);

        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, Info argu) {

        if (n.f0.present()) {
            // Checking if the method accepts more than one arguments
            if (((StatementInfo) argu).getFunction().getArguments().size() == 1) {
                System.out.print("Error: Method " + ((StatementInfo) argu).getFunction().getName() + "() takes one ");
                System.out.print("argument, but more were provided");
                System.exit(1);
            }
            n.f0.accept(this, argu);
        }
        return null;
    }


    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, Info argu) {
        int argument = ((StatementInfo) argu).getArgument();

        if (argument >= ((StatementInfo) argu).getFunction().getArguments().size()) {
            System.out.print("Line: " + n.f0.beginLine + " Error: Method " + ((StatementInfo) argu).getFunction().getName());
            System.out.print("() takes " + ((StatementInfo) argu).getOwner().getArguments().size() + " arguments but ");
            System.out.println(" more were provided");
            System.exit(1);
        }

        String argumentType = n.f1.accept(this, ((StatementInfo) argu).getOwner());

        if (!((StatementInfo) argu).getFunction().getArguments().get(argument).getType().equals(argumentType)) {
            System.out.print("Line: " + n.f0.beginLine + " Error in method " + ((StatementInfo) argu).getFunction().getName());
            System.out.println("(): Incompatible types. Cannot convert " + argumentType + " to " + ((StatementInfo) argu).getFunction().getArguments().get(argument).getType());
            System.exit(1);
        }

        ((StatementInfo) argu).increaseArgument();

        StatementInfo statement = new StatementInfo(((StatementInfo) argu).getOwner(), null);
        n.f1.accept(this, statement);

        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, Info argu) {

        if (argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            return null;
        }

        String arrayName = n.f0.accept(this, null);
        String arrayType = null;

        if (((MethodInfo) argu).variableNameExists(arrayName)) {
            arrayType = ((MethodInfo) argu).getCertainVariable(arrayName).getType();
        } else if (((MethodInfo) argu).getOwner().fieldNameExists(arrayName)) {
            arrayType = ((MethodInfo) argu).getOwner().getCertainField(arrayName).getType();
        } else if (((MethodInfo) argu).fieldInSuper(arrayName)) {
            arrayType = ((MethodInfo) argu).getSuper(arrayName).getCertainField(arrayName).getType();
        } else {
            System.out.println("Line: " + n.f1.beginLine + " Error: Symbol " + arrayName + " does not exist");
            System.exit(1);
        }

        // Checking if we're working with an array
        if (!(arrayType.equals("int[]") ^ arrayType.equals("boolean[]"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Array required, but not found");
            System.exit(1);
        }

        n.f2.accept(this, null);
        return "int";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, Info argu) {

        if (argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            n.f2.accept(this, argu);
            return null;
        }

        String arrayName = n.f0.accept(this, null);
        String arrayType = null;

        if (((MethodInfo) argu).variableNameExists(arrayName))
            arrayType = ((MethodInfo) argu).getCertainVariable(arrayName).getType();
        else if (((MethodInfo) argu).getOwner().fieldNameExists(arrayName))
            arrayType = ((MethodInfo) argu).getOwner().getCertainField(arrayName).getType();
        else if (((MethodInfo) argu).fieldInSuper(arrayName))
            arrayType = ((MethodInfo) argu).getSuper(arrayName).getCertainField(arrayName).getType();
        else {
            System.out.println("Line: " + n.f1.beginLine + " Error: Symbol " + arrayName + " does not exist");
            System.exit(1);
        }

        if (!(arrayType.equals("int[]") ^ arrayType.equals("boolean[]"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Array required, but not found");
            System.exit(1);
        }

        String expressionType = n.f2.accept(this, argu);
        if (!expressionType.equals("int")) {
            System.out.println("Line: " + n.f3.beginLine + " Error: Incompatible types: " + expressionType + " cannot be converted to int");
            System.exit(1);
        }

        String returnType = null;

        if (arrayType.equals("int[]"))
            returnType = "int";
        else if (arrayType.equals("boolean[]"))
            returnType = "boolean";

        return returnType;
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, Info argu) {

        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            n.f2.accept(this, argu);
            return null;
        }

        String firstType = n.f0.accept(this, argu);
        n.f1.accept(this, null);
        String secondType = n.f2.accept(this, argu);

        if (!(firstType.equals("boolean") && secondType.equals("boolean"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Bad operand types for && operator (" + firstType + " && " + secondType + ")");
            System.exit(1);
        }

        // Checking whether the primary expressions have been initialized
        StatementInfo statement = new StatementInfo((MethodInfo) argu, null);
        n.f0.accept(this, statement);
        n.f2.accept(this, statement);

        return "boolean";
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, Info argu) {

        // Case for initialization check
        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            n.f2.accept(this, argu);
            return null;
        }

        String firstType = n.f0.accept(this, argu);
        n.f1.accept(this, null);

        String secondType = n.f2.accept(this, argu);

        if (!(firstType.equals("int") && secondType.equals("int"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Incompatible types for * operator");
            System.exit(1);
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, Info argu) {

        // Case for initialization check
        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            n.f2.accept(this, argu);
            return null;
        }

        String firstType = n.f0.accept(this, argu);
        n.f1.accept(this, null);
        String secondType = n.f2.accept(this, argu);

        if (!(firstType.equals("int") && secondType.equals("int"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Incompatible types for - operator");
            System.exit(1);
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, Info argu) {

        // Case for initialization check
        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            n.f2.accept(this, argu);
            return null;
        }

        String firstType = n.f0.accept(this, argu);
        n.f1.accept(this, null);
        String secondType = n.f2.accept(this, argu);

        if (!(firstType.equals("int") && secondType.equals("int"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Incompatible types for + operator");
            System.exit(1);
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, Info argu) {

        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f0.accept(this, argu);
            n.f2.accept(this, argu);
            return null;
        }

        String firstType = n.f0.accept(this, argu);
        n.f1.accept(this, null);
        String secondType = n.f2.accept(this, argu);


        if (!(firstType.equals("int") && secondType.equals("int"))) {
            System.out.println("Line: " + n.f1.beginLine + " Error: Bad operand types for < operator (" + firstType + " < " + secondType + ")");
            System.exit(1);
        }

        // Checking whether the primary expressions have been initialized
        StatementInfo statement = new StatementInfo((MethodInfo) argu, null);
        n.f0.accept(this, statement);
        n.f2.accept(this, statement);

        return "boolean";
    }

    /**
     * f0 -> NotExpression()
     * | PrimaryExpression()
     */
    public String visit(Clause n, Info argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, Info argu) {

        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f1.accept(this, argu);
            return null;
        }

        n.f0.accept(this, null);

        String expressionType = n.f1.accept(this, argu);
        if (expressionType == null)
            System.out.println("Expression type is null");

        if (!expressionType/*n.f1.accept(this, argu)*/.equals("boolean")) {
            System.out.println("Line: " + n.f0.beginLine + " Error: Bad operand type int for unary operator !");
            System.exit(1);
        }

        return "boolean";
    }

    /**
     * Grammar production:
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    public String visit(PrimaryExpression n, Info argu) {

        String primaryExpression;
        primaryExpression = n.f0.accept(this, argu);

        // Case for identifiers: Returning an identifier's data type
        if (argu != null && argu.getClass() == MethodInfo.class) {
            if (((MethodInfo) argu).variableNameExists(primaryExpression)) {
                return ((MethodInfo) argu).getCertainVariable(primaryExpression).getType();
            } else if (((MethodInfo) argu).getOwner().fieldNameExists(primaryExpression)) {
                return ((MethodInfo) argu).getOwner().getCertainField(primaryExpression).getType();
            } else if (((MethodInfo) argu).fieldInSuper(primaryExpression)) {
                return ((MethodInfo) argu).getSuper(primaryExpression).getCertainField(primaryExpression).getType();
            } else {
                return primaryExpression;
            }
        }

        return primaryExpression;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, Info argu) {
        n.f0.accept(this, null);
        String expressionType = n.f1.accept(this, argu);
        n.f2.accept(this, null);
        return expressionType;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, Info argu) {
        n.f0.accept(this, null);

        // Identifier must be a name of a declared class
        String identifier = n.f1.accept(this, null);
        if (symbolTable.getClass(identifier) == null) {
            System.out.println("Line: " + n.f2.beginLine + " Error: Name " + identifier + " does not exist");
            System.exit(1);
        }

        n.f2.accept(this, null);
        n.f3.accept(this, null);

        return identifier;
    }

    /**
     * f0 -> BooleanArrayAllocationExpression()
     * | IntegerArrayAllocationExpression()
     */
    public String visit(ArrayAllocationExpression n, Info argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * Grammar production:
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(BooleanArrayAllocationExpression n, Info argu) {
        // Here, argu is an instance of MethodInfo
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        n.f1.accept(this, null);

        if (!n.f3.accept(this, argu).equals("int")) {
            System.out.println("Line: " + n.f4.beginLine + " Error: Incompatible types");
            System.exit(1);
        }

        return "boolean[]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(IntegerArrayAllocationExpression n, Info argu) {

        if (argu != null && argu.getClass() == StatementInfo.class) {
            n.f3.accept(this, argu);
            return null;
        }

        n.f0.accept(this, null);
        n.f1.accept(this, null);
        n.f2.accept(this, null);

        if (!n.f3.accept(this, argu).equals("int")) {
            System.out.println("Line: " + n.f4.beginLine + " Error: Incompatible types");
            System.exit(1);
        }

        return "int[]";
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, Info argu) {

        if (argu != null && argu.getClass() == StatementInfo.class)
            return null;

        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, Info argu) {
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, Info argu) {
        return "boolean";
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, Info argu) {

        String returnType = null;
        if (argu == null)
            returnType = "this";
        else if (argu != null && argu.getClass() != StatementInfo.class)
            returnType = ((MethodInfo) argu).getOwner().getName();

        return returnType;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, Info argu) {

        // Checking if the value that the identifier refers to is initialized or not
        if (argu != null && argu.getClass() == StatementInfo.class) {
            String identifier = n.f0.toString();
            MethodInfo owner = ((StatementInfo) argu).getOwner();

            // Case 1: The identifier is a local variable of the method
            if (owner.variableNameExists(identifier)) {
                if (!owner.getCertainVariable(identifier).getInitialized()) {
                    System.out.println("Line: " + n.f0.beginLine + " Error: Variable " + identifier + " might not have been initialized");
                    System.exit(1);
                }
            }
            // Case 2: The identifier is a field of the class that owns the method
            else if (owner.getOwner().fieldNameExists(identifier)) {
                if (!owner.getOwner().getCertainField(identifier).getInitialized()) {
                    System.out.println("Line: " + n.f0.beginLine + " Error: Field " + identifier + " might not have been initialized");
                    System.exit(1);
                }
            }
            // Case 3: The identifier is a field of a super class
            else if (owner.fieldInSuper(identifier)) {
                if (!owner.getSuper(identifier).getCertainField(identifier).getInitialized()) {
                    System.out.println("Line: " + n.f0.beginLine + " Error: Field " + identifier + " might not have been initialized");
                    System.exit(1);
                }
            }

            return null;
        }

        // Simply return the name of the identifier
        return n.f0.toString();
    }
}
