package irt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import compiler.*;
import minijava.node.*;
import tree.*;
import arch.*;
import minijava.analysis.DepthFirstAdapter;

/**
 * TODO: We only ever assign a label to the last class involved.
 */

/**
 * Generates the IRT for the programs fed to the Compiler. Assume that methods
 * not overridden remain the same as in DepthFirstAdapter
 */

public class IRTVisitor extends DepthFirstAdapter {
	private String currentClassName = "";
	private String currentMethodName = "";
	private String previousClassName = "";
	private String currentSubName = "";
	private ArrayList<REG> regInUse = new ArrayList<REG>();
	private ArrayList<Stm> blockStatements = new ArrayList<Stm>();
	private ArrayList<Stm> programStatements = new ArrayList<Stm>();
	
	private int currentLine;
	
	private Reg boolResultStore;

	private Access currentAccess;
	
	public MOVE currentMove;
	private Exp currentExp;
	private Stm currentStm;
	private Stm mainStm;
	public Stm irtRepresentation;

	private ClassTable table;

	public IRTVisitor(ClassTable classTable) {
		table = classTable;

	}
	
	
	/**
	 * Returns a list of registers that the IRT program has decided to use.
	 * @return
	 */
	public ArrayList<REG> getRegInUse()
	{
		return regInUse;
	}

	public void caseAProgram(AProgram node) {
		inAProgram(node);

		List<PClassDecl> copy = new ArrayList<PClassDecl>(node.getClassDecl());
		
		LABEL label = new LABEL(new Label("main"));

		copy.remove(0).apply(this);
		
		Stm programStm = new SEQ(label, mainStm);


		for (PClassDecl e : copy) {
			e.apply(this);
			//programStm = new SEQ(currentStm, programStm);
		}
		
		for(Stm s: programStatements)
		{
			programStm = new SEQ(s,programStm);
		}

//		PrintDot.prStm(programStm);
//		System.exit(0);
		
		outAProgram(node);
		irtRepresentation = programStm;
	}
	
	@Override
	public void caseABaseClassDecl(ABaseClassDecl node) 
	{
		inABaseClassDecl(node);

		currentClassName = node.getId().getText();

		LABEL baseLabel = new LABEL(new Label(node.getId().getText()));

		List<PMethod> copy = new ArrayList<PMethod>(node.getMethod());
		
		

		copy.remove(0).apply(this);
		
		SEQ methods = new SEQ(baseLabel, currentStm);
		//SEQ methods;
		
		//Stm current = currentStm;
		programStatements.add(currentStm);
		
	
		
		

		for (PMethod e : copy) {
			e.apply(this);
			programStatements.add(currentStm);
			methods = new SEQ(currentStm,methods);
		}

		currentStm = methods;

		outABaseClassDecl(node);
	}

	public void caseASubClassDecl(ASubClassDecl node) {
		inASubClassDecl(node);

		currentClassName = node.getId().getText();

		currentSubName = node.getExtends().getText();

		COMMENT baseLabel = new COMMENT(node.getId().getText());

		if (node.getId() != null) {
			node.getId().apply(this);
		}

		if (node.getExtends() != null) {
			node.getExtends().apply(this);
		}

		List<PMethod> copyM = new ArrayList<PMethod>(node.getMethod());

		copyM.remove(0).apply(this);

		SEQ methods = new SEQ(baseLabel, currentStm);

		for (PMethod e : copyM) {
			e.apply(this);
			
			methods = new SEQ(methods, currentStm);
		}

		currentStm = methods;

		outASubClassDecl(node);
	}

	@Override
	public void caseAMainClassDecl(AMainClassDecl node) 
	{
		inAMainClassDecl(node);

		currentClassName = node.getId().getText();

		if (node.getId() != null) {
			node.getId().apply(this);
		}

		if (node.getStmt() != null) {
			node.getStmt().apply(this);
		}
		// currentStm = new SEQ(mainLabel, currentStm);

		mainStm = currentStm;

		currentClassName = "";

		outAMainClassDecl(node);
	}

