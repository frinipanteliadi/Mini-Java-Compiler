import java.io.IOException;

public class Calculator{
    private int input_symbol; // The symbol that was just read

    public Calculator() throws IOException {
        consumeInput();
    }

    public void consumeInput() throws IOException{
        input_symbol = System.in.read();
    }

    public int calculate() throws ParseException, IOException {
        int value = exp();
        return value;
    }

    // exp -> term exp'
    public int exp() throws ParseException, IOException {
        int value = term();
        int result = exp2(value);
        return result;
    }

    /* exp2 -> + term exp2
            |  - term exp2
            | ε
     */
    public int exp2(int value) throws IOException, ParseException{
        int result;

        if(input_symbol == '+'){
            consumeInput();
            result = value + term();
            return(exp2(result));
        }
        else if(input_symbol == '-'){
            consumeInput();
            result = value - term();
            return(exp2(result));
        }
        else if(input_symbol == '*' || input_symbol == '/' || input_symbol == '(')
            throw new ParseException();

        return value;
    }

    // term -> factor term'
    public int term() throws ParseException, IOException {
        int value = factor();
        int result = term2(value);
        return result;
    }

    /* term2 -> * factor term2
             |  / factor term2
             |  ε
    */
    public int term2(int value) throws IOException, ParseException{
        int result;

        if(input_symbol == '*'){
            consumeInput();
            result = value * factor();
            return (term2(result));
        }
        else if(input_symbol == '/'){
            consumeInput();
            result = value / factor();
            return (term2(result));
        }
        else if(input_symbol == '+' || input_symbol == '-' || input_symbol == '(' || input_symbol == ')'){
            /* we are using the third rule */
        }
        else if(input_symbol >= '0' && input_symbol <= '9' &&  input_symbol == '(')
            throw new ParseException();

        return value;
    }

    /* factor -> num
              |  (exp)
     */
    public int factor() throws ParseException, IOException {
        int value;

        // The current input symbol can only be a decimal digit or a left parentheses
        if(input_symbol >= '0' && input_symbol <= '9') {
            value = num();
            //consumeInput();
        }
        else if(input_symbol == '('){
            consumeInput();
            value = exp();
            if(input_symbol == ')')
                consumeInput();
                return value;
        }
        else
            throw new ParseException();

        return value;
    }

    /* num -> digit
           |  digit num
     */
    public int num() throws IOException, ParseException {
        int lookahead;
        String concat = Integer.toString(digit(input_symbol));

        // Getting multi-digit numbers
        lookahead = System.in.read();
        while(true){
            if(lookahead >= '0' && lookahead <= '9'){
                concat += Integer.toString(digit(lookahead));
            }
            else if (lookahead == '(' || lookahead == ')' || lookahead == '+' || lookahead == '-' ||
            lookahead == '*' || lookahead == '/' || lookahead == '\n' || lookahead == -1) {
                break;
            }
            else
                throw new ParseException();

            lookahead = System.in.read();
        }

        input_symbol = lookahead;
        return Integer.parseInt(concat);
    }

    // digit -> 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
    public int digit(int value){
        return (value - '0');
    }

    public static void main(String[] args){
        try {
            System.out.println("Please, enter a mathematical expression.");
            Calculator calculator = new Calculator();
            System.out.println("Result: " + calculator.calculate());
        }
        catch (IOException e){
            System.out.println(e.toString());
        }
        catch (ParseException e){
            System.out.println(e.getMessage());
        }
    }
}

