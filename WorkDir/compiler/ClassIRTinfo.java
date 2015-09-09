package compiler;

import java.util.HashMap;
import java.util.LinkedList;

import minijava.node.PVarDecl;
import arch.Access;
import arch.AccessClassVariable;
import arch.AccessLocal;

public class ClassIRTinfo 
{
	HashMap<String, Access> instanceVarMap = new HashMap<String, Access>();
	HashMap<String, AccessLocal> varMap;
	int numWords = 0;
	int startOffset = 0;

	// all fields should access $sp 4 and then call mem on it

	public ClassIRTinfo(VarTable instanceVars, int startOffset) {
		// if we need to pass offset info from parent classes
		this.startOffset = startOffset;
		varMap = new HashMap<String, AccessLocal>();

		for (String name : instanceVars.getVarNames()) {
			AccessLocal var = new AccessLocal(startOffset);
			instanceVarMap.put(name, var);
			startOffset += 4;
		}

	}
	
	public HashMap<String, AccessLocal> accessMap()
	{
		return varMap;
	}
	public AccessLocal getFromNewMap(String id)
	{
		return varMap.get(id);
	}
	
	public void addToNewMap(String id, AccessLocal access)
	{
		varMap.put(id, access);
	}

	public int getNumWords() {
		return numWords;
	}

	public void setNumWords(int numWords) {
		this.numWords = numWords;
	}

	public HashMap<String, Access> getMap() {
		return instanceVarMap;
	}

	public void map(String key, Access access) {
		instanceVarMap.put(key, access);
	}

	public Access get(String id) {
		return instanceVarMap.get(id);
	}
}
