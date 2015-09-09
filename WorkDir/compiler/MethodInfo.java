package compiler;

import java.util.LinkedList;

import tree.*;

import minijava.node.AFormal;
import minijava.node.AVarDecl;
import minijava.node.PFormal;
import minijava.node.PType;
import minijava.node.PVarDecl;
import minijava.node.TId;

/**
 * A MethodInfo instance records information about a single MiniJava method. It
 * contains references to the method's return type, formal parameters, and its
 * local variables, in addition to the method's name.
 * 
 * @author Brad Richards
 */

public class MethodInfo {
	// ClassInfo parent;
	private PType retType;
	private TId name;
	private LinkedList<PFormal> formals;
	private VarTable locals; // Contains entries for parameters as well
	private ClassInfo enclosing; // The class in which method is actually
									// defined
	int offset = 0;

	/*
	 * Stuff we'll add for the IRT phase
	 */
	public MethodIRTinfo info;

	public MethodIRTinfo getInfo() {
		return info;
	}

	public void setInfo(MethodIRTinfo i) {
		info = i;
	}

	/**
	 * The constructor stores away references to the return type and formals,
	 * and builds a VarTable containing both the local variables and the
	 * formals. If variable name clashes are found (within locals, formals, or
	 * across locals and formals) we throw a VarClashException.
	 * 
	 * @param retType
	 *            The method's return type
	 * @param name
	 *            The method's name (a TId, not a String)
	 * @param formals
	 *            A list of the method's formal variables (params)
	 * @param locals
	 *            A list of the method's local variables
	 */
	public MethodInfo(PType retType, TId name, LinkedList<PFormal> formals,
			LinkedList<PVarDecl> locals) throws VarClashException {
		// TODO Fill in the guts of this method.
		this.retType = retType;
		this.name = name;

		LinkedList<PFormal> temp = new LinkedList<PFormal>();
		// make an access object for each formal
		// make an access object for each local

		for (PFormal formal : formals) {
			try {
				boolean someboolean = true;
				String idname = ((AFormal) formal).toString();
				for (PFormal formalTwo : temp) {
					if (((AFormal) formalTwo).toString().equals(idname)) {
						someboolean = false;
					}
				}
				if (someboolean) {
					temp.add(formal);

				} else {
					System.out.println("Error on line " + name.getLine());
					throw new VarClashException("Duplicate arguments detected");
				}
			} catch (VarClashException e) {
			}
		}

		this.formals = temp;

		// iterate through the formals and add access objects to them, between
		// their name and a new Access object
		// we set each of the access object's offsets

		this.locals = new VarTable(locals);

		for (PVarDecl var : locals) {
			AVarDecl avar = (AVarDecl) var;

			for (PFormal formal : formals) {
				String idname = ((AFormal) formal).getId().toString();
				if (avar.getId().toString().equals(idname)) {
					System.out.println("Error on line " + name.getLine());
					throw new VarClashException(
							"Parameter and local variable share same identifier");
				}
			}
		}

		/**
		 * Store IRT info. After we have done some symbol table checking.
		 */
		this.info = new MethodIRTinfo(temp, this.locals);

		dumpIRT(false);

	}

	/* Accessors */
	public TId getName() {
		return name;
	}

	public PType getRetType() {
		return retType;
	}

	public LinkedList<PFormal> getFormals() {
		return formals;
	}

	public VarTable getLocals() {
		return locals;
	}

	/**
	 * Print info about the return type, formals, and local variables. It's OK
	 * if the formals appear in the local table as well. In fact, it's a
	 * <i>good</i> thing since this output will help us debug later if
	 * necessary, and we'll want to see exactly what's in the VarTable.
	 */
	public void dump() {

		// iter through ll

		System.out.print(name.getText() + ":");

		for (PFormal form : formals) {
			System.out.print("(" + ((AFormal) form).getId().getText() + ":"
					+ Types.toStr(((AFormal) form).getType()) + ")");

			// System.out.println("formal type is " +form.toString())
		}

		System.out.print("  " + Types.toStr(retType));
		if (locals.size() != 0) {
			// System.out.println();
			locals.dump();
		}
		// else
		// System.out.println(name.getText() + " has no local variables");

		// System.out.println();
	}

	/**
	 * Create a new access object for locals and variables
	 * 
	 * @param dot
	 */
	public void dumpIRT(boolean dot) {
		// System.out.println("---------------");
		// System.out.println("Method Name: " + name.getText());
		// System.out.println("Accessors for parameters: ");

		// for (String key : this.info.getArgs().keySet()) {
		// tree.Print.prExp(info.getArgs().get(key).getTree());
		// }

		// System.out.println("Accessors for locals: ");
		// for (String key : this.info.getLocals().keySet()) {
		// tree.Print.prExp(info.getLocals().get(key).getTree());
		// }

	}
}
