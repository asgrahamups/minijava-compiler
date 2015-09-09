/** 
 * This class traverses a MiniJava Abstract Syntax Tree and prints a
 * representation of the original program.  It adds lots of parentheses
 * to make the precedence clear.
 *
 * @author Brad Richards
 */

package Old;

import minijava.node.*;
import minijava.analysis.DepthFirstAdapter;
import java.io.PrintWriter;
import java.util.*;

public class PrintVisitor extends DepthFirstAdapter {
	private int depth = -2; // Start at -2 so Program->ClassDecl puts back to 0
	private PrintWriter out;

	/** Constructor takes a PrintWriter, and stores in instance var. */
	public PrintVisitor(PrintWriter out) {
		this.out = out;
	}

	private void indent() {
		for (int i = 0; i < depth; i++)
			out.write("   ");
	}

	/**
	 * When descending into a new node, increment the indentation level.
	 * Decrement the level when leaving a node.
	 */
	public void defaultIn(Node node) {
		depth = depth + 1;
	}

	public void defaultOut(Node node) {
		depth = depth - 1;
		out.flush();
	}

	public void caseStart(Start node) {
		inStart(node);
		node.getPProgram().apply(this);
		node.getEOF().apply(this);
		outStart(node);
	}

	public void caseAMainClassDecl(AMainClassDecl node) {
		inAMainClassDecl(node);
		indent();
		out.print("class ");
		depth++;
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.println(" {");
		indent();
		out.println("public static void main(String[] bogus) {");
		if (node.getStmt() != null) {
			node.getStmt().apply(this);
		}
		indent();
		out.println("}");
		depth--;
		indent();
		out.println("}\n");
		outAMainClassDecl(node);
	}

	public void caseABaseClassDecl(ABaseClassDecl node) {
		inABaseClassDecl(node);

		indent();
		out.print("class ");
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.println(" {");
		{
			List<PVarDecl> copy = new ArrayList<PVarDecl>(node.getVarDecl());
			for (PVarDecl e : copy) {
				e.apply(this);
			}
		}
		{
			List<PMethod> copy = new ArrayList<PMethod>(node.getMethod());
			for (PMethod e : copy) {
				e.apply(this);
			}
		}
		indent();
		out.println("}\n");
		outABaseClassDecl(node);
	}

	public void caseASubClassDecl(ASubClassDecl node) {
		inASubClassDecl(node);
		out.print("class ");
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.print(" extends ");
		if (node.getExtends() != null) {
			node.getExtends().apply(this);
			out.print(node.getExtends().getText());
		}
		out.println(" {");
		{
			List<PVarDecl> copy = new ArrayList<PVarDecl>(node.getVarDecl());
			for (PVarDecl e : copy) {
				e.apply(this);
			}
		}
		{
			List<PMethod> copy = new ArrayList<PMethod>(node.getMethod());
			for (PMethod e : copy) {
				e.apply(this);
			}
		}
		out.println("}\n");
		outASubClassDecl(node);
	}

	public void caseAVarDecl(AVarDecl node) {
		inAVarDecl(node);
		indent();
		if (node.getType() != null) {
			node.getType().apply(this);
		}
		out.print(" ");
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.println(";");
		outAVarDecl(node);
	}

	public void caseAMethod(AMethod node) {
		inAMethod(node);
		out.println();
		indent();
		out.print("public ");
		if (node.getType() != null) {
			node.getType().apply(this);
		}
		out.print(" ");
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.print("(");
		{
			List<PFormal> copy = new ArrayList<PFormal>(node.getFormal());
			int len = copy.size();
			for (int i = 0; i < len - 1; i++) {
				copy.get(i).apply(this);
				out.print(", ");
			}
			if (len > 0)
				copy.get(len - 1).apply(this);
		}
		out.println(") {");
		{
			List<PVarDecl> copy = new ArrayList<PVarDecl>(node.getVarDecl());
			for (PVarDecl e : copy) {
				e.apply(this);
			}
		}
		{
			List<PStmt> copy = new ArrayList<PStmt>(node.getStmt());
			for (PStmt e : copy) {
				e.apply(this);
			}
		}
		indent();
		out.println("}");
		outAMethod(node);
	}

	public void caseAFormal(AFormal node) {
		inAFormal(node);
		if (node.getType() != null) {
			node.getType().apply(this);
		}
		out.print(" ");
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		outAFormal(node);
	}

	public void caseAIntType(AIntType node) {
		out.print("int");
	}

	public void caseABoolType(ABoolType node) {
		out.print("bool");
	}

	public void caseAIntArrayType(AIntArrayType node) {
		out.print("int[]");
	}

	public void caseAUserType(AUserType node) {
		inAUserType(node);
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		outAUserType(node);
	}

	public void caseAReturnStmt(AReturnStmt node) {
		inAReturnStmt(node);
		indent();
		out.print("return ");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.println(";");
		outAReturnStmt(node);
	}

