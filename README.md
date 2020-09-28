# MiniJava Compiler
My implementation of a compiler for the [MiniJava](http://www.cambridge.org/resources/052182060X/MCIIJ2e/grammar.htm) language (a small subset of Java). 

Its development process consists of three different projects, where each one served as an assignment for the [Department of Informatics and Telecommunications](https://www.di.uoa.gr/en) undergraduate [Compilers](http://cgi.di.uoa.gr/~thp06/) course.

## What is it? 🤔

The compiler is a program that converts instructions written in the MiniJava language into the intermediate representation used by the LLVM Compiler Project ([LLVM Language Reference Manual](https://llvm.org/docs/LangRef.html#instruction-reference)). 

Its operation includes the following stages: 

1. Parsing the MiniJava source files in order to generate a [parse tree](https://en.wikipedia.org/wiki/Parse_tree), detect and report [syntax errors](https://en.wikipedia.org/wiki/Syntax_error).
2. Traversing the parse tree using the [Visitor pattern](https://en.wikipedia.org/wiki/Visitor_pattern), creating a [symbol table](https://en.wikipedia.org/wiki/Symbol_table) and reporting [semantic errors](https://en.wikipedia.org/wiki/Semantics_(computer_science)).
3. Generating code in the LLVM assembly language using, once again, the Visitor pattern.


## How does it work? 🤷‍♀️ 

### Semantic Analysis and Type Checking

The compiler uses a symbol table for the purpose of keeping track of information for the different entities found within the source code. Specifically,

- regarding the **classes**, it records their name as well as both the fields and methods they contain,
- regarding the **methods**, it records their variables (local and arguments),
- regarding the **variables**, it records their type and whether they've been initialized or not.

Afterwards, the source code is [type checked](https://en.wikipedia.org/wiki/Type_system#Type_checking). 

If at least one of MiniJava's rules is being broken, the compilation process terminates and a message, that describes the error, is displayed. 

### Intermediate Code Generation

By using once again the Visitor pattern alongside a subset of LLVM's instructions, we produce the intermediate representation used by the LLVM compiler project for the original MiniJava code that was provided.

## What technologies were used? 🖥 

- Java
- JavaCC
- JTB
- JFlex
- Java CUP

## How do I run it? 🏃‍♀️

We can run each one of the projects that represent the final compiler separately. Specifically, 

- for **Project 2**, which covers the Semantic Analysis phase, we simply type the following on the command line prompt:

  ```bash
  make clean -C Project\ 2
  ```

  ```bash
  java Main <source_code>.txt
  ```

  *<source_code>.txt* is a .txt file that holds the MiniJava code we wish to use. 

  If no errors have been found, information about the source file's classes will be displayed on the screen. Otherwise, a message explaining what the first error was and where it was found will appear.  

- for **Project 3**, which covers both the Type Checking and the Intermediate Code Generation phases, we type the following:

  ```bash
  make -C Project\ 3
  ```

  ```bash
  java Main <input_file>.java
  ```

  <input_file.java>* is a .java file that holds the Miniava code we wish to compile. 

  After all of the above have been executed, a .ll file named *<input_file>.ll* will have been created. To execute it, we run the following commands:

  ```bash
  clang -o <name_of_result_file> <input_file>.ll
  ```

  ```bash
  ./<result_file>
  ```

  When we're done with running both projects, we remove all of the intermediate files that have been created by running:

  ```bash
  make clean -C Project\ 2
  make clean -C Project\ 3
  ```

##   Can I see it? 📸

![Project2-Example](/Users/frinipanteliadi/Desktop/K31-Compilers/Images/Project2-Example.png)

[^]: Image 1: Example of a successful Semantic Analysis (Project 2)

![Project2-ErrorExample](/Users/frinipanteliadi/Desktop/K31-Compilers/Images/Project2-ErrorExample.png)

[^]: Image 2: Example of an unsuccessful Semantic Analysis (Project 2)

![Project3-Example(Part 1)](/Users/frinipanteliadi/Desktop/K31-Compilers/Images/Project3-Example(Part 1).png)

![Project 3-Example(Part 2)](/Users/frinipanteliadi/Desktop/K31-Compilers/Images/Project 3-Example(Part 2).png)

[^]: Images 3 & 4: Example of generating and executing code in the LLVM assembly language

