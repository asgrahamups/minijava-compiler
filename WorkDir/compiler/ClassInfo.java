package compiler;

import tree.*;
import arch.*;

import java.util.ArrayList;
import java.util.LinkedList;

import minijava.node.PMethod;
import minijava.node.PVarDecl;
import minijava.node.TId;

//import Mips.MipsArch;  // These two are needed for the IRT phase
//import Arch.*;

//global IRT variable
/**
 * A ClassInfo instance records infomation about a single class. It stores the
 * name of its superclass (or null if there isn't one), a VarTable containing
 * the class's instance variables, and a MethodTable containing information on
 * the methods in the class, in addition to the name of the class itself.
 * 
 * @author Brad Richards
 */

public class ClassInfo {
	
	TId className; // TId holding our name, line number, etc.
	TId superClass; // Our superclass, if we have one

	VarTable vars; // A VarTable holding info on all instance vars
	MethodTable methods; // Table of info on methods
	ESEQ fieldStorage;

	int classOffset;

	/*
	 * 
	 * We'll add these once we get to the IRT phase. The IRTinfo object records
	 * the total number of words required for the instance variables in a class
	 * (including those we inherit).
	 */

	ClassIRTinfo info;

	public ClassIRTinfo getIRTinfo() {
		return info;
	}

	public void setIRTinfo(ClassIRTinfo i) {
		info = i;
	}

	public ESEQ getConstructorFragment() {
		return fieldStorage;
	}

	public int getOffset() {
		return classOffset;
	}

	/**
	 * The constructor takes all info associated with a subclass definition, but
	 * can be passed null for unused fields in the case of a base or main class
	 * declaration. Names are passed as TId rather than String so we can
	 * retrieve line number, etc, from the token if necessary.
	 * 
	 * @param className
	 *            The name of the class
	 * @param superClass
	 *            The name of its superclass
	 * @param vars
	 *            A list of all instance vars in the class
	 * @param methods
	 *            A list of method descriptors
	 */
	public ClassInfo(TId className, TId superClass, LinkedList<PVarDecl> vars,
			LinkedList<PMethod> methods) throws Exception {

		this.className = className;
		this.superClass = superClass;

		// make accessors for these
		this.vars = new VarTable(vars); // Populate table from list
		this.methods = new MethodTable(methods); // Ditto.
		info = new ClassIRTinfo(this.vars, classOffset);
		fieldStorage = constructorMem(getClassVarBytes(), new Reg("base"));
	}

	public void doneOffset() {
		// System.out.println("Offset - 4: " + (classOffset - 4));
		//this.info = new ClassIRTinfo(this.vars, classOffset);
		dumpIRT(false);
	}

	public void setClassOffset(int n) {
		classOffset = n;
	}

	public TId getName() {
		return className;
	}

	public TId getSuper() {
		return superClass;
	}

	public VarTable getVarTable() {
		return vars;
	}

	public MethodTable getMethodTable() {
		return methods;
	}

	public void dump() {
		// System.out.print("Class:" + className.getText());
		// if (superClass != (null))
		// System.out.print(" Extends:" + superClass.getText());
		// System.out.println();
		// System.out.println("------------");

		// if(methods.getMethodNames().size()==0)
		// System.out.println(className.getText()+ " has no methods");
		//
		// if(vars.getVarNames().size()==0)
		// System.out.println(className.getText()+ " has no fields");
		//
		// if(!(vars.getVarNames().size()==0))
		// {
		// System.out.println(className.getText() +
		// " has the following fields");
		vars.dump();
		// }
		//
		// if(!(methods.getMethodNames().size()==0))
		// {
		// System.out.println(className.getText() +
		// " has the following methods");
		methods.dump();
		// }

		// Signify the start of a new class
		// dumpIRT(false);
		// System.out.println("----------");
	}

	public MOVE fieldIRT(int offset, Reg param) {
		return new MOVE(new MEM(new BINOP(BINOP.PLUS, new REG(param),
				new CONST(offset * Access.WORD_SIZE))), new CONST(0));
	}

	public ESEQ constructorMem(int numVars, Reg param) {
		if (numVars == 0) {
			return new ESEQ(new MOVE(new REG(param), new CONST(0)), new REG(
					param));
		}
		// System.out.println("Now entering constructorMem");
		// System.out.println(numVars);

		// System.out.println(Access.WORD_SIZE);
		return new ESEQ(generateIRT(numVars * Access.WORD_SIZE, 0, param),
				new REG(param));
	}

	public Access getField(String name) {
		return this.info.get(name);
	}

	public MOVE allocateMem(int bytes, Reg dest) {
		return new MOVE(new REG(dest), new CALL(new NAME(new Label("malloc")),
				new ExpList(new CONST(bytes/4), null)));

	}

	public MOVE allocateFieldMem(int offset, Reg dest) {
		return new MOVE(new MEM(new BINOP(BINOP.PLUS, new REG(dest), new CONST(
				offset))), new CONST(0));
	}
	
	public MEM accessMem(int offset, Reg dest)
	{
		return new MEM(new BINOP(BINOP.PLUS, new REG(dest),
				new CONST(offset)));
	}

	public SEQ generateIRT(int bytes, int start, Reg dest) {
		
		
		SEQ malloc = new SEQ(allocateMem(bytes, dest), allocateFieldMem(start,
				dest));
		
		SEQ temp = malloc;

		ArrayList<String> varNames = new ArrayList<String>();
		
		
		for(String s: this.vars.getVarNames())
			varNames.add(s);
		
		
		int accessStart = start;
		
		while(accessStart < varNames.size())
		{
			String key = varNames.get(accessStart);
			AccessLocal currentAccess = new AccessLocal(accessStart*4);
			info.accessMap().put(key, currentAccess);
			accessStart++;
		}

		while (start < ((bytes/4)-4)) 
		{
			start += 4;
			temp = new SEQ(temp, allocateFieldMem(start, dest));
		}	
		
		return temp;
	}

	public void dumpIRT(boolean dot) {
		// System.out.println("Accessors for class variables: ");
		// System.out.println("Total var bytes for this class: "
		// + getClassVarBytes());
		// System.out.println("I'm in class: " + className.getText());

		// int locals = getVarTable().getVarNames().size();
		// int pass = getAdjustedVarCount();
		// System.out.println("Simulated: " + pass);

		// System.out.println("Begin constructor: ");
		// tree.Print.prExp(constructorMem(pass, new Reg("base")));
	}

	private int getAdjustedVarCount() {
		// we already know where we're starting, in bytes

		return vars.size();
	}

	private int getClassVarBytes() {
		// we already know where we're starting, in bytes

		return vars.size() * Access.WORD_SIZE;
	}
}
