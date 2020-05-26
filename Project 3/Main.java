import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

class Main {
    public static void main (String [] args){
        if(args.length != 1){
            System.err.println("Usage: java Driver <inputFile>");
            System.exit(1);
        }
        FileInputStream fis = null;

        try{
            fis = new FileInputStream(args[0]);

            MiniJavaParser parser = new MiniJavaParser(fis);
            System.err.println("Program parsed successfully.");
            Goal root = parser.Goal();
            SymbolTable symbolTable = new SymbolTable();

            ClassChecker classChecker = new ClassChecker(symbolTable);
            root.accept(classChecker, null);
            symbolTable.checkDataTypes();

            MethodChecker methodChecker = new MethodChecker(symbolTable);
            root.accept(methodChecker, null);

            /* Creating the .ll file */
            String[] arrOfStr = args[0].split(".java", 2);
            String fileName = "./" + arrOfStr[0] + ".ll";
            System.out.println("Filename: " + fileName);
            FileOutputStream out = new FileOutputStream(fileName);

            StatementChecker statementChecker = new StatementChecker(symbolTable);
            root.accept(statementChecker, null);

            symbolTable.setOffsets();
            symbolTable.setInheritedMethods();
            //symbolTable.printInheritedMethods();

            symbolTable.printSymbolTable();

            /* Creating the V-Tables */
            VTables vtables = new VTables(symbolTable, out);
            vtables.createClassTables();
            //vtables.printClassTables();
            vtables.writeVTables(/*out*/);

            Functions.declareFunctions(out);

            symbolTable.setRegisterNames();

            /* Creating the visitor responsible for the translation */
            Translator translator = new Translator(vtables);
            root.accept(translator, null);

            //System.out.println(root.accept(eval, null));
        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        catch (Exception ex){
            System.err.println(ex.getMessage());
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }
    }
}
