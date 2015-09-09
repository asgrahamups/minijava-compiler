package compiler;

import java.io.InputStreamReader;
import java.io.PushbackReader;

import compiler.SymTableVisitor;
import irt.*;
import simulator.Sim;
import tree.*;
import typechecker.TypeCheckerVisitor;
import minijava.lexer.Lexer;
import minijava.node.Start;
import minijava.parser.Parser;

/**
 * This class runs the full compiler and produces any desired output.
 */
public class RunCompiler {
	public static void printUsage() {
		System.out.println("Usage: java compiler.RunCompiler [option] < [infile]");
		System.out.println("Options: mips, runsim");
	}
	
	public static void main(String[] args) {
		if(args.length == 0) {
			printUsage();
			System.exit(1);
		}
		
		
		if (!((args[0].equals("mips")) || (args[0].equals("runsim")))) {
			printUsage();
			System.exit(1);
		}

		Parser parser = new Parser(new Lexer(new PushbackReader(
				new InputStreamReader(System.in), 1024)));

		SymTableVisitor symVisit = new SymTableVisitor();
		IRTVisitor irVisit = new IRTVisitor(symVisit.getTable());

		try {
			// Ask our parser object to do its thing. Store the AST in start.
			Start start = parser.parse();

			// Retrieve the top-level Program node from start, and apply
			// our symbol table visitor to it.
			start.getPProgram().apply(symVisit);

			// same with the IRT visitor
			start.getPProgram().apply(irVisit);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		TypeCheckerVisitor typeCheck = new TypeCheckerVisitor(
				symVisit.getTable());

		Parser checkparser = new Parser(new Lexer(new PushbackReader(
				new InputStreamReader(System.in), 1024)));

		try {
			Start start = checkparser.parse();
			start.getPProgram().apply(typeCheck);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// Check for overriding
		try {
			symVisit.checkOverriding();
		} catch (Exception e) {
			System.exit(-1);
		}

		boolean yesDump = false;

		if (yesDump) {
			symVisit.getTable().dump();
		}

		// decide on our final action: simulate IR, or assembly
		if(args[0].equals("mips")) {
			String headerFilePath = "../Out/header.s";
			MipsVisitor mips = new MipsVisitor(irVisit.irtRepresentation, irVisit.getRegInUse(), 
				headerFilePath, symVisit.getTable());	
		} else if(args[0].equals("runsim")) {
			Sim s = new Sim(irVisit.irtRepresentation);
			s.runProgram(false);
		}
	}
}
