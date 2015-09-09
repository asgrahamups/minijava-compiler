package compiler;

import java.util.HashMap;
import java.util.LinkedList;

import minijava.node.AFormal;
import minijava.node.AVarDecl;
import minijava.node.PFormal;
import arch.Access;
import arch.AccessLocal;

/**
 * An object to represent the necessary data to store IRT information.
 * 
 * @author Andrew
 *
 */
public class MethodIRTinfo {
	// Stores the parameters
	HashMap<String, Access> argumentMap = new HashMap<String, Access>();
	// Stores the local variables
	HashMap<String, Access> localMap = new HashMap<String, Access>();
	HashMap<String, Access> combinedMap = new HashMap<String, Access>();
	int staticlink = 0;
	int returnadress = 4;
	int startoffset = 8;

	public MethodIRTinfo(LinkedList<PFormal> formals, VarTable locals) {
		if (formals.size() > 4) {
			System.out
					.println("More than four parameters detected in program. Exiting...");
			System.exit(0);
		}
		// loop through the formals of the method, and store them in the
		// argument map
		for (PFormal formal : formals) {

			AFormal aform = (AFormal) formal;
			Access local = new AccessLocal(startoffset);
			combinedMap.put(aform.getId().getText(), local); // map the local to
																// it's offset

			startoffset += 4;
		}
		// same for the locals
		for (String id : locals.getVarNames()) {
			Access local = new AccessLocal(startoffset);
			combinedMap.put(id, local);
			startoffset += 4;
		}

		// this.buildAllMap();
	}

	// Get the argument map
	public HashMap<String, Access> getArgs() {
		return argumentMap;
	}

	/**
	 * fills a general map that has access to both parameters and locals
	 */
	public void buildAllMap() {
		for (String s : argumentMap.keySet())
			combinedMap.put(s, argumentMap.get(s));

		for (String s : localMap.keySet())
			combinedMap.put(s, localMap.get(s));

	}

	// get the local map
	public HashMap<String, Access> getLocals() {
		return localMap;
	}

	public Access getAccess(String id) {
		return combinedMap.get(id);
	}

}