	public void caseABlockStmt(ABlockStmt node) {
		inABlockStmt(node);
		indent();
		out.println("{");
		{
			List<PStmt> copy = new ArrayList<PStmt>(node.getStmt());
			for (PStmt e : copy) {
				e.apply(this);
			}
		}
		indent();
		out.println("}");
		outABlockStmt(node);
	}

	public void caseAIfStmt(AIfStmt node) {
		inAIfStmt(node);
		indent();
		out.print("if (");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.println(")");
		if (node.getYes() != null) {
			node.getYes().apply(this);
		}
		indent();
		out.println("else");
		if (node.getNo() != null) {
			node.getNo().apply(this);
		}
		outAIfStmt(node);
	}

	public void caseAWhileStmt(AWhileStmt node) {
		inAWhileStmt(node);
		indent();
		out.print("while (");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.println(")");
		if (node.getStmt() != null) {
			node.getStmt().apply(this);
		}
		outAWhileStmt(node);
	}

	public void caseAPrintStmt(APrintStmt node) {
		inAPrintStmt(node);
		indent();
		out.print("System.out.println(");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.println(");");
		outAPrintStmt(node);
	}

	public void caseAAsmtStmt(AAsmtStmt node) {
		inAAsmtStmt(node);
		indent();
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.print(" = ");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.println(";");
		outAAsmtStmt(node);
	}

	public void caseAArrayAsmtStmt(AArrayAsmtStmt node) {
		inAArrayAsmtStmt(node);
		indent();
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.print("[");
		if (node.getIdx() != null) {
			node.getIdx().apply(this);
		}
		out.print("] = ");
		if (node.getVal() != null) {
			node.getVal().apply(this);
		}
		out.println(";");
		outAArrayAsmtStmt(node);
	}

	public void caseAAndExp(AAndExp node) {
		inAAndExp(node);
		out.print("(");
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}
		out.print(" && ");
		if (node.getRight() != null) {
			node.getRight().apply(this);
		}
		out.print(")");
		outAAndExp(node);
	}

	public void caseALtExp(ALtExp node) {
		inALtExp(node);
		out.print("(");
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}
		out.print(" < ");
		if (node.getRight() != null) {
			node.getRight().apply(this);
		}
		out.print(")");
		outALtExp(node);
	}

	public void caseAPlusExp(APlusExp node) {
		inAPlusExp(node);
		out.print("(");
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}
		out.print(" + ");
		if (node.getRight() != null) {
			node.getRight().apply(this);
		}
		out.print(")");
		outAPlusExp(node);
	}

	public void caseAMinusExp(AMinusExp node) {
		inAMinusExp(node);
		out.print("(");
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}
		out.print(" - ");
		if (node.getRight() != null) {
			node.getRight().apply(this);
		}
		out.print(")");
		outAMinusExp(node);
	}

	public void caseATimesExp(ATimesExp node) {
		inATimesExp(node);
		out.print("(");
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}
		out.print(" * ");
		if (node.getRight() != null) {
			node.getRight().apply(this);
		}
		out.print(")");
		outATimesExp(node);
	}

	public void caseANotExp(ANotExp node) {
		inANotExp(node);
		out.print("!(");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.print(")");
		outANotExp(node);
	}

	public void caseARefExp(ARefExp node) {
		inARefExp(node);
		out.print("(");
		if (node.getName() != null) {
			node.getName().apply(this);
		}
		out.print("[");
		if (node.getIdx() != null) {
			node.getIdx().apply(this);
		}
		out.print("]");
		out.print(")");
		outARefExp(node);
	}

	public void caseALengthExp(ALengthExp node) {
		inALengthExp(node);
		out.print("(");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.print(".length)");
		outALengthExp(node);
	}

	public void caseAMethodExp(AMethodExp node) {
		inAMethodExp(node);
		out.print("(");
		if (node.getObj() != null) {
			node.getObj().apply(this);
		}
		out.print(".");
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.print("(");
		{
			List<PExp> copy = new ArrayList<PExp>(node.getArgs());
			int len = copy.size();
			for (int i = 0; i < len - 1; i++) {
				copy.get(i).apply(this);
				out.print(", ");
			}
			if (len > 0)
				copy.get(len - 1).apply(this);

		}
		out.print(")");
		out.print(")");
		outAMethodExp(node);
	}

	public void caseANumExp(ANumExp node) {
		inANumExp(node);
		if (node.getNum() != null) {
			out.print(node.getNum().getText());
		}
		outANumExp(node);
	}

	public void caseAIdExp(AIdExp node) {
		inAIdExp(node);
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		outAIdExp(node);
	}

	public void caseATrueExp(ATrueExp node) {
		out.print("true");
	}

	public void caseAFalseExp(AFalseExp node) {
		out.print("false");
	}

	public void caseAThisExp(AThisExp node) {
		out.print("this");
	}

	public void caseAAllocExp(AAllocExp node) {
		inAAllocExp(node);
		out.print("(new int[");
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		out.print("])");
		outAAllocExp(node);
	}

	public void caseANewExp(ANewExp node) {
		inANewExp(node);
		out.print("(new ");
		if (node.getId() != null) {
			out.print(node.getId().getText());
		}
		out.print("())");
		outANewExp(node);
	}
}
