package simulator;

import java.util.HashMap;
import java.util.Stack;

//import arch.*;
import Mips.MipsArch;
import tree.*;

/**
 * This class implements an IRT code simulator. It begins executing at the node
 * following the Label "main", if such a label exists, otherwise it starts at
 * the top of the IRT tree.
 * <p>
 * 
 * Guidelines:
 * <ul>
 * 
 * <li>Programs passed to the simulator should refer to the stack pointer
 * register <tt>$sp</tt> when accessing stack data. This register is maintained
 * automatically by the simulator (as are stack frames), so programs need not
 * modify <tt>$sp</tt>. (In fact, they <i>should</i> not, and programs that
 * attempt to will produce runtime errors.)
 * 
 * <li>CALL nodes should explicitly include the static link as their first
 * argument. It will be placed at an offset of 4 from "sp", and the rest of the
 * arguments will follow it in sequence. (Methods may have up to four arguments
 * in addition to the static link.)
 * 
 * <li>The simulator implements <tt>print</tt>, <tt>exit</tt>, and
 * <tt>malloc</tt> as built-in procedures. Since these are not methods (they
 * don't belong to a class), they aren't expecting to be passed a static link.
 * The exit method causes execution to cease, and can be used after detecting
 * errors (e.g. out-of-bounds array accesses).
 * 
 * <li>Programs may use as many registers as they wish. There are no naming
 * restrictions on them, with the exception of <tt>"$sp"</tt>.
 * </ul>
 * 
 * The simulator keeps separate memory structures for stacks and for main memory
 * (the heap), and one shouldn't expect addresses to conform to the conventions
 * of an actual machine. For example, heap addresses are larger than stack
 * addresses in the simulator, and the stack frame starts at the same address
 * for each procedure. (The simulator creates a new stack frame object for each
 * procedure call.)
 * <p>
 * 
 * Assumptions:
 * <ul>
 * 
 * <li>The left child of a MOVE node is always a MEM or REG expression.
 * 
 * <li>The first child of a CALL node is always a NAME expression.
 * 
 * <li>The target of a JUMP is always a NAME expression.
 * </ul>
 * 
 * 
 * @author Brad Richards
 */

public class Sim {
	private static final int MEM_SIZE = 10000; // In bytes
	private static final int STACK_SIZE = 256; // 256 bytes (64 words) per
												// stack, max
	private static final int SL_OFFSET = 4;
	private int returnVal; // Method return values left in this variable
	private boolean loud; // If true, the simulator prints info as it runs
	private Stack<IRStack> stacks; // The simulated stack frames
	private HashMap<String, Stm> labels; // Map labels to Stm references
	private HashMap<String, Integer> registers; // Map reg names to values
	private Memory memory; // Where we'll store heap items
	private Stm startNode; // Where we'll start executing (might not be root)

	/**
	 * The constructor is passed the IRT tree to simulate. It makes a pass over
	 * the IRT tree to find all labels and stitch together "next" links for Stm
	 * nodes. Finally, it finds the first node to visit during a simulation, and
	 * leaves it in a field for later use
	 * 
	 * @param prog
	 *            An IRT tree to "execute".
	 */
	public Sim(Stm prog) {

		// Make a pass over the tree to find all of the labels, and link them
		// to the appropriate nodes in the tree. Also, stitch together the
		// "next" links of the statements to produce a trail for the simulator
		// to follow.
		labels = FindLabels.traverse(prog);

		// Now we're ready to start. Look for a label called "main", but start
		// at the top of the tree if we can't find main. (It's dangerous to run
		// a program from the top, as we may run "past the end" of a procedure.)
		Stm start;
		if ((start = labels.get("main")) != null) {
			// System.out.println("Found main -- good to go.");
			startNode = start;
		} else {
			System.out.println("WARNING: no main -- starting at top of tree");
			startNode = prog;
		}
	}

	/**
	 * The run method begins the simulated execution of the IRT program passed
	 * to the constructor. The verbose flag controls whether or not it prints
	 * information about what it's doing as it runs the program. (If the flag is
	 * false, the output of print statements will still appear.) This method can
	 * be called repeatedly on the same tree.
	 * 
	 * @param verbose
	 *            If true, simulator describes what it's doing as it executes
	 */
	public void runProgram(boolean verbose) {
		loud = verbose;
		registers = new HashMap<String, Integer>(); // A table of registers
		memory = new Memory(MEM_SIZE, STACK_SIZE); // Our simulated heap
		stacks = new Stack<IRStack>(); // A stack of IRStacks
		stacks.push(new IRStack(STACK_SIZE)); // The initial stack
		registers.put(MipsArch.SP.toString(), 0); // Add SP with stack top
		evalStm(startNode); // Away we go...
	}

