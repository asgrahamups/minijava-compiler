package unusedchecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import compiler.*;

import minijava.analysis.DepthFirstAdapter;
import minijava.node.*;
import minijava.parser.*;

public class UnusedVarCheckerVisitor extends DepthFirstAdapter {
	ClassTable table;
	String currentClassName;
	String currentMethodName;

	// we'll want these as fields for accessibility
	InitTable straightlineVariableTable, activeVariableTable;

	public UnusedVarCheckerVisitor(ClassTable table) {
		this.table = table;
		straightlineVariableTable = new InitTable();

		// this is to enable switching b/t the above 2
		activeVariableTable = straightlineVariableTable;
	}

	public void caseABaseClassDecl(ABaseClassDecl node) {
		inABaseClassDecl(node);
		if (node.getId() != null) {

			currentClassName = node.getId().getText();
			// System.out.println("Entering the base class: " +
			// currentClassName);
			node.getId().apply(this);
		}

		// loop through the methods
		for (PMethod e : node.getMethod()) {
			e.apply(this);
		}

		List<PVarDecl> copy = new ArrayList<PVarDecl>(node.getVarDecl());
		for (PVarDecl e : copy) {
			e.apply(this);
		}

		currentClassName = ""; // reset the current class name
		outABaseClassDecl(node);
	}

	public void caseASubClassDecl(ASubClassDecl node) {
		inASubClassDecl(node);
		currentClassName = node.getId().getText();
		if (node.getId() != null) {
			node.getId().apply(this);
		}
		if (node.getExtends() != null) {
			node.getExtends().apply(this);
		}
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
		outASubClassDecl(node);
	}

	public void caseAMethod(AMethod node) {
		inAMethod(node);
		currentMethodName = node.getId().getText();

		MethodInfo current = table.get(currentClassName).getMethodTable()
				.get((currentMethodName));

		// we need to do this to keep the process going
		List<PFormal> copy = new ArrayList<PFormal>(node.getFormal());
		for (PFormal e : copy) {
			e.apply(this);
		}

		List<PVarDecl> varDeclcopy = new ArrayList<PVarDecl>(node.getVarDecl());
		for (PVarDecl e : varDeclcopy) {
			e.apply(this);
		}

		// reach the statements to actually check for str. initialization
		List<PStmt> stmCopy = new ArrayList<PStmt>(node.getStmt());
		for (PStmt e : stmCopy) {
			e.apply(this);
		}

		Set<String> entries = activeVariableTable.getVarNames();
		// examine the straightline initialization table
		for (String entry : entries) {
			// it might not have been init. ed
			if (activeVariableTable.get(entry) == Status.Maybe) {
				System.err.println("Warning: Maybe uninitialized variable: "
						+ entry + " in method: " + currentMethodName);
			}
		}

		// check for unused variables
		Set<String> locals = table.get(currentClassName).getMethodTable()
				.get(currentMethodName).getLocals().getVarNames();
		for (String local : locals) {
			if (!entries.contains(local)) {
				System.out.println("Error: unused variable: " + local
						+ " in method: " + currentMethodName);
				System.exit(1);
			}
		}

		activeVariableTable.clear();

		currentMethodName = "";
		outAMethod(node);
	}

	public void caseAFormal(AFormal node) {
		inAFormal(node);

		activeVariableTable.put(node.getId().getText(), Status.Yes);

		outAFormal(node);
	}

	public void caseAVarDecl(AVarDecl node) {
		inAVarDecl(node);

		// System.out.println("Var Decl on line: " + node.getId().getLine() +
		// "and column: " + node.getId().getPos());

		outAVarDecl(node);
	}

	public void caseAAsmtStmt(AAsmtStmt node) {
		inAAsmtStmt(node);

		// better safe than sorry
		if (node.getExp() != null) {
			activeVariableTable.put(node.getId().getText(), Status.Yes);
		}

		outAAsmtStmt(node);
	}

	public void caseAIfStmt(AIfStmt node) {
		inAIfStmt(node);

		InitTable yesVariableTable = new InitTable(activeVariableTable);
		InitTable noVariableTable = new InitTable(activeVariableTable);

		node.getExp().apply(this);

		activeVariableTable = yesVariableTable;

		if (node.getYes() != null) {
			node.getYes().apply(this);
		}

		activeVariableTable = noVariableTable;

		if (node.getNo() != null) {
			node.getNo().apply(this);
		}

		activeVariableTable = straightlineVariableTable;
		activeVariableTable.mergeIf(yesVariableTable, noVariableTable);

		inAIfStmt(node);
	}

	public void caseAWhileStmt(AWhileStmt node) {
		inAWhileStmt(node);

		InitTable whileTable = new InitTable(activeVariableTable);
		node.getExp().apply(this);

		activeVariableTable = whileTable;

		if (node.getStmt() != null) {
			node.getStmt().apply(this);
		}

		activeVariableTable = straightlineVariableTable;
		activeVariableTable.mergeWhile(whileTable);

		outAWhileStmt(node);
	}

	public void caseAIdExp(AIdExp node) {
		inAIdExp(node);

		// System.out.println(currentClassName);
		// System.out.println(currentMethodName);
		// System.out.println(node.getId().getText());

		Set<String> locals = table.get(currentClassName).getMethodTable()
				.get(currentMethodName).getLocals().getVarNames();

		if (activeVariableTable.get(node.getId().getText()) == null) {
			if (!locals.contains(node.getId().getText())) {
				System.out.println("Warning: Maybe uninitialized variable "
						+ node.getId().getText() + " on line "
						+ node.getId().getLine());
				return;
			}

			System.out.println("Error: Unrecognized variable "
					+ node.getId().getText() + " on line "
					+ node.getId().getLine());
			System.out
					.println("\tThis error may be thrown by the use of an uninitialized local variable");
			System.exit(1);
		}

		outAIdExp(node);
	}
}