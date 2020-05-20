import java.util.*;

public class ClassTables {

    private String className;
    private SymbolTable symbolTable;
    private LinkedHashMap<String, Integer> fieldsTable;
    private LinkedHashMap<String, Integer> pointersTable;

    public ClassTables(String className, SymbolTable symbolTable) {
        this.className = className;
        this.symbolTable = symbolTable;
        fieldsTable = new LinkedHashMap<String, Integer>();
        pointersTable = new LinkedHashMap<String, Integer>();
    }

    public void sortList(List<Pair> list) {
        Pair temp;

        for(int i = 0; i < list.size(); i++) {
            for(int j = i+1; j < list.size(); j++) {

                if(list.get(i).getArrayIndex() > list.get(j).getArrayIndex()) {
                  temp = list.get(i);
                  list.set(i, list.get(j));
                  list.set(j, temp);
                }
            }
        }
    }

    public void createPointersTable() {

        String methodName;
        int arrayIndex;
        int offset;
        List<Pair> tempList;

        ClassInfo currentClass = symbolTable.getClass(className);
        tempList = new ArrayList<Pair>();

        for(int i = 0; i < currentClass.getInheritedMethods().size(); i++) {
            methodName = currentClass.getInheritedMethods().get(i);
            offset = currentClass.getInheritedMethodMap().get(methodName).getOffset();

            if(offset == 0)
                arrayIndex = 0;
            else
                arrayIndex = offset/8;

            tempList.add(new Pair(methodName, arrayIndex));
        }

        for(int i = 0; i < currentClass.getMethods().size(); i++) {
            methodName = currentClass.getMethods().get(i);
            offset = currentClass.getClassMethod(methodName).getOffset();

            if(offset == 0)
                arrayIndex = 0;
            else
                arrayIndex = offset/8;

            tempList.add(new Pair(methodName, arrayIndex));
        }

        sortList(tempList);

        for(int i = 0; i < tempList.size(); i++)
            pointersTable.put(tempList.get(i).getMethodName(), tempList.get(i).getArrayIndex());
    }

    public void printPointersTable() {

        ClassInfo currentClass;

        currentClass = symbolTable.getClass(className);
        System.out.println("Pointers Table for Class " + currentClass.getName() + ":");

        Set<String> keys = pointersTable.keySet();
        for(String k:keys)
            System.out.println("    [" + pointersTable.get(k) + "]: " + k);
        System.out.println();
    }
}

class Pair {
    String methodName;
    Integer arrayIndex;

    public Pair(String methodName, Integer arrayIndex) {
        this.methodName = methodName;
        this.arrayIndex = arrayIndex;
    }

    public Integer getArrayIndex() { return this.arrayIndex; }

    public String getMethodName() { return this.methodName; }
}