	/**
	 * Descend left down the IR tree until we hit the first executable Stm. That
	 * node will have a link pointing to the next statement, and so on.
	 */
	private void evalStm(SEQ s) {
		evalStm(s.left);
	}

	/** Nothing to execute for a COMMENT, so we just move to the next Stm. */
	private void evalStm(COMMENT s) {
		if (loud)
			System.out.println("COMMENT: " + s.text);
		evalStm(s.next());
	}

	/** Nothing to execute for a LABEL, so we just move to the next Stm. */
	private void evalStm(LABEL s) {
		if (loud)
			System.out.println("LABEL: " + ((LABEL) s).label.toString());
		evalStm(s.next());
	}

	/**
	 * JUMPs cause us to shift execution to the specified Label instead of
	 * following the "next" link.
	 */
	private void evalStm(JUMP s) {
		assert (s.exp instanceof NAME);
		String label = ((NAME) (s.exp)).label.toString();
		if (loud)
			System.out.println("JUMPing to " + label);
		if (labels.get(label) == null)
			throw new Error("Label \"" + label + "\" doesn't exist!");
		else
			evalStm(labels.get(label));
	}

	/**
	 * Evaluate the Exps involved in the comparison, perform the specified test,
	 * and jump to the appropriate label instead of following "next". Note that
	 * this code doesn't handle the unsigned comparisons properly -- it performs
	 * signed comparisons instead since Java doesn't believe in unsigned values.
	 */
	private void evalStm(CJUMP s) {
		int left = evalExp(s.left);
		int right = evalExp(s.right);
		boolean test;
		switch (s.relop) {
		case CJUMP.EQ:
			test = (left == right);
			break;
		case CJUMP.NE:
			test = (left != right);
			break;
		case CJUMP.LT:
		case CJUMP.ULT:
			test = (left < right);
			break;
		case CJUMP.GT:
		case CJUMP.UGT:
			test = (left > right);
			break;
		case CJUMP.LE:
		case CJUMP.ULE:
			test = (left <= right);
			break;
		case CJUMP.GE:
		case CJUMP.UGE:
			test = (left >= right);
			break;
		default:
			throw new Error("Print.evalStm.CJUMP");
		}
		String target = test ? s.iftrue.toString() : s.iffalse.toString();
		if (loud)
			System.out.println("CJUMP jumping to " + target);
		if ((labels.get(target) == null))
			throw new Error("Label \"" + target + "\" doesn't exist!");
		else
			evalStm(labels.get(target));
	}

	/**
	 * When simulating a MOVE, we only handle REG or MEM on the left-hand side.
	 * An IRT program shouldn't ever try to move a new value into $sp, so we
	 * watch for that case and warn the user if they attempt it.
	 */
	private void evalStm(MOVE s) {
		// Find the value of the right-hand side
		int value = evalExp(s.src);
		// Figure out whether we're writing to REG or MEM
		if (s.dst instanceof REG) {
			String reg = ((REG) (s.dst)).reg.toString();
			assert (!reg.equals(MipsArch.SP)); // Can't change $sp
			registers.put(reg, value);
			if (loud)
				System.out.println("Reg " + reg + " <-- " + value);
		}
		// We can tell whether this is going into the stack or the heap based
		// on the size of the memory address. "Small" addresses must be for the
		// stack, while larger addresses are for the heap. This doesn't match
		// reality, necessarily, but the IRT program won't notice.
		else if (s.dst instanceof MEM) {
			int addr = evalExp(((MEM) (s.dst)).exp);
			if (addr > STACK_SIZE) {
				memory.write(addr, value);
				if (loud)
					System.out.println("mem[" + addr + "] <-- " + value);
			} else {
				stacks.peek().write(addr, value);
				if (loud)
					System.out.println("stack(" + stacks.size() + ")[" + addr
							+ "] <-- " + value);
			}
		} else
			throw new Error("Unknown node in MOVE: "
					+ s.dst.getClass().getName());
		evalStm(s.next());
	}

	/**
	 * Evaluate the expression contained within the EXPR, then move to the next
	 * IRT statement.
	 */
	private void evalStm(EXPR s) {
		evalExp(s.exp);
		evalStm(s.next());
	}

	/**
	 * Evaluate the expression to be returned, then leave it in the returnVal
	 * instance variable so the CALL mechanism can retrieve it. There's nowhere
	 * to go after hitting a RETURN, so there's no evalStm() call. Instead,
	 * we'll start unwinding Java's call stack until we get back to the CALL
	 * node that got us here.
	 */
	private void evalStm(RETURN s) {
		returnVal = evalExp(s.ret);
	}