	int methodNumber = 0;

	@Override
	public void caseAMethod(AMethod node) {
		inAMethod(node);

		// So whoever called us who needed a label can get one
		currentMethodName = node.getId().getText();

		LABEL label = new LABEL(new Label(currentClassName + "."
				+ currentMethodName));
		
		COMMENT comment = new COMMENT("");

		List<PStmt> copy = new ArrayList<PStmt>(node.getStmt());

		PStmt returnStmt = copy.get(0);
	
		copy.remove(0);
		returnStmt.apply(this);
		//Print.prStm(currentStm);
		
		//System.exit(0);

		SEQ methodSequence = new SEQ(comment, currentStm);

		for (PStmt e : copy) {
			e.apply(this); // set irt fragment to something
			methodSequence = new SEQ(methodSequence, currentStm);
		}
		
		methodSequence = new SEQ(label,methodSequence);

		currentStm = methodSequence;
		currentMethodName = "";

		outAMethod(node);
	}

	@Override
	public void caseANewExp(ANewExp node) {
		inANewExp(node);
		
		if (node.getId() != null) {
			node.getId().apply(this);
		}
		
		ClassInfo newClass = table.get(node.getId().getText());

		previousClassName = currentClassName;

		currentClassName = node.getId().getText();

		currentExp = newClass.constructorMem(newClass.getVarTable().size()*Access.WORD_SIZE,
				new Reg("$gp"));
		
		//Reg.reset();

		outANewExp(node);
	}

	@Override
	public void caseAAsmtStmt(AAsmtStmt node) {
		inAAsmtStmt(node);

		if (node.getId() != null)
			node.getId().apply(this);

		Exp mem = currentExp;
		
		int line = currentLine;

		if (node.getExp() != null)
			node.getExp().apply(this);

		// System.out.println("Storing");
		// Print.prExp(currentExp);
		// System.out.println("Into " + node.getId().getText() +
		// ", which is located in");
		// Print.prExp(mem);
		COMMENT comment = new COMMENT(node.getId().getText() + "=" + "[line " + line + "]");
		
		currentStm = new SEQ(comment, new MOVE(mem, currentExp));

		outAAsmtStmt(node);
	}

	@Override
	public void caseAAllocExp(AAllocExp node) {
		inAAllocExp(node);
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}
		
		//System.out.println("allocating...");

		Exp indexExp = currentExp;

		REG tempReg = new REG(new Reg());
		
		//used for the allocator in MIPS visitor to see which registers are available
		if(!regInUse.contains(tempReg))
			regInUse.add(tempReg);

		MOVE mallocMove = new MOVE(tempReg, new CALL(new NAME(new Label(
				"malloc")), new ExpList(new BINOP(BINOP.MUL, new BINOP(
				BINOP.PLUS, currentExp,new CONST(1)), new CONST(
				Access.WORD_SIZE)), null)));

		MOVE putLength = new MOVE(new MEM(tempReg), indexExp);

		currentExp = new ESEQ(new SEQ(mallocMove, putLength), tempReg);
		
		Reg.reset();

