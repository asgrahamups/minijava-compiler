package unusedchecker;

import java.io.InputStreamReader;
import java.io.PushbackReader;

import compiler.SymTableVisitor;

import typechecker.TypeCheckerVisitor;
import minijava.lexer.Lexer;
import minijava.node.Start;
import minijava.parser.Parser;

public class VariableCheck {

	public static void main(String[] args) {

		Parser parser = new Parser(new Lexer(new PushbackReader(
				new InputStreamReader(System.in), 1024)));
		SymTableVisitor visitor = new SymTableVisitor();

		try {
			// Ask our parser object to do its thing. Store the AST in start.
			Start start = parser.parse();

			// Retrieve the top-level Program node from start, and apply
			// our symbol table visitor to it.
			start.getPProgram().apply(visitor);

			// Check for overriding
			try {
				// visitor.checkOverriding();
			} catch (Exception e) {
				System.exit(-1);
			}

			UnusedVarCheckerVisitor unusedCheck = new UnusedVarCheckerVisitor(
					visitor.getTable());
			start.getPProgram().apply(unusedCheck);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
