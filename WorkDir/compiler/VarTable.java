package compiler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import arch.Access;
import arch.AccessLocal;
import minijava.node.AVarDecl;
import minijava.node.PType;
import minijava.node.PVarDecl;
import minijava.node.TId;

/**
 * A VarTable records name and type information about a <i>collection</i> of
 * variables. An exception is thrown if we try to add a duplicate name.
 * 
 * @author Brad Richards
 */
public class VarTable {
	HashMap<String, VarInfo> table = new HashMap<String, VarInfo>();

	/**
	 * Constructor populates table from an initial list of VarDecls.
	 * 
	 * @param vars
	 *            A list of PVarDecl nodes from our AST.
	 */
	public VarTable(LinkedList<PVarDecl> vars) throws VarClashException {
		for (PVarDecl var : vars) {
			AVarDecl temp = ((AVarDecl) var);
			try {
				put(temp.getId(), temp.getType());
			} catch (VarClashException e) {
			}
		}
	}

	public void put(TId id, PType type) throws VarClashException {
		String name = id.getText();

		if (table.containsKey(name)) {
			System.out.println("Error on line " + id.getLine());
			throw new VarClashException("Duplicate variable names detected, "
					+ name);
		} else {
			VarInfo temp = new VarInfo(type);
			// int offset = 0;
			// //Get the offset
			// Access access = new AccessLocal(offset);
			// temp.setAccess(access);
			table.put(name, temp); // No clash; add new binding
		}
	}

	/** Lookup and return the type of a variable */
	public PType get(String name) {
		return table.get(name).getType(); // So things will compile for now...
	}

	/** Lookup and return a variable's VarInfo record */
	public VarInfo getInfo(String name) {
		return table.get(name);
	}

	/** Return all var names in the table */
	public Set<String> getVarNames() {
		return table.keySet();
	}

	/** Returns the number of entries in the table */
	public int size() {
		return table.size();
	}

	/** Print out the entire contents of the table */
	public void dump() {
		for (String key : getVarNames()) {
			System.out.print("        ");
			System.out.println(key + ":"
					+ Types.toStr(table.get(key).getType()));
		}
	}

	public void dumpIRT(boolean dot) {
		// TODO Fill in the guts of this method -- but not until the IRT
		// checkpoint
	}
}