	/**
	 * Dispatch an evalStm to the appropriate method. It would be better OO
	 * design if each of the Tree nodes knew how to evaluate itself, but the
	 * folks writing that code didn't imagine the IRT would be executable so
	 * we're forced to do all of this outside of the Tree classes.
	 */
	private void evalStm(Stm s) {
		if (s == null)
			return; // Ran out of Stms
		if (s instanceof SEQ)
			evalStm((SEQ) s);
		else if (s instanceof LABEL)
			evalStm((LABEL) s);
		else if (s instanceof COMMENT)
			evalStm((COMMENT) s);
		else if (s instanceof JUMP)
			evalStm((JUMP) s);
		else if (s instanceof CJUMP)
			evalStm((CJUMP) s);
		else if (s instanceof MOVE)
			evalStm((MOVE) s);
		else if (s instanceof EXPR)
			evalStm((EXPR) s);
		else if (s instanceof RETURN)
			evalStm((RETURN) s);
		else
			throw new Error("Unknown node in evalStm: "
					+ s.getClass().getName());
	}

	/**
	 * Evaluate an IRT expression tree. It evaluates the left and right
	 * subexpressions, then performs the appropriate operation and returns the
	 * result. Note that expression nodes aren't "stitched together" like
	 * statements are -- there's no notion of where to go next after evaluating
	 * an expression. Instead, we just recursively evaluate the expression tree
	 * and return a value.
	 * 
	 * @param e
	 *            An IRT expression tree.
	 */
	public int evalExp(BINOP e) {
		int left = evalExp(e.left);
		int right = evalExp(e.right);
		// System.out.println("Args to binop are "+left+", "+right);
		switch (e.binop) {
		case BINOP.PLUS:
			return left + right;
		case BINOP.MINUS:
			return left - right;
		case BINOP.MUL:
			return left * right;
		case BINOP.DIV:
			return left / right;
		case BINOP.AND:
			return left & right;
		case BINOP.OR:
			return left | right;
		case BINOP.LSHIFT:
			return left << right;
		case BINOP.RSHIFT:
			return left >> right;
			// case BINOP.ARSHIFT: return left >> right;
			// case BINOP.XOR: return left + right;
		default:
			throw new Error("Unknown op in BINOP");
		}
	}

	/**
	 * Both MEM and REG represent storage locations. We'll catch MEM and REG
	 * nodes that appear on the LHS of a MOVE node and handle them separately,
	 * so it's safe to assume that these expressions imply reads instead of
	 * writes. As above, we can tell stack from heap addresses by their
	 * magnitude.
	 */
	private int evalExp(MEM e) {
		int idx = evalExp(e.exp);
		if (idx > STACK_SIZE) {
			if (loud)
				System.out.println("mem[" + idx + "] --> " + memory.read(idx));
			return memory.read(idx);
		} else {
			if (loud)
				System.out.println("stack(" + stacks.size() + ")[" + idx
						+ "] --> " + stacks.peek().read(idx));
			return stacks.peek().read(idx);
		}
	}

	/** Return the contents of the specified register */
	private int evalExp(REG e) {
		if (loud)
			System.out.println("Reg " + e.reg + " --> "
					+ registers.get(e.reg.toString()));
		if (registers.get(e.reg.toString()) == null) {
			System.err.println("Reg " + e.reg + " read before written!");
			return 0;
		} else
			return registers.get(e.reg.toString());
	}

	/**
	 * Simulate the statement within this node, then evaluate the expression and
	 * return its value.
	 */
	private int evalExp(ESEQ e) {
		evalStm(e.stm);
		return evalExp(e.exp);
	}

	/**
	 * We intercept NAME references higher in the tree, so we should never get
	 * one here.
	 */
	private int evalExp(NAME e) {
		throw new Error("Evaluated a NAME expression: " + e);
	}

	/** Return the CONST's value */
	private int evalExp(CONST e) {
		return e.value;
	}

