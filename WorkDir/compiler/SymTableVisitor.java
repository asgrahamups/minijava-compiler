package compiler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Set;

import minijava.analysis.DepthFirstAdapter;
import minijava.node.*;
import arch.*;
import tree.*;

/**
 * This visitor class builds a symbol table as it traverses the tree. The table,
 * an instance of ClassTable, can be returned via getTable().
 * 
 * @author Andrew Graham, Adam Reynolds
 */
public class SymTableVisitor extends DepthFirstAdapter {
	String currentClassName = "";
	String currentSubClassName = "";
	String currentMethodName = "";

	String currentIRTNode = "";

	private int stackOffset = 0;
	private ArrayList<HashMap<String, Integer>> memoryStackMaps = new ArrayList<HashMap<String, Integer>>();
	private ClassTable table = new ClassTable();

	/** getTable returns the entire table */
	public ClassTable getTable() {
		return table;
	}

	/**
	 * Dumps all the variables, their respective classes, and their offsets
	 */
	public void dumpOffsets() {
		// for (HashMap<String, Integer> map : memoryStackMaps) {
		// System.out.println("Variables and offsets for class");
		// Set<String> keyset = map.keySet();

		// for (String key : keyset) {
		// String currentVariable = key; // the variable name
		// int offset = map.get(key); // it's offset
		// System.out.println("-------------------------");
		// System.out.println("Variable Name: " + currentVariable);
		// System.out.println("Variable Offset: " + offset);
		// System.out.println("-------------------------");
		// }
		// }
	}

	/**
	 * Maps each local variable and parameter to an offset in memory(for every
	 * class in the table.)
	 */
	public void allocateLocalVariableMemory() {
		Set<String> classKeySet = table.getClassNames();

		// Allocate memory for each class
		for (String key : classKeySet) {
			currentClassName = key; // So our methods can add object references.
			allocateClassMemory(table.get(key));
		}

	}

	private void allocateClassMemory(ClassInfo classInfo) {
		// Allocate memory for each method
		for (String key : classInfo.getMethodTable().getMethodNames()) {
			currentMethodName = key; // so our
			memoryStackMaps.add(allocateLocalMemory(classInfo.getMethodTable()
					.get(key)));
		}

	}

	/**
	 * Allocates memory for a given method's local variables and parameters
	 * 
	 * @param methodInfo
	 */
	private HashMap<String, Integer> allocateLocalMemory(MethodInfo methodInfo) {
		HashMap<String, Integer> localMap = new HashMap<String, Integer>();
		// TODO: Return addresses are currently stored as a string "return" but
		// will need to be IRTified later

		// Store the return addresss
		localMap.put("return", stackOffset);

		stackOffset += 4;
		// TODO: Static links are currently stored as a string "slink" but will
		// need to be IRTified later
		localMap.put("slink", stackOffset);

		stackOffset += 4;
		// parameters
		for (PFormal formal : methodInfo.getFormals()) {
			AFormal form = (AFormal) formal;
			localMap.put(form.getId().getText(), stackOffset);
			stackOffset += 4;
		}

		// local variables
		VarTable localVariables = methodInfo.getLocals();
		Set<String> localKeySet = localVariables.getVarNames();
		for (String key : localKeySet) {
			localMap.put(key, stackOffset);
			stackOffset += 4;
		}

		stackOffset = 0;
		return localMap;
	}

	/**
	 * Checks to see if there are any overridden methods. A method is overridden
	 * if it has the same signature one in a super class but has different
	 * arguments.
	 * 
	 * TODO: Still need to specify which method it overrides in error message.
	 * 
	 * @throws MethodClashException
	 *             if there is overriding throw an error.
	 * @return true if there is method overriding
	 * 
	 */
	public boolean checkOverriding() throws MethodClashException {
		// a keyset we can use to iterate through the class table's classes
		Set<String> keySet = getTable().getClassNames();

		// Store all classes that have a super class
		ArrayList<ClassInfo> subClasses = new ArrayList<ClassInfo>();

		// round up all sub classes
		for (String key : keySet)
			if (getTable().get(key).getSuper() != null) // is a sub class if
														// it's extends text is
														// not null
				subClasses.add(getTable().get(key));

		if (subClasses.size() == 0) // if we have no sub classes there cannot be
									// method overriding
			return false;

		for (ClassInfo subclass : subClasses) {
			ArrayList<ClassInfo> allSuperClasses = getAllExtendedClasses(subclass); // collect
																					// the
																					// entire
																					// hierarchy
																					// of
																					// classes
																					// for
																					// that
																					// sub
																					// clas
			for (ClassInfo superClass : allSuperClasses)
				// compare subclass to all of super classes to check for
				// overriding.
				if (overrideCheckHelper(subclass, superClass)) // if there is
																// overriding
																// between the
																// super and sub
																// class
				{
					// System.out.println("Error on line " +
					// subclass.getName().getLine());
					throw new MethodClashException("Sub class '"
							+ subclass.getName().getText()
							+ "' illegally overrides a method.");
				}
		}

		return false;
	}

	/**
	 * A private helper method to compile a list of all ClassInfos in the
	 * program that a sub class extends
	 * 
	 * @param subclass
	 * @param superClass
	 * @return
	 */
	private ArrayList<ClassInfo> getAllExtendedClasses(ClassInfo subClass) {
		ArrayList<String> ids = new ArrayList<String>();
		String id = subClass.getSuper().getText();
		// while there is another super class
		while (id != null) {
			ids.add(id); // add its super class
			TId extendsId = getTable().get(id).getSuper();// go check to see if
															// that class has a
															// super class.
			if (extendsId == null)
				id = null;
			else
				id = extendsId.getText();
		}

		ArrayList<ClassInfo> allExtendedClasses = new ArrayList<ClassInfo>();

		for (String name : ids)
			allExtendedClasses.add(getTable().get(name));

		return allExtendedClasses;
	}