		outAAllocExp(node);
	}

	public void caseAArrayAsmtStmt(AArrayAsmtStmt node) {

		inAArrayAsmtStmt(node);
		if (node.getId() != null) {
			node.getId().apply(this);
		}

		Exp mem = currentExp;

		if (node.getIdx() != null) {
			node.getIdx().apply(this);
		}

		Exp indexExp = currentExp;

		if (node.getVal() != null) {
			node.getVal().apply(this);
		}

		BINOP byteOffset = new BINOP(BINOP.PLUS, indexExp, new CONST(
				1));
		
		BINOP byteAndSizeOffset = new BINOP(BINOP.MUL, byteOffset,
				new CONST(4)); // first entry in array is its length
		
		
		mem = new MEM(new BINOP(BINOP.PLUS, mem, byteAndSizeOffset));
		
		MOVE move = new MOVE(mem, currentExp);

		currentStm = move;

		outAArrayAsmtStmt(node);
	}

	public void caseAPlusExp(APlusExp node) {
		inAPlusExp(node);

		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}

		Exp left = currentExp;

		if (node.getRight() != null) {
			node.getRight().apply(this);
		}

		Exp right = currentExp;

		currentExp = new BINOP(BINOP.PLUS, left, right);

		outAPlusExp(node);
	}

	@Override
	public void caseAMinusExp(AMinusExp node) {
		inAMinusExp(node);
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}

		Exp left = currentExp;

		if (node.getRight() != null) {
			node.getRight().apply(this);
		}

		Exp right = currentExp;

		currentExp = new BINOP(BINOP.MINUS, left, right);

		outAMinusExp(node);
	}

	@Override
	public void caseATimesExp(ATimesExp node) {
		inATimesExp(node);
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}

		Exp left = currentExp;

		if (node.getRight() != null) {
			node.getRight().apply(this);
		}

		Exp right = currentExp;

		currentExp = new BINOP(BINOP.MUL, left, right);

		outATimesExp(node);
	}

	@Override
	public void caseARefExp(ARefExp node) {
		// should return a MEM
		inARefExp(node);
		if (node.getName() != null) {
			node.getName().apply(this);
		}
		Exp name = currentExp;

		if (node.getIdx() != null) {
			node.getIdx().apply(this);
		}
		Exp index = currentExp;

		BINOP memoryOffset = new BINOP(BINOP.PLUS, index, new CONST(1)); // add
																			// account
																			// for
																			// size
																			// being
																			// the
																			// first
																			// entry
																			// of
																			// an
																			// array
		BINOP indexOffset = new BINOP(BINOP.MUL, memoryOffset, new CONST(
				Access.WORD_SIZE));

		currentExp = new MEM(new BINOP(BINOP.PLUS, indexOffset, name));

		outARefExp(node);
	}

	@Override
	public void caseALengthExp(ALengthExp node) {
		// grab the first thing out of an array
		inALengthExp(node);

		if (node.getExp() != null) {
			node.getExp().apply(this);
		}

		currentExp = new MEM(currentExp);

		outALengthExp(node);
	}

	@Override
	public void caseAPrintStmt(APrintStmt node) {
		inAPrintStmt(node);
		NAME name = new NAME(new Label("print"));

		if (node.getExp() != null) {
			node.getExp().apply(this);
		}

		currentExp = new CALL(name, new ExpList(currentExp, null));
		currentStm = new EXPR(currentExp);
		outAPrintStmt(node);
	}

	@Override
	public void caseAThisExp(AThisExp node) {
		inAThisExp(node);
		int offset = table.get(currentClassName).getOffset();
		//System.out.println(offset);
		currentExp = new ESEQ(new COMMENT("this"),new MEM(new BINOP(BINOP.PLUS, new REG(new Reg("$sp")),
				new CONST(offset))));
		outAThisExp(node);
	}

	@Override
	public void caseAMethodExp(AMethodExp node) {
		inAMethodExp(node);

		CALL finalCall;

		if (node.getObj() != null) {
			node.getObj().apply(this);
		}

		Exp memLocation = currentExp;

		if (node.getId() != null) {
			node.getId().apply(this);
		}

		// name of the method as an IRT object
		NAME methodCall = new NAME(new Label(currentClassName + "."
				+ node.getId().getText()));

		List<PExp> copy = new ArrayList<PExp>(node.getArgs());

		ArrayList<PExp> arrayCopy = new ArrayList<PExp>();

		for (PExp e : copy) {
			arrayCopy.add(e);
		}

		// No args
		if (copy.size() == 0) {
			finalCall = new CALL(methodCall, new ExpList(memLocation, null));
			currentExp = finalCall;
		}
		// One arg
		else if (copy.size() == 1) {
			for (PExp e : copy) {
				e.apply(this);
			}
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(currentExp, null)));
			currentExp = finalCall;
		} else if (copy.size() == 2) {
			arrayCopy.get(0).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(currentExp, null)));
			Exp previousExp = currentExp;
			arrayCopy.get(1).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(previousExp, new ExpList(currentExp, null))));
			currentExp = finalCall;
		} else if (copy.size() == 3) {
			arrayCopy.get(0).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(currentExp, null)));
			Exp previousExpOne = currentExp;
			arrayCopy.get(1).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(previousExpOne, new ExpList(currentExp, null))));
			Exp previousExpTwo = currentExp;
			arrayCopy.get(2).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(previousExpOne, new ExpList(previousExpTwo,
							new ExpList(currentExp, null)))));
			currentExp = finalCall;
		} else if (copy.size() == 4) {
			arrayCopy.get(0).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(currentExp, null)));
			Exp previousExpOne = currentExp;
			arrayCopy.get(1).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(previousExpOne, new ExpList(currentExp, null))));
			Exp previousExpTwo = currentExp;
			arrayCopy.get(2).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(previousExpOne, new ExpList(previousExpTwo,
							new ExpList(currentExp, null)))));
			Exp previousExpThree = currentExp;
			arrayCopy.get(3).apply(this);
			finalCall = new CALL(methodCall, new ExpList(memLocation,
					new ExpList(previousExpOne, new ExpList(previousExpTwo,
							new ExpList(previousExpThree, new ExpList(
									currentExp, null))))));
			currentExp = finalCall;
		} else {
			System.out
					.println("Error: More than four arguments passed to a method call, exiting...");
			System.exit(0);
		}

		outAMethodExp(node);
	}

	@Override
	public void caseAReturnStmt(AReturnStmt node) {
		inAReturnStmt(node);

		if (node.getExp() != null) {
			node.getExp().apply(this);
		}

		// extract the return stm's expression (since it's been evaled above)
		currentStm = new RETURN(currentExp);

		outAReturnStmt(node);
	}

	@Override
	public void caseALtExp(ALtExp node) {
		inALtExp(node);

		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}

		Exp left = currentExp;
		Label leftLabel = new Label();

		if (node.getRight() != null) {
			node.getRight().apply(this);
		}

		Exp right = currentExp;
		Label rightLabel = new Label();

		CJUMP lessThan = new CJUMP(CJUMP.LT, left, right, leftLabel, rightLabel);

		// used by both moveFalse and moveTrue
		Label done = new Label();
		JUMP jumpToEnd = new JUMP(done);
		Reg reg = new Reg();
		boolResultStore = reg;
		
		if(!regInUse.contains(reg))
			regInUse.add(new REG(reg));
		

		// sequence for false
		MOVE putZero = new MOVE(new REG(reg), new CONST(0));
		SEQ moveFalse = new SEQ(putZero, jumpToEnd);

		// sequence for true
		MOVE putOne = new MOVE(new REG(reg), new CONST(1));
		SEQ moveTrue = new SEQ(putOne, jumpToEnd);

		// stitching the LT together
		SEQ connectTrue = new SEQ(new LABEL(leftLabel), moveTrue);
		SEQ connectFalse = new SEQ(new LABEL(rightLabel), moveFalse);

		SEQ topSeq = new SEQ(connectFalse,
				new SEQ(connectTrue, new LABEL(done)));
		
		//REVERT THIS BACK TO currentStm = new SEQ(lessThan, topSeq);
		currentExp = new ESEQ(new SEQ(lessThan, topSeq),new REG(reg));  
		
