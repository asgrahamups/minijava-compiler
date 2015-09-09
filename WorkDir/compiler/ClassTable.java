package compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import arch.Access;
import minijava.node.PMethod;
import minijava.node.PVarDecl;
import minijava.node.TId;
import minijava.node.AMethod;

/**
 * A ClassTable records information about a COLLECTION of class definitions.
 * 
 * This is the heart of the symbol table - this, passed as a param, can
 * represent the symbol table for an entire program.
 */
public class ClassTable {
	HashMap<String, ClassInfo> table = new HashMap<String, ClassInfo>();
	int baseOffset = 0;

	/**
	 * This method adds a new table entry for class "id". It will throw a
	 * ClassClashException if the new class name is already in the table, and
	 * might also pass along Var or Method clash exceptions encountered while
	 * processing the lists of instance variables and methods. (This method
	 * doesn't inspect the lists, but the constructor for ClassInfo does.)
	 * 
	 * @param id
	 *            The name of the class (a TId, not a String)
	 * @param extendsId
	 *            The name of its superclass (or null)
	 * @param vars
	 *            A list of the class's instance variables
	 * @param methods
	 *            A list of the methods in the class
	 */
	public void put(TId id, TId extendsId, LinkedList<PVarDecl> vars,
			LinkedList<PMethod> methods) throws Exception {

		String name = id.getText(); // name here is used as the key to get at
									// our class table

		// Throw an error with an understandable error message for two classes
		// with the same name.
		if (this.table.containsKey(name)) {
			System.out.println("Error on line " + id.getLine());
			throw new ClassClashException(name
					+ " class has been defined more than once");
		} else {
			if (extendsId == null) {
				try {
					// use initial offset of 0 (start of malloc region) if there
					// wasn't a super class
					ClassInfo cons = new ClassInfo(id, null, vars, methods);
					cons.setClassOffset(baseOffset);
					baseOffset += 4;
					table.put(name, cons);
					cons.doneOffset();
				} catch (ClassClashException e) {
				}
			} else {
				try {
					// count superclasses if there was one - there may be more
					// up the chain
					ClassInfo cons = new ClassInfo(id, extendsId, vars, methods);

					// maintain count of vars in all super classes
					int runningOffset = 0;
					ArrayList<ClassInfo> supers = getAllExtendedClasses(cons);
					for (ClassInfo s : supers) {
						// count each variable in each super class
						runningOffset += s.getVarTable().getVarNames().size();
					}

					cons.setClassOffset(runningOffset * Access.WORD_SIZE);
					table.put(name, cons);
					cons.doneOffset();
				} catch (ClassClashException e) {
				}
			}
		}
	}

	public void putMain(String className, String methodName) throws Exception {
		// Make a list so we can put our one method into it
		LinkedList<PMethod> mainMethod = new LinkedList<PMethod>();

		// Make our method we want to give our table
		AMethod tempA = new AMethod();
		tempA.setId(new TId(methodName));

		// Cast the method to agree with our linked list type
		PMethod temp = (PMethod) (tempA);
		mainMethod.add(temp);

		// Put in a class where most everything is null, because main classes in
		// minijava are sort of faked.
		table.put(className, new ClassInfo(new TId(className), null,
				new LinkedList<PVarDecl>(), mainMethod));

		baseOffset += 4;
	}

	/** Lookup and return the ClassInfo record for the specified class */
	public ClassInfo get(String id) {
		return table.get(id);
	}

	/** Return all method names in the table */
	public Set<String> getClassNames() {
		return table.keySet();
	}

	/** dump prints info on each of the classes in the table */
	public void dump() {
		// System.out.println("Inside class table dump method");

		Set<String> keys = table.keySet();
		// Tell all the classes to dump.
		for (String key : keys) {
			ClassInfo temp = table.get(key);
			temp.dump();
		}
	}

	/**
	 * dump prints info on each of the classes in the table and displays IRT
	 * info as well.
	 * 
	 * @param dot
	 *            Are we generating output for dot?
	 */
	public void dumpIRT(boolean dot) {
		// TODO Fill in the guts of this method -- but not until the IRT
		// checkpoint.
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

		// for (String name : ids)
		// System.out.println(name);

		ArrayList<ClassInfo> allExtendedClasses = new ArrayList<ClassInfo>();

		for (String s : ids)
			allExtendedClasses.add(table.get(s));

		return allExtendedClasses;
	}
}
