package tree;

import java.util.Stack;

/**
 * A pretty-printer for IRT code that produces an input file for the dot
 * graph-drawing utility
 */

public class PrintDot {

	static java.io.PrintStream out = System.out;
	private static int nodeCount = 0;
	private static Stack<Integer> nodeStack = new Stack<Integer>();

	public static void printStm(Stm s) {
		nodeCount = 0;
		nodeStack = new Stack<Integer>();
		prStm(s);
	}

	public static void printExp(Exp e) {
		nodeCount = 0;
		nodeStack = new Stack<Integer>();
		prExp(e);
	}

	/**
	 * Tidy up as we leave a node. Pop the stack, and watch for the case where
	 * we leave the very last node.
	 */
	public static void defaultOut() {
		nodeStack.pop();
		if (nodeStack.empty()) {
			out.println("}");
			out.flush();
		}
	}

	private static void defaultIn(String name) {
		if (nodeStack.empty()) {
			out.println("\ngraph IRTGraph {");
		}

		// Label this node for dot
		out.println(nodeCount + " [ label=\"" + name + "\" ];");

		// Generate info on link from parent to this node
		if (!nodeStack.empty()) {
			out.print(nodeStack.peek());
			out.print(" -- ");
			out.println(nodeCount);
		}
		nodeStack.push(nodeCount++);
	}

	static void prStm(SEQ s) {
		defaultIn("SEQ");
		prStm(s.left);
		prStm(s.right);
		defaultOut();
	}

	static void prStm(LABEL s) {
		defaultIn("LABEL\\n" + s.label);
		defaultOut();
	}

	static void prStm(COMMENT s) {
		defaultIn("CMT: " + s.text);
		defaultOut();
	}

	static void prStm(JUMP s) {
		defaultIn("JUMP");
		prExp(s.exp);
		defaultOut();
	}

	static void prStm(CJUMP s) {
		String kind;
		switch (s.relop) {
		case CJUMP.EQ:
			kind = "EQ";
			break;
		case CJUMP.NE:
			kind = "NE";
			break;
		case CJUMP.LT:
			kind = "LT";
			break;
		case CJUMP.GT:
			kind = "GT";
			break;
		case CJUMP.LE:
			kind = "LE";
			break;
		case CJUMP.GE:
			kind = "GE";
			break;
		case CJUMP.ULT:
			kind = "ULT";
			break;
		case CJUMP.ULE:
			kind = "ULE";
			break;
		case CJUMP.UGT:
			kind = "UGT";
			break;
		case CJUMP.UGE:
			kind = "UGE";
			break;
		default:
			throw new Error("Unknown OP in CJUMP: " + s.relop);
		}
		defaultIn("CJUMP\\n" + kind);
		prExp(s.left);
		prExp(s.right);
		// These are children of the CJUMP too, right?
		defaultIn(s.iftrue.toString());
		defaultOut();
		defaultIn(s.iffalse.toString());
		defaultOut();
		defaultOut();
	}

	static void prStm(MOVE s) {
		defaultIn("MOVE");
		prExp(s.dst);
		prExp(s.src);
		defaultOut();
	}

	static void prStm(EXPR s) {
		defaultIn("EXPR");
		prExp(s.exp);
		defaultOut();
	}

	static void prStm(RETURN s) {
		defaultIn("RETURN");
		prExp(s.ret);
		defaultOut();
	}

	public static void prStm(Stm s) {
		if (s instanceof SEQ)
			prStm((SEQ) s);
		else if (s instanceof LABEL)
			prStm((LABEL) s);
		else if (s instanceof COMMENT)
			prStm((COMMENT) s);
		else if (s instanceof JUMP)
			prStm((JUMP) s);
		else if (s instanceof CJUMP)
			prStm((CJUMP) s);
		else if (s instanceof MOVE)
			prStm((MOVE) s);
		else if (s instanceof EXPR)
			prStm((EXPR) s);
		else if (s instanceof RETURN)
			prStm((RETURN) s);
		else
			throw new Error("Unknown Stm node in Print: "
					+ s.getClass().getName());
	}

	static void prExp(BINOP e) {
		String kind;
		switch (e.binop) {
		case BINOP.PLUS:
			kind = "+";
			break;
		case BINOP.MINUS:
			kind = "-";
			break;
		case BINOP.MUL:
			kind = "*";
			break;
		case BINOP.DIV:
			kind = "/";
			break;
		case BINOP.AND:
			kind = "&&";
			break;
		case BINOP.OR:
			kind = "||";
			break;
		case BINOP.LSHIFT:
			kind = "LSHIFT";
			break;
		case BINOP.RSHIFT:
			kind = "RSHIFT";
			break;
		case BINOP.ARSHIFT:
			kind = "ARSHIFT";
			break;
		case BINOP.XOR:
			kind = "XOR";
			break;
		default:
			throw new Error("Unknown OP in BINOP: " + e.binop);
		}
		defaultIn("BINOP\\n" + kind);
		prExp(e.left);
		prExp(e.right);
		defaultOut();
	}

	static void prExp(MEM e) {
		defaultIn("MEM");
		prExp(e.exp);
		defaultOut();
	}

	static void prExp(REG e) {
		defaultIn("REG\\n" + e.reg.toString());
		defaultOut();
	}

	static void prExp(ESEQ e) {
		defaultIn("ESEQ");
		prStm(e.stm);
		prExp(e.exp);
		defaultOut();
	}

	static void prExp(NAME e) {
		defaultIn("NAME\\n" + e.label.toString());
		defaultOut();
	}

	static void prExp(CONST e) {
		defaultIn("CONST\\n" + e.value);
		defaultOut();
	}

	static void prExp(CALL e) {
		defaultIn("CALL");
		prExp(e.func);
		for (ExpList a = e.args; a != null; a = a.tail) {
			prExp(a.head);
		}
		defaultOut();
	}

	public static void prExp(Exp e) {
		if (e instanceof BINOP)
			prExp((BINOP) e);
		else if (e instanceof MEM)
			prExp((MEM) e);
		else if (e instanceof REG)
			prExp((REG) e);
		else if (e instanceof ESEQ)
			prExp((ESEQ) e);
		else if (e instanceof NAME)
			prExp((NAME) e);
		else if (e instanceof CONST)
			prExp((CONST) e);
		else if (e instanceof CALL)
			prExp((CALL) e);
		else
			throw new Error("Unknown Exp node in Print: "
					+ e.getClass().getName());
	}

}