//		PrintDot.prExp(currentExp);
//		System.exit(0);
		
		Reg.reset();

		outALtExp(node);
	}
	
	public void caseAAndExp(AAndExp node) 
	{
		inAAndExp(node);
		
		if (node.getLeft() != null) {
			node.getLeft().apply(this);
		}
		
		Exp left = currentExp;
		
		
		if (node.getRight() != null) {
			node.getRight().apply(this);
		}
		
		Exp right = currentExp;
		
		currentExp =  new BINOP(BINOP.AND,left,right);		
		
		
		outAAndExp(node);
	}
	
	

	@Override
	public void caseAIfStmt(AIfStmt node) {
		inAIfStmt(node);
		
		Label trueLabel = new Label();
		Label falseLabel = new Label();
		Label done = new Label();
		
		if (node.getExp() != null) {
			node.getExp().apply(this);
		}

		// holds the CJUMP for the boolean expression
		// made in the eval. of the if condition
		Exp boolExp = currentExp;

		// takes us to the true if block
		if (node.getYes() != null) {
			node.getYes().apply(this);
		}

		
		
		if(blockStatements.size() == 0)
			blockStatements.add(currentStm);
		
		SEQ yesSeq = new SEQ(new COMMENT("Block statement"),blockStatements.remove(0));
		
		//recurse and make even more statements!
		for(Stm s: blockStatements)
			yesSeq = new SEQ(yesSeq, s);
		
		blockStatements.clear();
		
		// takes us to the false if block
		if (node.getNo() != null) {
			node.getNo().apply(this);
		}

		

		
		JUMP jumpDone = new JUMP(done);
		
		if(blockStatements.size() == 0)
			blockStatements.add(currentStm);
		
		SEQ noSeq = new SEQ(new COMMENT("Block statement"),blockStatements.remove(0));
		
		//recurse and make even more statements!
		for(Stm s: blockStatements)
			noSeq = new SEQ(noSeq, s);
		
		blockStatements.clear();

		
		//Note: if I broke it, just change yesSeq back to yes and noSeq back to no

		SEQ trueBranch = new SEQ(new LABEL(trueLabel), new SEQ(yesSeq, jumpDone));

		SEQ falseBranch = new SEQ(new LABEL(falseLabel), new SEQ(noSeq, jumpDone));
//
//		SEQ trueAndFalseBranch = new SEQ(trueBranch, new SEQ(falseBranch,
//				new LABEL(done)));
		SEQ trueAndFalseBranch = new SEQ(trueBranch, new SEQ(falseBranch,
				new LABEL(done)));

		// comment cjump should be linked to ifJump here
		CJUMP ifJump = new CJUMP(CJUMP.EQ,
				boolExp, new CONST(1), trueLabel,
				falseLabel);

		currentStm = new SEQ(ifJump, trueAndFalseBranch);
		
		

		outAIfStmt(node);
	}

	@Override
	public void caseAForStmt(AForStmt node) {
		inAForStmt(node);

		Label done = new Label();
		Label fakey = new Label();
		Label forLabel = new Label();

		if(node.getInst() != null) {
			node.getInst().apply(this);
		}
		
		if(node.getExp() != null) {
			node.getExp().apply(this);
		}

		Exp stopCondition = currentExp;

		// we need both of these to register before we do anything
		if(node.getIncr() != null) {
			node.getIncr().apply(this);
		}

		Stm loopIncr = currentStm;
		
		if(node.getBod() != null) {
			node.getBod().apply(this);
		}

		// most of this is inherited from while loops
		if(blockStatements.size() == 0)
			blockStatements.add(currentStm);
		
		SEQ statements = new SEQ(new COMMENT("Block statement"),blockStatements.remove(0));
		
		//recurse and make even more statements!
		for(Stm s: blockStatements)
			statements = new SEQ(statements, s);

		// add the increment condition to the end of the block statements
		// this effectively mimics while IRT tree with a for loop >.>
		statements = new SEQ(statements, loopIncr);
	
		// correct me if I'm wrong, but I think we need this
		// duplicate labels since we're faking an "if" structure
		JUMP jumpDone = new JUMP(done);

		// we need both labels to do this successfully, as per above
		SEQ forStm = new SEQ(new LABEL(fakey), new SEQ(statements, jumpDone));
		
		SEQ cForStm = new SEQ(forStm, new LABEL(forLabel));
		
		// comment cjump should be linked to ifJump here
		CJUMP forJump = new CJUMP(CJUMP.EQ,
				stopCondition, new CONST(0), forLabel,
				fakey);
		
		SEQ forWithDone = new SEQ(new LABEL(done), forJump);
		
		currentStm = new SEQ(forWithDone, cForStm);
		
		blockStatements.clear();

		outAForStmt(node);
	}

	@Override
	public void caseAWhileStmt(AWhileStmt node) {
		inAWhileStmt(node);
		
		Label done = new Label();
		Label fakey = new Label();
		Label whileLabel = new Label();

		if (node.getExp() != null) {
			node.getExp().apply(this);
		}

		Exp boolExp = currentExp;
		
		if (node.getStmt() != null) {
			node.getStmt().apply(this);
		}
		
		//if we aren't a block statement
		if(blockStatements.size() == 0)
			blockStatements.add(currentStm);
		
		SEQ statements = new SEQ(new COMMENT("Block statement"),blockStatements.remove(0));
		
		//recurse and make even more statements!
		for(Stm s: blockStatements)
			statements = new SEQ(statements, s);
	
		// correct me if I'm wrong, but I think we need this
		JUMP jumpDone = new JUMP(done);

		// we need both labels to do this successfully
		SEQ whileStm = new SEQ(new LABEL(fakey), new SEQ(statements, jumpDone));
		
		SEQ cWhileStm = new SEQ(whileStm, new LABEL(whileLabel));
		
		// comment cjump should be linked to ifJump here
		CJUMP whileJump = new CJUMP(CJUMP.EQ,
				boolExp, new CONST(0), whileLabel,
				fakey);
		
		SEQ whileWithDone = new SEQ(new LABEL(done),whileJump);
		
		currentStm = new SEQ(whileWithDone, cWhileStm);
		
		blockStatements.clear();
		
		outAWhileStmt(node);
	}
	
	@Override
	public void caseABlockStmt(ABlockStmt node)
	{
		inABlockStmt(node);
		{
			ArrayList<PStmt> copy = new ArrayList<PStmt>(node.getStmt());
			for(PStmt e : copy)
			{
				e.apply(this);
				blockStatements.add(currentStm);
			}
		}
		outABlockStmt(node);
	}

	@Override
	public void caseATrueExp(ATrueExp node) {
		inATrueExp(node);

		currentExp = new CONST(1); 

		outATrueExp(node);
	}

	@Override
	public void caseAFalseExp(AFalseExp node) {
		inAFalseExp(node);

		currentExp = new CONST(0);

		outAFalseExp(node);
	}

	/**
	 * TOKENS
	 */

	public void caseTId(TId node) 
	{
		// setup for checking class vars
		currentLine = node.getLine();
		String idName = node.getText();
		ClassInfo currentClass = table.get(currentClassName);
		VarTable classVars = currentClass.getVarTable();
		Set<String> classVarNames = classVars.getVarNames();

		// check in table
		if (classVarNames.contains(idName)
				&& !currentClass.getMethodTable().getMethodNames()
						.contains(idName)) 
		{
			
			//set currentExp to a MEM access of the field
			AccessLocal accessObject = table.get(currentClassName).getIRTinfo().accessMap().get(idName);
			int readOffset = accessObject.getOffset();
			Exp accessFieldMem = accessObject.generateBsIrt();
			Exp readField = new MEM(new BINOP(BINOP.PLUS,accessFieldMem,new CONST(readOffset)));
			
			currentExp = readField;
		}

		// if it wasn't an instance var, it must be local
		else if (!currentMethodName.equals("")) {
			try {
				currentAccess = table.get(currentClassName).getMethodTable()
						.get(currentMethodName).getInfo()
						.getAccess(node.getText());
				
				//COMMENT local = new COMMENT("Accessing local around line " + node.getLine());
				
				currentExp = currentAccess.getTree();
				
				

			} catch (Exception e) {
				 //e.printStackTrace();
			}
		}

		defaultCase(node);
	}

	@Override
	public void caseTNum(TNum node) {
		currentExp = new CONST(Integer.parseInt(node.getText()));
		defaultCase(node);
	}
	
}
