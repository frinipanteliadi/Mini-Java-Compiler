# MiniJava Compiler
My implementation of a compiler for the [MiniJava](http://www.cambridge.org/resources/052182060X/MCIIJ2e/grammar.htm) language (a small subset of Java). 

Its development process consists of three different projects, where each one served as an assignment for the [Department of Informatics and Telecommunications](https://www.di.uoa.gr/en) undergraduate [Compilers](http://cgi.di.uoa.gr/~thp06/) course.

## What is it? 🤔

The compiler is a program that converts instructions written in the MiniJava language into the intermediate representation used by the LLVM Compiler Project ([LLVM Language Reference Manual](https://llvm.org/docs/LangRef.html#instruction-reference)). 

Its operation includes the following stages: 

1. Parsing the MiniJava source files in order to generate a [parse tree](https://en.wikipedia.org/wiki/Parse_tree), detect and report [syntax errors](https://en.wikipedia.org/wiki/Syntax_error).
2. Traversing the parse tree using the [Visitor pattern](https://en.wikipedia.org/wiki/Visitor_pattern), creating a [symbol table](https://en.wikipedia.org/wiki/Symbol_table) and reporting [semantic errors](https://en.wikipedia.org/wiki/Semantics_(computer_science)).
3. Generating code in the LLVM assembly language.


## How does it work? 🤷‍♀️ 

### Semantic Analysis

The compiler uses a symbol table for the purpose of keeping track of information for the different entities found within the source code. Specifically,

- for the **classes** it records their name as well as both the fields and methods they contain,
- for the **methods** it records their variables (local and arguments),
- for the **variables** it records their type and whether they've been initialized or not.

Throughout this process the program is examined for:

1. Duplicate classes 





## What technologies were used? 🖥 

- Java
- JavaCC
- JTB
- JFlex
- Java CUP

## How do I run it? 🏃‍♀️



##   Can I see it? 📸

