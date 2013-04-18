This is a program generates assembly code for the SimpleJava language (a non-object-oriented version of Java).

This includes: 
	-a lexer and parser written using JavaCC (simplejava.jj)
	-AST and AAT class nodes
	-The driver class (sjc.java)
	-Sample SimpleJava file (queens.sjava)

To run:
	- cd into folder
	- javac *.java
	- java sjc <SimpleJava file> (outputs take the name <fileName given> + ".s")
	- Output can be run using Spim
