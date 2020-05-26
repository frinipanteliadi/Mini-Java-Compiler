import syntaxtree.*;
import visitor.GJDepthFirst;

import java.lang.reflect.Method;
import java.util.List;

public class MethodChecker extends GJDepthFirst<String, Info> {

    private SymbolTable symbolTable;

    // Constructor
    public MethodChecker(SymbolTable symbolTable/*, List<String> validTypes*/) {
        this.symbolTable = symbolTable;
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
        String parameterName = n.f11.accept(this, null);

        MethodInfo currentMethod = symbolTable.getClass(className).getClassMethod("main");

        // Adding the name of the argument to the list of the methods variables
        currentMethod.addVariable("String[]", parameterName, 0, true);

        // Adding the name of the argument to the list of the method's arguments
        currentMethod.addParameter("String[]", parameterName, 0, true);

        if(n.f14.present())
            n.f14.accept(this, currentMethod);

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

        if(n.f4.present())
            n.f4.accept(this, symbolTable.getClass(className));

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

        if(n.f6.present())
            n.f6.accept(this, symbolTable.getClass(className));

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

        n.f3.accept(this, null);

        if(n.f4.present())
            n.f4.accept(this, currentMethod);

        // Checking polymorphism
        if(currentMethod.methodInSuper(currentMethod.getName(), ((ClassInfo) argu))) {

            ClassInfo superClass = currentMethod.getSuperMethod(currentMethod.getName());
            MethodInfo superMethod = superClass.getClassMethod(currentMethod.getName());

            // The methods must have the same return type
            if(!superMethod.getReturnType().equals(currentMethod.getReturnType())) {
                System.out.print("Error: Method " + methodName + "() has different return types in super class (" + superMethod.getReturnType());
                System.out.println(") and child (" + currentMethod.getReturnType() + ")");
                System.exit(1);
            }

            // The methods must have the same number of arguments
            if(superMethod.getArguments().size() != currentMethod.getArguments().size()) {
                System.out.print("Error: Method " + methodName + "() must have the same number of arguments");
                System.out.print(" in both the super class (" + superMethod.getArguments().size());
                System.out.println(") and the child (" + currentMethod.getArguments().size() + ") class");
                System.exit(1);
            }

            // The arguments in both methods must have the same data types
            for(int i = 0; i < superMethod.getArguments().size(); i++) {
                String superType = superMethod.getArguments().get(i).getType();
                String childType = currentMethod.getArguments().get(i).getType();

                if(!superType.equals(childType)) {
                    System.out.print("Error: Method " + methodName + "() must have the same type of arguments");
                    System.out.println(" in both the super class and the child class");
                    System.exit(1);
                }
            }
        }



//        if(((ClassInfo) argu).hasParent()) {
//            ClassInfo classParent = ((ClassInfo) argu).getParent();
//
//            if(classParent.getMethods().contains(methodName)) {
//
//                // The methods must have the same return type
//                if(classParent.getClassMethod(methodName).getReturnType() != currentMethod.getReturnType()) {
//                    System.out.print("Error: Method has different return types in parent (" + classParent.getClassMethod(methodName).getReturnType());
//                    System.out.println(") and child (" + currentMethod.getReturnType() + ")");
//                    System.exit(1);
//                }
//
//                // The methods should have the same number of arguments
//                else if(classParent.getClassMethod(methodName).getArguments().size() != currentMethod.getArguments().size()) {
//                    System.out.print("Error: Method " + methodName + "() must have the same number of arguments");
//                    System.out.print(" in both the parent (" + classParent.getClassMethod(methodName).getArguments().size());
//                    System.out.println(") and the child (" + currentMethod.getArguments().size() + ") class");
//                    System.exit(1);
//                }
//
//                // The methods should have the same argument types in the same order
//                else {
//                     for(int i = 0; i < currentMethod.getArguments().size(); i++){
//                        if(!currentMethod.getArguments().get(i).getType().equals(classParent.getClassMethod(methodName).getArguments().get(i).getType())){
//                            System.out.println("Error: Method: " + methodName + "(): Arguments should have the same data types in both the parent and child class");
//                            System.exit(1);
//                            break;
//                        }
//                     }
//                }
//            }
//        }

        n.f5.accept(this, null);
        n.f6.accept(this, null);

        if(n.f7.present())
            n.f7.accept(this, currentMethod);

//        if(n.f8.present())
//            n.f8.accept(this, currentMethod);

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, Info argu) {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, Info argu) {

        String parameterType = n.f0.accept(this, null);
        if(!/*validTypes*/symbolTable.getValidTypes().contains(parameterType)) {
            System.out.println("Error: Invalid type " + parameterType);
            System.exit(1);
        }

        String parameterName = n.f1.accept(this, null);

        // Checking if the name has already been used
        if(((MethodInfo) argu).variableNameExists(parameterName)) {
            System.out.println("Error: The name " + parameterName + " is already being used in method " + argu.getName() + "()");
            System.exit(1);
        }
        // Adding the parameter to the list of arguments
        ((MethodInfo) argu).addParameter(parameterType, parameterName, 0, true);

        // Adding the parameter to the list of variables
        ((MethodInfo) argu).addVariable(parameterType, parameterName, 0, true);

        return null;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public String visit(FormalParameterTail n, Info argu) {
        if(n.f0.present())
            n.f0.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public String visit(FormalParameterTerm n, Info argu) {
        n.f0.accept(this, null);
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public String visit(Type n, Info argu) {
        return super.visit(n, argu);
    }

    /**
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
     */
    public String visit(ArrayType n, Info argu) {
        String arrayType = n.f0.accept(this, null);
        return arrayType;
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, Info argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, Info argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, Info argu) {
        return (n.f0.toString() + n.f1.toString() + n.f2.toString());
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n, Info argu) {
        return (n.f0.toString() + n.f1.toString() + n.f2.toString());
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, Info argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, Info argu) {

        String variableType = n.f0.accept(this, null);
        if(!/*validTypes*/symbolTable.getValidTypes().contains(variableType)){
            System.out.println("Error: invalid type " + variableType);
            System.exit(1);
        }

        String variableName = n.f1.accept(this, null);
        if(((MethodInfo) argu).variableNameExists(variableName)) {
            System.out.println("Error: The name " + variableName + " is already being used in method " + argu.getName() + "()");
            System.exit(1);
        }

        // Adding the name to the list
        ((MethodInfo) argu).addVariable(variableType, variableName, 0, false);

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
    public String visit(Statement n, Info argu) {
        n.f0.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, Info argu) {
        // argu is of type MethodInfo

        String identifierName = n.f0.accept(this, null);
        String expression;

        //  The identifier is a local variable
        if(((MethodInfo) argu).variableNameExists(identifierName)){
            //System.out.println(identifierName + " is a local variable of the method " + ((MethodInfo) argu).getName() + "()");

            n.f1.accept(this, null);
            expression = n.f2.accept(this, null);
            //System.out.println("Expression " + expression);

            /* -- Case 1: Primary Expressions -- */
            // TrueLiteral and FalseLiteral
            if("true".equals(expression) ^ "false".equals(expression)) {
                if(!((MethodInfo) argu).checkVariableType("boolean", identifierName)){
                    System.out.println("Line: " + n.f3.beginLine + " Error: incompatible types.");
                    System.exit(1);
                }
            }

            // ThisExpression
            if("this".equals(expression)) {
                boolean flag = false;

                if(((MethodInfo) argu).checkVariableType(((MethodInfo) argu).getOwner().getName(), identifierName))
                    flag = true;
                // Case of polymorphism
                else if(((MethodInfo) argu).getOwner().hasParent() && ((MethodInfo) argu).checkVariableType(((MethodInfo) argu).getOwner().getParent().getName(), identifierName))
                    flag = true;

                if(flag == false) {
                    System.out.println("Line: " + n.f3.beginLine + " Error: incompatible types.");
                    System.exit(1);
                }
            }

            // IntegerLiteral
            if(SymbolTable.isInteger(expression, 10)) {
                if(!((MethodInfo) argu).checkVariableType("int", identifierName)) {
                    System.out.println("Line: " + n.f3.beginLine + " Error: incompatible types.");
                    System.exit(1);
                }
            }

            // ArrayAllocationExpression
            if(expression.contains("new int[") || expression.contains("new boolean[")) {
                System.out.println("This is an array allocation");
            }
        }

        // The identifier is a field of the class
        else if(((MethodInfo) argu).getOwner().fieldNameExists(identifierName)) {
            System.out.println(identifierName + " is a field of the class");

            n.f1.accept(this, null);
            expression = n.f2.accept(this, null);
        }

        // The identifier is a field of the super class
        else if(((MethodInfo) argu)./*fieldInSuper(identifierName)*/getOwner().hasParent() == true && ((MethodInfo) argu).getOwner().getParent().fieldNameExists(identifierName)) {
            System.out.println(identifierName + " is a field of a superclass");

            n.f1.accept(this, null);
            expression = n.f2.accept(this, null);
        }

        // If all else fails, return an error
        else {
            System.out.println("Line: " + n.f1.beginLine + " Error: Name " + identifierName + " does not exist");
            System.exit(1);
        }
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
    public String visit(Expression n, Info argu) {
        return n.f0.accept(this, null);
    }

    /**
     * f0 -> NotExpression()
     *       | PrimaryExpression()
     */
    public String visit(Clause n, Info argu) {
        return n.f0.accept(this, null);
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
    public String visit(PrimaryExpression n, Info argu) {
        return n.f0.accept(this, null);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, Info argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, Info argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, Info argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, Info argu) {
        return n.f0.toString();
    }

    /**
     * f0 -> BooleanArrayAllocationExpression()
     *       | IntegerArrayAllocationExpression()
     */
    public String visit(ArrayAllocationExpression n, Info argu) {
        return n.f0.accept(this, null);
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
        n.f0.accept(this, null);
        n.f1.accept(this, null);
        n.f2.accept(this, null);

        String expression = n.f3.accept(this, null);
        System.out.println("Expression:  " + expression);
        n.f4.accept(this, null);

        return ("new boolean[" + n.f4.accept(this, null).toString() + "]");
    }
}


