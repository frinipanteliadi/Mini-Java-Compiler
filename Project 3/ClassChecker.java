import syntaxtree.*;
import visitor.GJDepthFirst;

import java.util.List;

public class ClassChecker extends GJDepthFirst<String, Info> {

    private SymbolTable symbolTable;

    public ClassChecker(SymbolTable symbolTable) { this.symbolTable = symbolTable; }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, Info m) {
        n.f0.accept(this, null);
        if (n.f1.present()) {
            n.f1.accept(this, null);
        }
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
    public String visit(MainClass n, Info m) {
        n.f0.accept(this, null);

        // Every class must have a unique name.
        String className = n.f1.accept(this, null);
        if(symbolTable.getClasses().contains(className)) {
            System.out.println("Line:" + n.f2.beginLine + " Error: Duplicate class " + n.f1.f0);
            System.exit(1);
        }
        else {
            // Add the name of the class to the list of valid data types
            symbolTable.getValidTypes().add(className);

            // Add the name of the current class to the list
            symbolTable.getClasses().add(className);

            // Create a mapping for the current class
            symbolTable.putClass(className, 0, null);
            //symbolTable.printClass(className);

            n.f2.accept(this, null);
            n.f3.accept(this, null);
            n.f4.accept(this, null);

            String returnType = "void";
            n.f5.accept(this, null);

            n.f6.accept(this, null);
            String methodName = "main";
            if(className.equals(methodName)) {
                System.out.println("Line: " + n.f7.beginLine + " Error: Method cannot have the same name as class " + methodName);
                System.exit(1);
            }
            else {
                // Adding the method's name to the list
                symbolTable.putMethod(className, methodName);

                // Creating a mapping for the current method
                symbolTable.getClass(className).putMethod(methodName, "void", 0, symbolTable.getClass(className));
            }


            n.f7.accept(this, null);
            n.f8.accept(this, null);
            n.f9.accept(this, null);
            n.f10.accept(this, null);
        }

        return null;
    }

    /**
     * f0 -> ClassDeclaration()
     *       | ClassExtendsDeclaration()
     */
    public String visit(TypeDeclaration n, Info m) {
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
    public String visit(ClassDeclaration n, Info m) {
        n.f0.accept(this, null);

        String className = n.f1.accept(this, null);
        if(symbolTable.getClasses().contains(className)) {
            System.out.println("Line:" + n.f2.beginLine + " Error: Duplicate class " + n.f1.f0);
            System.exit(1);
        }
        else {
            // Add the name of the class to the list of valid data types
            symbolTable.getValidTypes().add(className);

            // Add the name of the current class to the list
            symbolTable.getClasses().add(className);

            // Create a mapping for the current class
            symbolTable.putClass(className, 0, null);
            //symbolTable.printClass(className);

            if(n.f3.present()) {
                for(int i = 0; i < n.f3.nodes.size(); i++) {
                    String[] parts = n.f3.nodes.get(i).accept(this, null).split(" ");
                    String fieldType = parts[0];
                    String fieldName = parts[1];

                    // Every field in a class must have a unique name
                    boolean flag = true;
                    int index = 0;
                    while(flag == true && index < symbolTable.getClassFields(className).size()) {
                        if((symbolTable.getClassFields(className).get(index).getName()).equals(fieldName)) {
                            System.out.println("Error: Variable " + fieldName + " is already defined in class " + className);
                            flag = false;
                        }

                        index++;
                    }

                    // Create a mapping for the current field
                    if(flag)
                        symbolTable.putField(className, n.f3.nodes.get(i).accept(this, null), 0);
                    else
                        System.exit(1);
                }

                //symbolTable.printClassFields(className);
            }

            if(n.f4.present()) {
                n.f4.accept(this, symbolTable.getClass(className));
            }
        }

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
    public String visit(ClassExtendsDeclaration n, Info m) {
        n.f0.accept(this, null);

        // Every class must have a unique name.
        String className = n.f1.accept(this, null);
        if(symbolTable.getClasses().contains(className)) {
            System.out.println("Line:" + n.f2.beginLine + " Error: Duplicate class " +n.f1.f0);
            System.exit(1);
        }

        // Add the name of the class to the list of valid data types
        symbolTable.getValidTypes().add(className);

        // Add the name of the current class to the list
        symbolTable.getClasses().add(className);

        // Create a mapping for the current class
        symbolTable.putClass(className, 0, null);

        n.f2.accept(this, null);

        /* A class can't extend another class if:
           1) the super class hasn't been declared,
           2) the names of both the super and sub class match.
         */
        String superName = n.f3.accept(this, null);
        if(!symbolTable.getClasses().contains(superName)) {
            System.out.println("Line: " + n.f4.beginLine + " Error: Superclass " + superName + " has not been defined");
            System.exit(1);
        }
        else if(symbolTable.getClasses().contains(superName) && className == superName){
            System.out.println("Line: " + n.f4.beginLine + " Error: Cyclic inheritance involving " + className);
            System.exit(1);
        }
        else {
            symbolTable.setParentClass(className, superName);
            //symbolTable.printClass(className);

            n.f4.accept(this, null);

            if(n.f5.present()) {
                for(int i = 0; i < n.f5.nodes.size(); i++) {
                    String[] parts = n.f5.nodes.get(i).accept(this, null).split(" ");
                    String fieldType = parts[0];
                    String fieldName = parts[1];

                    // Every field in a class must have a unique name
                    boolean flag = true;
                    int index = 0;
                    while(flag == true && index < symbolTable.getClassFields(className).size()) {
                        if((symbolTable.getClassFields(className).get(index).getName()).equals(fieldName)) {
                            System.out.println("Error: Variable " + fieldName + " is already defined in class " + className);
                            flag = false;
                        }

                        index++;
                    }

                    // Create a mapping for the current field
                    if(flag)
                        symbolTable.putField(className, n.f5.nodes.get(i).accept(this, null), 0);
                    else
                        System.exit(1);
                }
                //symbolTable.printClassFields(className);
            }

            if(n.f6.present()) {
                n.f6.accept(this, symbolTable.getClass(className));
            }
        }

        return null;
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, Info m) {
        return n.f0.toString();
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, Info m) {
        //System.out.println("Returning: " + n.f0.accept(this) + " " + n.f1.accept(this));
        return (n.f0.accept(this, null) + " " + n.f1.accept(this, null));
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public String visit(Type n, Info m) {
        String type = n.f0.accept(this, null);
        return type;
    }

    /**
     * f0 -> BooleanArrayType()
     *       | IntegerArrayType()
     */
    public String visit(ArrayType n, Info m) {
        String arrayType = n.f0.accept(this, null);
        return arrayType;
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, Info m) {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, Info m) {
        return n.f0.toString();
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, Info m) {
        return (n.f0.toString() + n.f1.toString() + n.f2.toString());
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n, Info m) {
        return (n.f0.toString() + n.f1.toString() + n.f2.toString());
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
    public String visit(MethodDeclaration n, Info m) {
        n.f0.accept(this, null);

        String returnType = n.f1.accept(this, null);

        String methodName = n.f2.accept(this, null);
        if(((ClassInfo) m).getMethods().contains(methodName)) {
            System.out.println("Line: " + n.f3.beginLine + " Error: Duplicate method name " + methodName);
            System.exit(1);
        }
        else if(m.getName().equals(methodName)) {
            System.out.println("Line: " + n.f3.beginLine + " Error: Method cannot have the same name as class " + m.getName());
            System.exit(1);
        }
        else {
            //System.out.println("Read the method: " + returnType + " " + methodName + "()");

            // Adding the method's name to the list
            ((ClassInfo) m).addMethod(methodName);

            // Create a mapping for the current method
            ((ClassInfo)  m).putMethod(methodName, returnType, 0, ((ClassInfo) m));
        }


        return null;
    }
}
