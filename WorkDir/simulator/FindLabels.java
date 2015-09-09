package simulator;

import java.util.HashMap;
import tree.*;

/**
 * This class is passed an IRT program and makes a pass over the entire tree,
 * mapping labels to corresponding tree nodes, and setting each Stm node's
 * "next" link so the simulator has a sequence to follow.
 */
public class FindLabels {
	// We set unlink when we hit main's label. The label's parent then sets the
	// right subtree's next to null.
	private static boolean unlink = false;
	private static HashMap<String, Stm> table;

	public static HashMap<String, Stm> traverse(Stm s) {
		table = new HashMap<String, Stm>();
		visitStm(s, null);

		return table;
	}

	/**
	 * Visiting a SEQ means visiting the left subtree, then the right. When
	 * visiting the left subtree, the "next" link is the top of the right
	 * subtree. When visiting the right subtree, we use the next link passed to
	 * us (the SEQ's next node). The only twist is that if the left subtree is
	 * the LABEL "main", we set next the null as we descend the right subtree.
	 * This ensures that after traversing main during a simulation we don't go
	 * anywhere else once we're done.
	 * 
	 * @param s
	 *            The SEQ node being visited
	 * @param next
	 *            Where to go after this SEQ node
	 */
	static void visitStm(SEQ s, Stm next) {
		visitStm(s.left, s.right);
		if (unlink) {
			unlink = false;
			visitStm(s.right, null);
		} else {
			visitStm(s.right, next);
		}
	}

	static void visitStm(COMMENT s, Stm next) {
		s.link = next;
	}

	static void visitStm(LABEL s, Stm next) {
		String label = s.label.toString();
		if (table.get(label) != null)
			throw new Error("Label \"" + label + "\" is multiply-defined");
		table.put(label, s);
		if (label.equals("main")) {
			unlink = true;
		}
		s.link = next;
	}

	static void visitStm(JUMP s, Stm next) {
		visitExp(s.exp);
		s.link = null;
	}

	static void visitStm(CJUMP s, Stm next) {
		visitExp(s.left);
		visitExp(s.right);
		s.link = next;
	}

	static void visitStm(MOVE s, Stm next) {
		visitExp(s.dst);
		visitExp(s.src);
		s.link = next;
	}

	static void visitStm(EXPR s, Stm next) {
		visitExp(s.exp);
		s.link = next;
	}

	static void visitStm(RETURN s, Stm next) {
		visitExp(s.ret);
		s.link = null;
	}

	public static void visitStm(Stm s, Stm next) {
		if (s instanceof SEQ)
			visitStm((SEQ) s, next);
		else if (s instanceof LABEL)
			visitStm((LABEL) s, next);
		else if (s instanceof COMMENT)
			visitStm((COMMENT) s, next);
		else if (s instanceof JUMP)
			visitStm((JUMP) s, next);
		else if (s instanceof CJUMP)
			visitStm((CJUMP) s, next);
		else if (s instanceof MOVE)
			visitStm((MOVE) s, next);
		else if (s instanceof EXPR)
			visitStm((EXPR) s, next);
		else if (s instanceof RETURN)
			visitStm((RETURN) s, next);
		else {
			throw new Error("Unknown Stm node in visitStm: "
					+ s.getClass().getName());
		}
	}

	static void visitExp(BINOP e) {
		visitExp(e.left);
		visitExp(e.right);
	}

	static void visitExp(MEM e) {
		visitExp(e.exp);
	}

	static void visitExp(REG e) {
		// Nothing to do for a register
	}

	static void visitExp(ESEQ e) {
		visitStm(e.stm, null);
		visitExp(e.exp);
	}

	static void visitExp(NAME e) {
		// Might want to check that this name is eventually in table
	}

	static void visitExp(CONST e) {
		// No need to visit constants
	}

	static void visitExp(CALL e) {
		visitExp(e.func);
		for (ExpList a = e.args; a != null; a = a.tail)
			visitExp(a.head);
	}

	public static void visitExp(Exp e) {
		if (e instanceof BINOP)
			visitExp((BINOP) e);
		else if (e instanceof MEM)
			visitExp((MEM) e);
		else if (e instanceof REG)
			visitExp((REG) e);
		else if (e instanceof ESEQ)
			visitExp((ESEQ) e);
		else if (e instanceof NAME)
			visitExp((NAME) e);
		else if (e instanceof CONST)
			visitExp((CONST) e);
		else if (e instanceof CALL)
			visitExp((CALL) e);
		else
			throw new Error("Unknown Exp node in visitExp: "
					+ e.getClass().getName());
	}
}
