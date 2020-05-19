import java.util.LinkedHashMap;

public class VTables {

    public LinkedHashMap<String, ClassTables> classTables;

    public VTables() { classTables = new LinkedHashMap<String, ClassTables>(); }

    public void createVTables(SymbolTable symbolTable) {
        for(int i = 0; i < symbolTable.getClasses().size(); i++) {
            String className = symbolTable.getClasses().get(i);
            //classTables.put(className, new ClassTables(className));

            ClassInfo currentClass = symbolTable.getClass(className);
            while(currentClass.getParent() != null) {



            }

        }
    }
}
