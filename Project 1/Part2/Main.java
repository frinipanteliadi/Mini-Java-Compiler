import java_cup.runtime.*;

import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception{
        System.out.println("Please, type in the program you wish to compile.");
        Parser parser = new Parser(new Scanner(new InputStreamReader(System.in)));
        parser.parse();
    }


}
