package typechecker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import compiler.*;

import minijava.analysis.DepthFirstAdapter;
import minijava.node.*;

/**
 * A type checking class which will traverse our tree and detect problems
 * associated with incorrect types. Upon finding a conflict, this visitor prints
 * the nature of the conflict and the line number it occurred on.
 * 
 * @author Andrew
 *
 */

public class TypeCheckerVisitor extends DepthFirstAdapter {

	String currentClassName = "";
	String currentMethodName = "";
	String currentSubClassName = "";

	String activeType;
	ClassTable table;

	public TypeCheckerVisitor(ClassTable ct) {
		table = ct;
	}

	/**
	 * Helper method used to find an id in the symbol table. Sets activeType
	 * equal to the type if finds for the given varId in the table. If it can't
	 * find the variable, it prints a warning then exits.
	 * 
	 * @param varId
	 *            name of the id
	 */
	public void setIdType(String varId) {

		// maybe there are no local variables

		Set<String> localVarNames = table.get(currentClassName)
				.getMethodTable().get(currentMethodName).getLocals()
				.getVarNames();
		Set<String> classFieldNames = table.get(currentClassName).getVarTable()
				.getVarNames();

		// if it's a local variable
		for (String s : localVarNames) {
			if (varId.equals(s)) {
				activeType = Types.toStr(table.get(currentClassName)
						.getMethodTable().get(currentMethodName).getLocals()
						.get(s));
				// System.out.println(varId +
				// " is a local variable and is of type " + activeType);
				return;
			}
		}

		// if it's a formal
		for (PFormal formal : table.get(currentClassName).getMethodTable()
				.get(currentMethodName).getFormals()) {
			AFormal aFormal = (AFormal) formal; // cast to get access to id
			// if we find ourselves in the formal list
			if (aFormal.getId().getText().equals(varId)) {
				activeType = Types.toStr(aFormal.getType()); // if we find
																// ourselves as
																// a formal use
																// that
				// System.out.println(varId + " is an agrument and is of type "
				// + activeType);
				return;
			}
		}

		// if it's a field
		for (String s : classFieldNames) {
			if (varId.equals(s)) {
				activeType = Types.toStr(table.get(currentClassName)
						.getVarTable().get(s));
				// System.out.println(varId + " is a field and is of type "
				// +activeType);
				return;
			}
		}

		for (ClassInfo inheritedClass : getAllExtendedClasses(table
				.get(currentClassName))) {
			Set<String> keySet = inheritedClass.getVarTable().getVarNames();
			for (String key : keySet)
				// go through all the local variables
				if (varId.equals(key)) // if we find ourselves
				{
					activeType = Types.toStr((inheritedClass.getVarTable()
							.get(key))); // eyy we know the type we are
					// System.out.println(varId
					// + " is an inherited field and is of type "
					// + activeType);
					return;
				}
		}

		System.out.println(varId + " is not declared anywhere in the program");
		System.exit(-1);

	}

	@Override
	public void caseAMainClassDecl(AMainClassDecl node) {
		inAMainClassDecl(node);
		currentClassName = node.getId().getText();
		if (node.getId() != null) {
			node.getId().apply(this);
		}
		if (node.getStmt() != null) {
			node.getStmt().apply(this);
		}
		outAMainClassDecl(node);
	}

	@Override
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

	@Override
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

	@Override
	public void caseAMethod(AMethod node) {
		inAMethod(node);
		if (node.getType() != null) {
			node.getType().apply(this);
		}

		if (node.getId() != null) {
			currentMethodName = node.getId().getText();
			// System.out.println("Current method name is: " +
			// currentMethodName);
			node.getId().apply(this);
		}

		List<PFormal> copy = new ArrayList<PFormal>(node.getFormal());
		for (PFormal e : copy) {
			e.apply(this);
		}

		List<PStmt> stmCopy = new ArrayList<PStmt>(node.getStmt());
		for (PStmt e : stmCopy) {
			// System.out.println(e);
			e.apply(this);
		}

		// we know that the last statement is the return statement.
		if (!activeType.equals(Types.toStr(node.getType()))) {
			System.out.println("Type error on line " + node.getId().getLine());
			System.out
					.println("Method signature return type does not match actual return type.");
			System.exit(-1);
		}

		outAMethod(node);
	}

