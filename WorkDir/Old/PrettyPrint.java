/**
 * This driver code will instantiate a lexer and parser from the SableCC-
 * generated classes, feed it some sample input, then traverse the AST
 * and print a representation of the original program.  
 */

// These imports are for SableCC-generated packages.

package Old;

import minijava.lexer.Lexer;
import minijava.parser.Parser;
import minijava.node.*;

// We also need some general Java I/O classes

import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.InputStreamReader;

/**
 * We extend the DepthFirstAdapter built for us by SableCC, which is basically
 * the same as the base Visitor class you implemented as part of your first
 * assignment. Visitors invoke a defaultCase method on each node they visit. We
 * override this method so that it prints info about a node.
 */
public class PrettyPrint {

	/**
	 * The main method creates an instance of SableCC's Parser that's fed tokens
	 * by an instance of SableCC's Lexer. (The Lexer takes its input from
	 * System.in.) The parser produces an AST, which is then traversed by the
	 * PrintVisitor.
	 */
	public static void main(String[] args) {
		Parser parser = new Parser(new Lexer(new PushbackReader(
				new InputStreamReader(System.in), 1024)));

		try {
			// Ask our parser object to do its thing. Store the AST in start.
			Start start = parser.parse();

			// Retrieve the top-level Program node from start, and apply a
			// PrintVisitor object to it.
			start.getPProgram().apply(
					new PrintVisitor(new PrintWriter(System.out)));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
