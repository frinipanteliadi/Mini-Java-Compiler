import syntaxtree.MainClass;
import syntaxtree.MethodDeclaration;
import visitor.GJDepthFirst;

public class Translator extends GJDepthFirst<String, Info> {

    private VTables vTables;

    public Translator(VTables vTables) { this.vTables = vTables; }

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

        String s;
        s = "define i32 @main() {\n";

        try{
            byte b[] = s.getBytes();
            vTables.getOutFile().write(b);
        }
        catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }

        return null;
    }
}