	/**
	 * To evaluate a CALL, we create a new IRStack entry and write the evaluated
	 * arguments to the proper offsets in the stack. (We assume the first of
	 * these arguments is the static link.) We then push our new IRStack onto
	 * the stack of IRStacks and jump to the new method. When control returns,
	 * we pop the stack, retrieve the return value from the returnVal instance
	 * variable, and return it as the value of the CALL node.
	 */
	private int evalExp(CALL e) {
		int result;
		int offset = SL_OFFSET; // Offset of first arg in new stack -- the
								// static link
		assert (e.func instanceof NAME);
		String methodName = ((NAME) (e.func)).label.toString();
		if (loud)
			System.out.println("Evaluating args for CALL to " + methodName
					+ ":");

		IRStack newStack = new IRStack(STACK_SIZE);
		for (ExpList a = e.args; a != null; a = a.tail) {
			result = evalExp(a.head);
			newStack.write(offset, result);
			if (loud)
				System.out.println("stack(" + (stacks.size() + 1) + ")["
						+ (offset) + "] <-arg- " + result);

			offset += 4;
		}
		stacks.push(newStack);

		/*
		 * The arguments to the method are now in a fresh IRStack that's been
		 * pushed on to our stack of stacks. Time to execute the method. Look to
		 * see if it's a built-in (malloc, print, or exit), otherwise jump to
		 * the appropriate label.
		 */
		if (loud)
			System.out.println("Calling " + methodName);
		if (methodName.equals("malloc")) { // Assumes 2nd arg is bytes, not SL
			int arg = stacks.peek().read(SL_OFFSET);
			returnVal = memory.malloc(arg);
		} else if (methodName.equals("print")) {
			int arg = stacks.peek().read(SL_OFFSET);
			System.out.println("PRINT: " + arg);
			returnVal = 0;
		} else if (methodName.equals("exit")) {
			int arg = stacks.peek().read(SL_OFFSET);
			System.out.println("EXITING with value " + arg);
			System.exit(arg);
		} else if (labels.get(methodName) == null)
			throw new Error("Label \"" + methodName + "\" doesn't exist!");
		else
			evalStm(labels.get(methodName));
		stacks.pop();
		if (loud)
			System.out.println("CALL to " + methodName + " returned "
					+ returnVal);
		return returnVal;
	}

	/** Dispatch to the appropriate evalExp method */
	private int evalExp(Exp e) {
		if (e instanceof BINOP)
			return evalExp((BINOP) e);
		else if (e instanceof MEM)
			return evalExp((MEM) e);
		else if (e instanceof REG)
			return evalExp((REG) e);
		else if (e instanceof ESEQ)
			return evalExp((ESEQ) e);
		else if (e instanceof NAME)
			return evalExp((NAME) e);
		else if (e instanceof CONST)
			return evalExp((CONST) e);
		else if (e instanceof CALL)
			return evalExp((CALL) e);
		else
			throw new Error("Unknown node in evalExp: "
					+ e.getClass().getName());
	}
}

/**
 * The memory class simulates a word-oriented computer memory of a fixed size.
 * Addresses are expressed in bytes, but must be word aligned. A primitive
 * malloc is implemented, with no ability to free allocated memory. Instances of
 * this class are used to represent heaps in the simulator. The simulator can
 * only distinguish heap addresses from stack addresses by their size, which has
 * led to a rather inelegant compromise: Stack frames have a maximum size, and
 * valid heap addresses start just above that. (Hence the "start" address passed
 * to the constructor.) Since memory is cheap, we just choose to waste the first
 * portion of the heap.
 */
class Memory {
	private int[] mem; // Where all the words of memory live
	private int freeIdx; // Index of next unused space on heap

	/**
	 * The constructor takes the total size of the simulated memory, and the
	 * starting address at which malloc should start grabbing simulated memory.
	 * If start isn't zero, we end up with some unusable memory.
	 * 
	 * @param size
	 *            Total size of the memory in words.
	 * @param start
	 *            Address at which malloc starts grabbing memory.
	 */
	public Memory(int size, int start) {
		mem = new int[size];
		freeIdx = start / 4 + 1;
	}

	/** Read the word value at the specified address */
	public int read(int idx) {
		if (idx % 4 != 0)
			System.err.println("Non-aligned mem access: " + idx);
		return mem[idx / 4];
	}

	/** Write the word value to the specified address */
	public void write(int idx, int val) {
		if (idx % 4 != 0)
			System.err.println("Non-aligned mem access: " + idx);
		mem[idx / 4] = val;
	}

	/** Allocate numBytes bytes of memory and return its starting address. */
	public int malloc(int numBytes) {
		int request = (int) Math.ceil(numBytes / 4.0);
		int start = freeIdx;
		freeIdx += request;
		return start * 4;
	}
}

/**
 * IRStack instances are used to represent stack frames in the simulator.
 */

class IRStack {
	private int[] mem;
	private int start; // lowest index ever used
	private int size;

	public IRStack(int size) { // Size is in words
		mem = new int[size];
		this.size = size;
		start = size;
	}

	public int topAddr() {
		return size - 4 - size % 4;
	}

	public int read(int idx) {
		if (idx % 4 != 0)
			System.err.println("Non-aligned mem access: " + idx);
		return mem[idx / 4];
	}

	public void write(int idx, int val) {
		if (idx % 4 != 0)
			System.err.println("Non-aligned mem access: " + idx);
		mem[idx / 4] = val;
		start = Math.min(start, idx);
	}

	public void dumpStack(int start) {
		int minIdx = start / 4;
		for (int i = 0; i < minIdx; i++)
			System.out.println((i * 4) + ":  " + mem[i]);
	}
}