	/**
	 * Checks for method overriding between two classes.
	 * 
	 * @param subClass
	 * @param superClass
	 * @return true if overriding is found, false if it is not
	 */
	private boolean overrideCheckHelper(ClassInfo subClass, ClassInfo superClass) {
		// Save some time by
		MethodTable superMethodTable = superClass.getMethodTable();
		MethodTable subMethodTable = subClass.getMethodTable();

		for (String name : superMethodTable.getMethodNames()) {
			MethodInfo activeMethod = superMethodTable.get(name); // the super
																	// class
																	// method we
																	// are
																	// looking
																	// at
			String activeMethodName = activeMethod.getName().getText();
			for (String subname : subMethodTable.getMethodNames()) {
				MethodInfo activeSubMethod = subMethodTable.get(subname); // sub
																			// class
																			// method
																			// to
																			// look
																			// at
				String activeSubName = activeSubMethod.getName().getText();

				// if they have the same name, different arguments and same
				// return type
				if (activeSubName.equals(activeMethodName)) {
					if (sameType(activeMethod, activeSubMethod)) {
						if (!sameArgs(activeMethod, activeSubMethod)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * A helper function to check for equality between the return types of
	 * MethodInfo
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	private boolean sameType(MethodInfo first, MethodInfo second) {
		String firstType = Types.toStr(first.getRetType());
		String secondType = Types.toStr(second.getRetType());

		return firstType.equals(secondType);
	}

	/**
	 * A helper method to detect if two MethodInfo's contain the same arguments.
	 * 
	 * @param first
	 * @param second
	 * @return
	 */
	private boolean sameArgs(MethodInfo first, MethodInfo second) {

		LinkedList<PFormal> firstForm = first.getFormals();
		LinkedList<PFormal> secondForm = second.getFormals();

		// if they both have no arguments then their arguments are the same.
		// This saves time and memory because then we don't actually have to
		// inspect each one
		if (firstForm.size() == 0 && secondForm.size() == 0) {
			System.out.println("Both methods have no arguments.");
			return true;
		}

		else {
			// Some temp linked lists we can use to store a formals when we cast
			// from p formals
			LinkedList<AFormal> firstAFormalList = new LinkedList<AFormal>();
			LinkedList<AFormal> secondAFormalList = new LinkedList<AFormal>();

			// Cast to AFormals for both lists.
			for (PFormal formal : firstForm)
				firstAFormalList.add((AFormal) formal);
			for (PFormal formal : secondForm)
				secondAFormalList.add((AFormal) formal);

			// Loop through formal list and look for differences
			for (AFormal formal : firstAFormalList)
				for (AFormal secondFormal : secondAFormalList)
					if (!sameFormal(formal, secondFormal))
						return false;
		}
		return true;
	}

	/**
	 * A helper method to determine if two AFormals are equal.
	 * 
	 * @param first
	 * @param second
	 * @return true if they are equal.
	 */
	private boolean sameFormal(AFormal first, AFormal second) {
		// Attributes of formals we can use to deduce equality
		String firstType = Types.toStr(first.getType());
		String secondType = Types.toStr(second.getType());

		return firstType.equals(secondType);
	}

	/**
	 * Handles the main class declaration, because it only has one statement, we
	 * can just evaluate it and be done.
	 * 
	 * @param a
	 *            main class node
	 */
	public void caseAMainClassDecl(AMainClassDecl node) {
		currentClassName = node.getId().getText();
		inAMainClassDecl(node);

		// So we can pass the main class id to the put method in class table.

		// go forth and apply thine statements!
		if (node.getStmt() != null)
			node.getStmt().apply(this);

		try {
			getTable().putMain(node.getId().getText(), "main"); // here is
																// incorrect
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// so we need to somehow get the method name from the statement..

		outAMainClassDecl(node);
		currentClassName = "";
	}

	@Override
	public void caseABaseClassDecl(ABaseClassDecl node) {
		inABaseClassDecl(node);

		// set class name so we know where to add methods
		if (node.getId() != null) {
			currentClassName = node.getId().getText();

		}

		// copy of the fields
		LinkedList<PVarDecl> copy = new LinkedList<PVarDecl>(node.getVarDecl());
		// copy of methods
		LinkedList<PMethod> methodCopy = new LinkedList<PMethod>(
				node.getMethod());

		try {
			getTable().put(node.getId(), null, copy, methodCopy);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (PVarDecl e : copy) {
			e.apply(this);
		}

		for (PMethod e : methodCopy) {
			e.apply(this);
		}

		outABaseClassDecl(node);
		currentClassName = "";
	}

	@Override
	public void caseASubClassDecl(ASubClassDecl node) {
		inASubClassDecl(node);

		currentSubClassName = node.getExtends().getText();
		// set class name so we know where to add methods

		if (node.getId() != null) {
			currentClassName = node.getId().getText();

		}

		// copy of the fields
		LinkedList<PVarDecl> copy = new LinkedList<PVarDecl>(node.getVarDecl());
		// copy of methods
		LinkedList<PMethod> methodCopy = new LinkedList<PMethod>(
				node.getMethod());

		try {
			getTable().put(node.getId(), node.getExtends(), copy, methodCopy);

		} catch (Exception e) {
			System.exit(-1);
		}

		for (PVarDecl e : copy) {
			e.apply(this);
		}

		for (PMethod e : methodCopy) {
			e.apply(this);
		}

		currentSubClassName = "";
		currentClassName = "";
		outASubClassDecl(node);
	}
}