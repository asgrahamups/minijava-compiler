package unusedchecker;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import minijava.node.*;

/** The table maps var names to entries of type Status */
enum Status {
	Maybe, Yes
};

/**
 * An InitTable instance records initialization information on a collection of
 * variables. Variable names are mapped to a Status value &mdash; "Maybe"
 * (possibly initialized), or "Yes" (definitely initialized). There's no need
 * for a "No" entry, as uninitialized variables don't have entries in the table.
 */

public class InitTable {
	HashMap<String, Status> table;

	/** Default constructor builds a blank table */
	public InitTable() {
		table = new HashMap<String, Status>();
	}

	/** Build a new table that's a clone of an existing table */
	public InitTable(InitTable old) {
		table = new HashMap<String, Status>(old.table);
	}

	/**
	 * Build a new table, adding entries for each of the formals in a list of
	 * parameters, indicating they're initialized.
	 */
	public InitTable(LinkedList<PFormal> formals) {
		table = new HashMap<String, Status>();
		for (PFormal f : formals)
			table.put(((AFormal) f).getId().getText(), Status.Yes);
	}

	/**
	 * Add a status entry for a variable. If there's already an entry for this
	 * var, upgrade the status if the new value is more precise than the old.
	 */
	public void put(String var, Status info) {
		Status old = table.get(var);
		if ((old == null) || (old.ordinal() < info.ordinal()))
			table.put(var, info);
	}

	public void clear() {
		table.clear();
	}

	/** Look up the status info on a variable */
	public Status get(String var) {
		return table.get(var);
	}

	/** Remove the entry for the specified var */
	public void remove(String var) {
		table.remove(var);
	}

	/** List all the vars in the table */
	public Set<String> getVarNames() {
		return table.keySet();
	}

	/**
	 * Merge the status information from another table into this one. This
	 * method would be used to incorporate the initialization information
	 * discovered while processing the body of a while loop: Since the loop may
	 * not run, initializations that occur inside the loop should be considered
	 * to be "Maybe" below the loop. Thus, if the input table has an entry for a
	 * var and we have none, we should add a Maybe (assuming that entries will
	 * be Yes or Maybe in the incoming table). If we already have an entry, it's
	 * either Maybe (in which case no action need be taken), or Yes (in which
	 * case no action SHOULD be taken). The put() method is smart enough to add
	 * entries only if they're "better" than the existing info, so it doesn't
	 * hurt to do a put() for each var in the new table.
	 */
	public void mergeWhile(InitTable newInfo) {
		if (newInfo != null) {
			for (String varName : newInfo.getVarNames())
				put(varName, Status.Maybe);
		}
	}

	/**
	 * Merge info from the branches of a conditional. I'll let you do this one.
	 */
	public void mergeIf(InitTable t, InitTable f) {
		if (t != null) {
			for (String name : t.getVarNames()) {
				put(name, Status.Maybe);
			}
		}

		if (f != null) {
			for (String name : t.getVarNames()) {
				put(name, Status.Maybe);
			}
		}
	}
}