	@Override
	public void caseAAsmtStmt(AAsmtStmt node) {
		String leftSide = "";
		String rightSide = "derp";

		inAAsmtStmt(node);
		if (node.getId() != null) {
			setIdType(node.getId().getText());
			leftSide = activeType;
		}

		if (node.getExp() != null) {
			node.getExp().apply(this);
			rightSide = activeType;
		}

		if (!(leftSide.equals(rightSide))) {
			System.out.println("Type error on line " + node.getId().getLine());
			System.out.println("Expected type: " + leftSide + " Actual type: "
					+ rightSide);
			System.exit(-1);
		}
		outAAsmtStmt(node);
	}

	@Override
	public void caseAIdExp(AIdExp node) {
		inAIdExp(node);

		if (node.getId() != null) {
			this.setIdType(node.getId().getText());
		}

		outAIdExp(node);
	}

	@Override
	public void caseAPlusExp(APlusExp node) {
		inAPlusExp(node);

		// both the left side and the right side should be of type AIntType.

		if (node.getLeft() != null) {
			node.getLeft().apply(this); // this could lead us to another plus
										// expression or an id look up

			if (!(activeType.equals("int"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Type error");
				System.out.println(activeType
						+ " cannot be used in an addition operation");
				System.exit(-1);
			}

		}
		if (node.getRight() != null) {
			node.getRight().apply(this);

			if (!(activeType.equals("int"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Type error");
				System.out.println(activeType
						+ " cannot be used in an addition operation");
				System.exit(-1);
			}
		}

		activeType = "int";
		outAPlusExp(node);
	}

	@Override
	public void caseAMinusExp(AMinusExp node) {
		inAMinusExp(node);
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
			if (!(activeType.equals("int"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Type error");
				System.out.println(activeType
						+ " cannot be used in a subtraction operation");
				System.exit(-1);
			}
		}
		if (node.getRight() != null) {
			node.getRight().apply(this);
			if (!(activeType.equals("int"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Type error");
				System.out.println(activeType
						+ " cannot be used in a subtraction operation");
				System.exit(-1);
			}
		}

		activeType = "int";

		outAMinusExp(node);
	}

	@Override
	public void caseATimesExp(ATimesExp node) {
		inATimesExp(node);
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
			if (!(activeType.equals("int"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Type error");
				System.out.println(activeType
						+ " cannot be used in a multiplication operation");
				System.exit(-1);
			}
		}
		if (node.getRight() != null) {
			node.getRight().apply(this);
			if (!(activeType.equals("int"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Type error");
				System.out.println(activeType
						+ " cannot be used in a multiplication operation");
				System.exit(-1);
			}
		}
		activeType = "int";
		outATimesExp(node);
	}

	@Override
	public void caseAIfStmt(AIfStmt node) {
		inAIfStmt(node);

		if (node.getExp() != null) {
			node.getExp().apply(this);
			if (!(activeType.equals("boolean"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Type error on line ");
				System.out
						.println("Expecting boolean expression in if statement");
				System.exit(-1);
			}
		}
		if (node.getYes() != null) {
			node.getYes().apply(this);
		}
		if (node.getNo() != null) {
			node.getNo().apply(this);
		}
		outAIfStmt(node);
	}

	@Override
	public void caseAWhileStmt(AWhileStmt node) {
		inAWhileStmt(node);
		// this exp needs to be a boolean
		if (node.getExp() != null) {
			node.getExp().apply(this);
			if (!(activeType.equals("boolean"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Error on line ");
				System.out
						.println("Expecting boolean expression in if statement");
				System.exit(-1);
			}
		}
		if (node.getStmt() != null) {
			node.getStmt().apply(this);
		}
		outAWhileStmt(node);
	}

	@Override
	public void caseAArrayAsmtStmt(AArrayAsmtStmt node) {
		System.out.println("Now in an array assignment statement");
		inAArrayAsmtStmt(node);
		if (node.getId() != null) {
			setIdType(node.getId().getText());
			if (!activeType.equals("int[]")) {
				System.out.println("Type error on line: "
						+ node.getId().getLine());
				System.out.println("Expecting type: int[]" + "Actual type: "
						+ activeType);
				System.exit(-1);
			}
		}
		if (node.getIdx() != null) {
			node.getIdx().apply(this);
			if (!(activeType.equals("int"))) {

				System.out.println("Error on line: " + node.getId().getLine());
				System.out.println("Index must be an interger value");
				System.exit(-1);
			}
		}
		if (node.getVal() != null) {
			node.getVal().apply(this);
		}

		outAArrayAsmtStmt(node);
	}

	@Override
	public void caseAAndExp(AAndExp node) {
		inAAndExp(node);
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
			if (!(activeType.equals("boolean"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Error on line ");
				System.out
						.println("Expecting boolean expression in AND statement");
				System.exit(-1);
			}
		}
		if (node.getRight() != null) {
			node.getRight().apply(this);
			if (!(activeType.equals("boolean"))) {
				/**
				 * Find line number then put it in the print statement below
				 */
				System.out.println("Error on line ");
				System.out
						.println("Expecting boolean expression in AND statement");
				System.exit(-1);
			}
		}
		activeType = "boolean";
		outAAndExp(node);
	}

	@Override
	public void caseALtExp(ALtExp node) {
		inALtExp(node);
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
			if (!(activeType.equals("int"))) {
				System.out.println("Error on line ");
				System.out
						.println("Expecting int on the left side of less than");
				System.exit(-1);
			}
		}
		if (node.getRight() != null) {
			node.getRight().apply(this);
			if (!(activeType.equals("int"))) {
				System.out.println("Error on line ");
				System.out
						.println("Expecting int on the right side of less than");
				System.exit(-1);
			}
		}
		activeType = "boolean";
		outALtExp(node);
	}

	@Override
	public void caseANotExp(ANotExp node) {
		inANotExp(node);
		if (node.getExp() != null) {
			node.getExp().apply(this);
			if (!(activeType.equals("boolean"))) {
				System.out
						.println("Expecting boolean expression for not expression");
				System.exit(-1);
			}
		}
		activeType = "boolean";
		outANotExp(node);
	}

	// [name]:exptwo lbracket [idx]:expthree rbracket
	@Override
	public void caseARefExp(ARefExp node) {
		inARefExp(node);
		if (node.getName() != null) {
			node.getName().apply(this);
			if (!activeType.equals("int[]")) {
				System.out.println("Type error " + node);
				System.out.println("Can only dereference from type int[] ");
				System.out.println("Actual type: " + activeType);
			}
		}
		if (node.getIdx() != null) {
			node.getIdx().apply(this);
			if (!activeType.equals("int")) {
				System.out.println("Type error");
				System.out.println("Index for dereference must be of type int");
				System.out.println("Actual type: " + activeType);
			}
		}
		outARefExp(node);

		activeType = "int";
	}

	@Override
	public void caseALengthExp(ALengthExp node) {
		inALengthExp(node);
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		outALengthExp(node);
		activeType = "int";
	}

	@Override
	public void caseAThisExp(AThisExp node) {
		inAThisExp(node);
		outAThisExp(node);
	}

	@Override
	public void caseAAllocExp(AAllocExp node) {
		inAAllocExp(node);
		if (node.getExp() != null) {
			node.getExp().apply(this);
			if (!(activeType.equals("int"))) {
				System.out
						.println("Expecting integer value in array allocation");
				System.exit(-1);
			}
		}
		activeType = "int[]";
		outAAllocExp(node);
	}

	@Override
	public void caseANewExp(ANewExp node) {
		inANewExp(node);
		if (node.getId() != null) {
			activeType = node.getId().getText();
		}
		outANewExp(node);
	}

	@Override
	public void caseAMethodExp(AMethodExp node) {
		// make sure object is a defined class
		// make sure formals line up
		String oldMethodName = currentMethodName;
		// String curMethodName = node.getId().getText();
		// currentMethodName = node.getId().getText();
		System.out.println("CURRENT METHOD NAME IS " + currentMethodName);

		// int a;
		// a = fac.testAgain();
		// fac is the obj name make sure fac is
		inAMethodExp(node);
		if (node.getObj() != null) {

			node.getObj().apply(this);

			System.out.println(activeType);
			// node.getObj().apply(this);
			Set<String> classNames = table.getClassNames();
			String thisClass = node.getId().getText();
			for (String s : classNames) {

			}
		}
		if (node.getId() != null) {
			// possible method calls are
			node.getId().apply(this);

			// setIdType(node.getId().getText()); // this should set active type
			// equal to the right value.
		}

		List<PExp> copy = new ArrayList<PExp>(node.getArgs());

		// active type is now equal to the type..which if it is a method must
		// be...the class name
		MethodInfo currentMethod = table.get(activeType).getMethodTable()
				.get(node.getId().getText());

		// store the current arguments we found in the method.
		LinkedList<PFormal> currentArgs = currentMethod.getFormals();

		// no point in even checking if they take different number of arguments
		if (currentArgs.size() != copy.size()) {
			System.out.println("Error on line " + node.getId().getLine());
			System.out.println("Number of arguments in method call "
					+ node.getId().getText() + " is incorrect");
		}

		// otherwise we can check them one by one, an iterator is useful here
		// because then we can specify
		// which parameter the error was found on (does java even do that?)
		else {
			// currentArgs.size() and copy.size() are guaranteed to be equal
			// because this is an else statement
			for (int i = 0; i < currentArgs.size(); i++) {
				copy.get(i).apply(this); // should set the active type.
				AFormal temp = (AFormal) (currentArgs.get(i));
				if (!activeType.equals(Types.toStr(temp.getType()))) {
					System.out.println("Error on line "
							+ node.getId().getLine());
					System.out
							.println("Parameter type mismatch error on parameter number "
									+ (i + 1));
					System.out.println("Expecting type: " + activeType
							+ " Actual type " + Types.toStr(temp.getType()));
					System.exit(-1);
				}
			}
		}

		currentMethodName = oldMethodName; // back to the actual method we are
											// in.

		String type = Types.toStr(currentMethod.getRetType());
		activeType = type;
		outAMethodExp(node);
	}

	/**
	 * Our five base cases
	 */

	@Override
	public void caseANumExp(ANumExp node) {
		inANumExp(node);

		if (node.getNum() != null) {
			activeType = Types.toStr(new AIntType());
			node.getNum().apply(this);
		}
		outANumExp(node);
	}

	@Override
	public void caseAIntType(AIntType node) {
		inAIntType(node);
		activeType = Types.toStr(node);
		outAIntType(node);
	}

	@Override
	public void caseABoolType(ABoolType node) {
		inABoolType(node);
		activeType = Types.toStr(node);
		outABoolType(node);
	}

	@Override
	public void caseATrueExp(ATrueExp node) {
		inATrueExp(node);
		activeType = Types.toStr(new ABoolType()); // active type should be a
													// bool type
		outATrueExp(node);
	}

	@Override
	public void caseAFalseExp(AFalseExp node) {
		inAFalseExp(node);
		activeType = Types.toStr(new ABoolType()); // active type should be a
													// bool type
		outAFalseExp(node);
	}

	/**
	 * Helper class which accumulates classes in a given hierarchy
	 * 
	 * @param subClass
	 *            the lowest class in the hierarchy
	 * @return all classes above a given class in the hierarchy
	 */
	private ArrayList<ClassInfo> getAllExtendedClasses(ClassInfo subClass) {
		ArrayList<String> ids = new ArrayList<String>();

		if (subClass.getSuper() == null)
			return new ArrayList<ClassInfo>();

		String id = subClass.getSuper().getText();
		// while there is another super class
		while (id != null) {
			ids.add(id); // add its super class
			TId extendsId = table.get(id).getSuper();// go check to see if that
														// class has a super
														// class.
			if (extendsId == null)
				id = null;
			else
				id = extendsId.getText();
		}

		for (String name : ids)
			System.out.println(name);

		ArrayList<ClassInfo> allExtendedClasses = new ArrayList<ClassInfo>();

		for (String s : ids)
			allExtendedClasses.add(table.get(s));

		return allExtendedClasses;
	}
}