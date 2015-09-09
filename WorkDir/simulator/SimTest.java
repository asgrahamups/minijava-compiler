package simulator;

import arch.*;
import tree.*;

/**
 * Builds a sample IRT program, wraps an instance of the simulator around the
 * tree, then runs the program in the simulator. After building your own IRT
 * program, you can similarly wrap a simulator around it and call runProgram.
 * 
 * @author Brad Richards
 */

public class SimTest {

	// The register to use for our stack pointer
	private static String SP = "$sp";

	/**
	 * This method builds and returns an IRT tree corresponding to the
	 * Factorial.java test program, called on the constant value <i>n</i>.
	 * 
	 * @param n
	 *            The value whose factorial should be computed.
	 */
	private static Stm fac(int n) {
		Exp num = new MEM(new BINOP(BINOP.PLUS, new REG(new Reg(SP)),
				new CONST(8)));
		Exp num_aux = new MEM(new BINOP(BINOP.PLUS, new REG(new Reg(SP)),
				new CONST(12)));
		Label t = new Label("true");
		Label f = new Label("false");
		Label done = new Label("done");
		Label name = new Label("Fac.ComputeFac");
		Label main = new Label("main");

		Stm truePart = new SEQ(new LABEL(t), new SEQ(new MOVE(num_aux,
				new CONST(1)), new JUMP(done)));

		Stm falsePart = new SEQ(new LABEL(f), new SEQ(new MOVE(num_aux,
				new BINOP(BINOP.MUL, num, new CALL(new NAME(name), new ExpList(
						new CONST(0), new ExpList(new BINOP(BINOP.MINUS, num,
								new CONST(1)), null))))), new JUMP(done)));

		Stm cond = new CJUMP(CJUMP.LT, num, new CONST(1), t, f);

		Stm ret = new SEQ(new LABEL(done), new RETURN(num_aux));

		Stm fac = new SEQ(new LABEL(name), new SEQ(cond, new SEQ(truePart,
				new SEQ(falsePart, ret))));

		Stm start = new SEQ(new LABEL(main), new EXPR(new CALL(new NAME(
				new Label("print")), new ExpList(new CALL(new NAME(name),
				new ExpList(new CONST(0), // static link
						new ExpList(new CONST(n), null))), null))));

		// PrintDot.printStm(new SEQ(start, fac));

		return new SEQ(start, fac);
	}

	/**
	 * The main method calls the helper above to build a tree, makes a simulator
	 * instance to simulate it, then starts the simulation.
	 */
	public static void main(String[] args) {
		Stm prog = fac(4); // Build IRT tree for factorial(4)
		// Print out the IRT program before we run it
		Print.prStm(prog);
		// PrintDot.printStm(prog); // Uncomment this if you prefer DOT-style
		// output
		Sim s = new Sim(prog); // Create simulator around our IRT
		s.runProgram(true); // Run the program, with debugging output
		System.out.println("Done.");
	}
}
