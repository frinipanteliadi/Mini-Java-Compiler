/* ---- Part 1: User Code ---- */
import java_cup.runtime.*;

/* ---- Part 2: Options & Declarations ---- */
%%

%class Scanner
%unicode
%cup
%line
%column

%{
  StringBuffer stringBuffer = new StringBuffer();

  private Symbol symbol(int type){
    return new Symbol(type, yyline, yycolumn);
  }

  private Symbol symbol(int type, Object value){
    return new Symbol(type, yyline, yycolumn, value);
  }
%}

/* An identifier in Java:
   - can include upper-case or lower-case letters, decimal digits,
     the dollar sign ($) or the undescore symbol (_)
   - can't begin with a decimal digit
*/
Ident = [A-Za-z$_] [A-Za-z0-9$_]*

/* From https://www.baeldung.com/java-string-newline: "Adding a new line in
   Java is as simple as including \n" or \r or \r\n at the end of our string"
*/
LineTerminator = \n|\r|\r\n

WhiteSpace = [\t\f ] | {LineTerminator}

/* Java has three types of comments: (1)Single Line, (2)Multi Line and
   (3)Documentation
*/
TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment     = "//" [^\r\n]* {LineTerminator}?
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )*

Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment}

%state STRING

/* ---- Part 3: Lexical Rules ---- */
%%
<YYINITIAL> {
/* Keywords */
"if"      { return symbol(sym.IF); }
"else"    { return symbol(sym.ELSE); }
"prefix"  { return symbol(sym.PREFIX); }
"reverse" { return symbol(sym.REVERSE); }

/* Operators */
"+"      { return symbol(sym.PLUS); }
\"       { stringBuffer.setLength(0); yybegin(STRING); }

/* Separators */
"("      { return symbol(sym.LPAR); }
")"      { return symbol(sym.RPAR); }
","      { return symbol(sym.COMMA); }
"{"      { return symbol(sym.BEGIN); }
"}"      { return symbol(sym.END); }

/* Identifiers */
{Ident}  { return symbol(sym.IDENT, new String(yytext())); }
}

/* Comments */
{Comment} { /* ignore */ }

/* Whitespace */
{WhiteSpace} { /* ignore */ }

<STRING> {
/* The backslash (\) escape character turns special characters into string
   characters
*/

\"           { yybegin(YYINITIAL);
               return symbol(sym.STRING_LITERAL, stringBuffer.toString()); }
[^\n\r\"\\]+ { stringBuffer.append( yytext() ); }
\\t          { stringBuffer.append('\t'); }
\\n          { stringBuffer.append('\n'); }
\\r          { stringBuffer.append('\r'); }
\\\"         { stringBuffer.append('\"'); }
\\           { stringBuffer.append('\\'); }
}

/* Error Fallback */
[^] { throw new Error("Illegal character <"+
                       yytext()+">"); }
