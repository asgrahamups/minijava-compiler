package irt;
import tree.*;

/**
 * THE TASK:
 * Process the method bodies (DONT PRINT EVERYTHING JUST STORE IT UP)
 * -add registers to an arraylist which the label hashes to
 * -Whenever you come to a call node
 */
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import compiler.*;

import java.util.Scanner;

import arch.Reg;

public class MipsVisitor
{
	static final char newLine = '\n';
	static final String indent = "	";
	ClassTable table;

	boolean moveSource;

	boolean isReturn = false;

	//Set true when a 'CALL' expression has a 'MEM' as an argument.
	//We can do some futher digging to see if we are extracting 
	//The stack pointer.
	boolean extractThisFromStack;
	boolean innerMemRead = true;
	boolean buildingMethod;


	String headerFile ="";

	//String we build up throughout the program.
	String code = "";
	String constImmediate = "";
	String currentLabel = "";
	String currentName = "";
	String memWord = "";
	String moveString = "";
	String stackExtract = "";
	String currentComment = "";


	String buildMethod = "";

	REG currentReg;
	REG returnReg;
	Allocator allocator;
	MipsGUI gui;
	ArrayList<String> labels = new ArrayList<String>();
	ArrayList<REG> inUse = new ArrayList<REG>();
	HashMap<String, ArrayList<REG>> methodRegs;
	HashMap<String, String> methodLabelToMIPSCode;


	private void newLine(String msg)
	{
		if(!buildingMethod)
			code += newLine + msg;
	}

	private void writeCode(String msg)
	{
		code += newLine + msg;
	}

	private void sameLine(String msg)
	{
		code += msg;
	}

	private void buildMethod(String msg)
	{
		if(buildingMethod)
			buildMethod +=  msg + newLine;
	}


	public MipsVisitor(Stm start, ArrayList<REG> regInUse,String headerFilePath, ClassTable table)
	{
		headerFile = "";
		this.table = table;
		methodRegs = new HashMap<String, ArrayList<REG>>();
		methodLabelToMIPSCode = new HashMap<String, String>();

		try
		{
			parseHeader(new File(headerFilePath));
		}
		catch(Exception e)
		{
			System.out.println("Could not find file path");
			System.exit(0);
		}

		//adds the header file to the beginning of the code file.
		code+=headerFile;

		ArrayList<REG> singletonRegs = new ArrayList<REG>();

		for(REG reg: regInUse)
			if(!singletonRegs.contains(reg))
				singletonRegs.add(reg);

		allocator = new Allocator(singletonRegs);
				gui = new MipsGUI(allocator, true);
				gui.setTickRate(0);

		visit(start);
		System.out.println("#----FINAL MIPS OUTPUT -----");

		// we know that main is always guaranteed, or there can be no compilation
		// therefore, we can do this
		// our ABI guarantees that the RA is at the "top" of the stack
		code += "\n" + indent + "lw $ra, 0($sp)";
		code += "\n" + indent + "jr $ra";
		System.out.println(code);

	}

	/**
	 * ----------Statements-------------
	 **/
	public void caseSeq(SEQ node)
	{

		if(node.left != null)
			visit(node.left);

		if(node.right != null)
			visit(node.right);

		gui.tick(allocator);

	}

	public void caseReturn(RETURN node)
	{	


		isReturn = true;//This will set mem to store to v0

		currentReg = new REG(new Reg("$v0"));

		if(node.ret!=null)
			visit(node.ret);

		isReturn = false;

		this.methodLabelToMIPSCode.put(currentLabel, buildMethod);

		String methodCode = "";

		methodCode+=currentLabel+":"+ newLine;
		methodCode+=indent + "#Prologue"+ newLine;

		String className = parseClassNameFromLabel(currentLabel);
		String methodName = parseMethodNameFromLabel(currentLabel);

		ClassInfo info = table.get(className);
		MethodInfo methodInfo = info.getMethodTable().get(methodName);

		ArrayList<REG> tempRegs = methodRegs.get(currentLabel);//data structure to hold the list of local variables
		ArrayList<REG> added = new ArrayList<REG>();

		for(REG r: tempRegs)
		{
			if(!added.contains(r) && !allocator.isAnAReg(r))
				added.add(r);
		}

		int parameterBytes = (methodInfo.getFormals().size()+methodInfo.getLocals().size() + added.size())*4+8;

		/**
		 * Set up calls to store the return address, the static link, and a call to allocate a new stack frame
		 */

		String allocateStackMem = indent + "addi $sp, $sp, -" + parameterBytes + newLine;	
		String setupCall = indent + "sw $ra, 0($sp)"+ "\n"+
				indent + "sw $gp, 4($sp)"+newLine;

		methodCode+=allocateStackMem;
		methodCode+=setupCall;

		int formals = 0, formalsEnd = ((methodInfo.getFormals().size()))*4;
		//int bytesRequired? Calls a previous pass through the tree to see how many registers we need for a given call?

		int stackPosition = 8;

		//tell them which locals they need here?


		/**
		 * We need to store all of the local variables we will need
		 */

		ArrayList<REG> args = new ArrayList<REG>(); //data structure to hold the list of arguments


		if(formalsEnd!=0)
		{
			while(formals<formalsEnd)
			{
				REG arg = allocator.requestAReg();
				args.add(arg);
				methodCode+=indent + "sw " + regString(arg) + ", " + stackPosition + "($sp)"+ newLine;
				stackPosition+=4;
				formals+=4;
			}
			for(REG arg: args)
			{
				allocator.restore(arg);
			}
		}

		stackPosition += methodInfo.getLocals().size()*4; //this is the *skip* to reserve space for local variables in the method

		/**
		 * Reserve space for all registers we think we will need
		 */

		for(REG r: added)
		{
			methodCode+=indent + "sw " + regString(r) + ", " + stackPosition + "($sp)" + newLine;
			stackPosition+=4;
		}


		String midCode = this.methodLabelToMIPSCode.get(currentLabel);


		methodCode+= "\n" + midCode;

		//System.out.println(methodCode);

		/**
		 * Setup Epilogue
		 * -Restore all of the registers we used (those exist in added)
		 */

		methodCode+=indent + "#Epilogue"+ newLine;

		stackPosition -= added.size()*4;

		for(REG r: added)
		{
			methodCode+=indent + "sw " + regString(r) + ", " + stackPosition + "($sp)" + newLine;
			stackPosition+=4;
		}		
		methodCode+=indent + "lw $ra, 0($sp)" + newLine;
		methodCode+=indent + "addi $sp, $sp, " + parameterBytes+ newLine;
		methodCode+=indent + "jr $ra"+ newLine;
		methodCode+=indent + "#End of Epilogue"+ newLine;

		this.methodLabelToMIPSCode.put(currentLabel, methodCode);
		this.buildMethod = "";

		for(REG reg: inUse)
		{
			allocator.restore(reg);
		}

		inUse.clear();


		gui.tick(allocator);
	}

	public void caseComment(COMMENT node)
	{
		if(node.text.equals(""))
			return;

		buildMethod(indent + "#");
		buildMethod(indent + "# " + node.text);
		buildMethod(indent + "#");

		currentComment = node.text;

		gui.tick(allocator);
	}

	public void caseExpr(EXPR node)
	{


		if(node.exp != null)
			visit(node.exp);



		gui.tick(allocator);
	}

	public void caseLabel(LABEL node)
	{
		currentLabel = node.label.toString();

		if(currentLabel.equals("main"))
		{
			buildingMethod = false;

			/**
			 * Before we hit main, we can just print out all the labels and their associated code.
			 */

			for(String label : labels)
			{
				String labelAndCode = methodLabelToMIPSCode.get(label);
				writeCode(labelAndCode);
			}

			for(REG reg: allocator.getAvailable())
			{
				if(allocator.isAnAReg(reg))
				{
					allocator.restore(reg);
				}
			}


			newLine("main:");
			newLine(indent + "sw $ra 0($sp)");
		}
		else
		{
			labels.add(currentLabel);
			buildingMethod = true;
			methodRegs.put(currentLabel,new ArrayList<REG>());
		}

		gui.tick(allocator);
	}

	/*
	 * Jumps
	 */

	public void caseCJump(CJUMP node)
	{	

		//node.iffalse;
		//node.iftrue;
		//node.relop;


		if(node.left!=null)
			visit(node.right);

		if(node.right != null)
			visit(node.right);	

		gui.tick(allocator);
	}

	public void caseJump(JUMP node)
	{
		String temp;

		if(node.exp !=null)
			visit(node.exp);

		temp =indent + "j " + currentLabel;

		code+=temp;

		gui.tick(allocator);
	}

	//probably make two flavors of call
	//One that deals with calls within main and one that handles "in method" calls
	public void caseCall(CALL node)
	{	
		//sets currentName equal to the call's name

		if(node.func != null) //this changes the current name
			visit(node.func);	

		String name = currentName;

		ExpList temp = node.args;
		Exp head = temp.head;

		if(head instanceof CALL)
		{
			newLine(indent + "#Setting up call to " + currentName + " (nested)");
			buildMethod(indent + "jal " + name);

			visit(head);

			newLine(indent + "jal " + name);
			buildMethod(indent + "jal " + name);
			newLine(indent + "#Done with call to " + name);
			buildMethod(indent + "#Done with call to " + name);

			currentReg = new REG(new Reg("$v0"));

			return;
		}

		buildMethod(indent + "#Setting up call to " + currentName);

		ArrayList<REG> args = new ArrayList<REG>();
		ArrayList<Exp> doAfter = new ArrayList<Exp>();

		while(head != null)
		{ 	
			if(head instanceof ESEQ)
			{
				doAfter.add(head);
				if(temp.tail==null)
				{
					break;
				}
				else
				{
					head = temp.tail.head;
					temp = temp.tail;
				}
			}
			else
			{
				currentReg = allocator.requestAReg();
				inUse.add(currentReg);
				visit(head);

				args.add(currentReg);

				if(temp.tail == null)
					break;

				head = temp.tail.head;
				temp = temp.tail;
			}
		}

		for(Exp exp: doAfter)
		{
			if(exp instanceof ESEQ)
			{
				visit(exp);
			}
		}
		
		//restore a registers
		for(REG reg : inUse)
		{
			//if(allocator.isAnAReg(reg))
				allocator.restore(reg);	
		}
		newLine(indent + "jal " + name);
		buildMethod(indent + "jal " + name);
		newLine(indent + "#Done with call to " + name);
		buildMethod(indent + "#Done with call to " + name);


		if(!name.equals("print") && !name.equals("malloc") && currentLabel.equals("main"))
		{
			REG param = allocator.requestAReg();
			newLine(indent + "move " + regString(param) + ", $v0");
		}

		currentReg = new REG(new Reg("$v0"));


		gui.tick(allocator);
	}

	public void caseMove(MOVE node)
	{
		//sets the interpreter to do a load word instead of store word for all mem calls
		moveSource = true;
		
		if(node.src != null)
			visit(node.src);

		REG source = currentReg;	

		moveSource = false;

		for(REG reg: inUse)
		{
			if(!equals(reg,currentReg))
				allocator.restore(reg);
		}
		
		if(node.dst != null)
			visit(node.dst);

		REG dest = currentReg;

		String regSource = regString(source);
		String regDest = regString(dest);

		String saveWord = indent + "sw "+ regSource  + " 0("+regDest+")";	
		buildMethod(saveWord);

		/**
		 * Clean up fields that control the compiler, and restore any temporary regs we used
		 */
		for(REG reg: inUse)
		{
			allocator.restore(reg);
		}
		moveSource = true;
		

		gui.tick(allocator);
	}

	/**
	 * ----------Expressions-------------
	 **/
	boolean onAMission = false;
	boolean inMEM = false;
	public void caseMem(MEM node)
	{
		String loadWord = "";
		REG called = currentReg;
		
		if(node.exp instanceof BINOP)
		{
			onAMission = true;
			visit(node.exp);
			onAMission = false;
		}
		
		//If someone told us where to store it

		if(node.exp != null)
			visit(node.exp);
		
		String reg = regString(currentReg);
		
		//IF WE ARE EVER PASSED ANYTHING EVER USE THIS CONDITIONAL
		if(called!=null)
		{
			if(allocator.isAnAReg(called))
			{
				String aString = regString(called);
				loadWord = indent + "lw " +  aString + ", " +"0("+ reg +")";
			}
			else if(isReturn)
			{
				loadWord = indent + "lw $v0" + "," + "0(" + reg + ")";
				buildMethod(loadWord);
				return;
			}
			else if(inMEM)
			{
				loadWord = indent + "lw " + reg + " , " + "0(" + reg + ")";
				buildMethod(loadWord);
				inMEM = false;
				allocator.markUnavailable(currentReg);
				return;
			}
			else
			{
				loadWord = indent + "lw " +  reg + ", " +"0("+ reg +")";	
			}
		}

		else
		{
			loadWord = indent + "lw " + reg + "," + "0(" + reg + ")";
		}
		


		if(moveSource)
			buildMethod(loadWord);
		
	
		allocator.markUnavailable(currentReg);

	}
	

	public void caseBinop(BINOP node)
	{
		if(onAMission)
		{
			if(node.left instanceof MEM)
			{
				inMEM = true;
				currentReg = allocator.requestTReg();
				methodRegs.get(currentLabel).add(currentReg);
				inUse.add(currentReg);
			}
			
			return;
		}
		
		int operation = node.binop;
		String binopFragment = indent;

		
		
		switch(operation) 
		{
		case BINOP.PLUS:
			binopFragment += "add ";
			break;

		case BINOP.MINUS:
			binopFragment += "sub ";
			break;

		case BINOP.MUL:
			binopFragment += "mul ";
			break;

		case BINOP.AND:
			binopFragment += "and ";
			break;

		default:
			break;
		}
		
		if(node.left!=null)
			visit(node.left);

		REG left = currentReg;
		
		if(node.right instanceof CALL)
		{
			
		}

		if(node.right instanceof CONST)
		{
			currentReg = allocator.requestTReg();
			
			if(!inMain())
				methodRegs.get(currentLabel).add(currentReg);
		}

		if(node.right!=null)
			visit(node.right);

		REG right = currentReg;
		
		
//		if(equals(right,"$v0"))
//		{
//			REG pleaseJustStoreItCorrectly = allocator.requestTReg();
//			binopFragment += regString(left) + " " + regString(left) + " " + regString(right);
//			methodRegs.get(currentLabel).add(pleaseJustStoreItCorrectly);
//			inUse.add(pleaseJustStoreItCorrectly);
//			buildMethod(binopFragment);
//			return;	
//		}
		if(!equals(left,"$sp") && !equals(left,"$gp") && !equals(left, "$ra"))
		{
			binopFragment += regString(left) + " " + regString(left) + " " + regString(right);
			currentReg = left;
		}
		else
		{
			binopFragment += regString(right) + " " + regString(left) + " " + regString(right);
		}

		newLine(binopFragment);
		buildMethod(binopFragment);

		gui.tick(allocator);
	}

	public void caseConst(CONST node)
	{
		String value = node.value + "";
			
		
		REG constReg = currentReg;

		if(constReg==null)
			constReg = allocator.requestTReg();
		
		if(equals(constReg,"$v0"))
			constReg = allocator.requestTReg();

//		else if(equals(constReg,("$sp")))
//			constReg = allocator.requestTReg();
		
		if(!equals(constReg, "$gp") && !equals(constReg, "$v0"))
			inUse.add(constReg);

		String constFragment =indent + "li " + regString(constReg) + "," + value;
		buildMethod(constFragment);
		newLine(constFragment);
		//System.out.println(constFragment);

		if(equals(constReg, "$gp") || equals(constReg, "$sp")) {
			return;
		}
		currentReg = constReg; //pass the register we just used to whoever needs it

		gui.tick(allocator);
	}

	public void caseESEQ(ESEQ node)
	{	
		if(node.stm instanceof COMMENT)
		{			
			if(node.stm!=null)
				visit(node.stm);	

			if(currentComment.equals("this"))
			{
				REG temp = allocator.requestTReg();
				String reg = regString(temp);
				methodRegs.get(currentLabel).add(temp);
				buildMethod(indent + "li $gp, 4");
				buildMethod(indent + "add " + reg + " $sp, $gp");
				buildMethod(indent + "lw " + reg + " 0("+reg+ ")");
				buildMethod(indent + "move $gp, " +reg);
				allocator.restore(temp);
				currentComment = "";
				return;
			}
		}

		if(node.exp !=null)
			visit(node.exp);

		REG passItOn = currentReg;

		if(node.stm!=null)
			visit(node.stm);

		currentReg = passItOn;


		gui.tick(allocator);
	}



	public void caseName(NAME node)
	{
		currentName = node.label.toString();
		gui.tick(allocator);
	}

	public void caseReg(REG node)
	{

		if(node.reg != null)
			currentReg = new REG(node.reg);

		gui.tick(allocator);
	}	
	
	private boolean inMain()
	{
		return currentLabel.equals("main");
	}
	private boolean in(String string)
	{
		return currentLabel.equals(string);
	}

	public void visit(Stm stm)
	{
		if(stm instanceof SEQ) caseSeq((SEQ)stm);
		if(stm instanceof RETURN ) caseReturn((RETURN)stm);
		if(stm instanceof MOVE) caseMove((MOVE)stm);
		if(stm instanceof COMMENT) caseComment((COMMENT)stm);
		if(stm instanceof EXPR) caseExpr((EXPR)stm);
		if(stm instanceof LABEL) caseLabel((LABEL)stm);	
		if(stm instanceof CJUMP) caseCJump((CJUMP)stm);
		if(stm instanceof JUMP) caseJump((JUMP)stm);
	}

	public void visit(Exp exp)
	{
		if(exp instanceof BINOP) caseBinop((BINOP)exp);
		if(exp instanceof CALL) caseCall((CALL) exp);
		if(exp instanceof CONST) caseConst((CONST)exp);
		if(exp instanceof ESEQ) caseESEQ((ESEQ)exp);
		if(exp instanceof MEM) caseMem((MEM)exp);
		if(exp instanceof NAME) caseName((NAME)exp);
		if(exp instanceof REG) caseReg((REG)exp);
	}

	public void visit(ExpList expList)
	{

		ExpList temp = expList;
		Exp head = temp.head;

		while(head != null)
		{
			visit(head);

			if(temp.tail == null)
				break;

			head = temp.tail.head;
			temp = temp.tail;
		}
	}

	private String regString(REG reg)
	{
		return reg.reg.toString();
	}

	/**
	 * A class to allow the compiler to access registers that are not currently in use.
	 * You can request 'v', 'a', 't', and 's' registers
	 * @author Andrew
	 *
	 */
	private class Allocator
	{
		ArrayList<REG> available = new ArrayList<REG>();

		//when we give the compiler a register it can use, we set it as unavailable
		private final String unavail = "unavailable";

		private final String usedInIrt = "irt";

		private final REG unavailable = new REG(new Reg(unavail));
		private final REG irtReg = new REG(new Reg(usedInIrt));

		public Allocator(ArrayList<REG> regInUse)
		{
			for(int i=2;i <28;i++)
				available.add(new REG(new Reg("$"+ i)));


			removeIRTRegisters(regInUse);


		}

		public ArrayList<REG> getAvailable()
		{
			return available;
		}

		private void removeIRTRegisters(ArrayList<REG> usedInIrt)
		{
			for(REG reg: usedInIrt)
				setIrtReg(reg);

		}



		private void setIrtReg(REG reg)
		{
			for(REG r: available)
			{
				if(equals(r,reg))
				{
					available.set(available.indexOf(r), irtReg);
				}
			}
		}

		public boolean available(int index)
		{
			return !(available.get(index-2).reg.toString().equals(unavail) || usedInIrt(index));// || !available.get(index-2).reg.toString().equals(usedInIrt);
		}
		public boolean usedInIrt(int index)
		{
			return available.get(index-2).reg.toString().equals(usedInIrt);
		}

		public REG requestVReg()
		{
			for(int i=2; i<4;i++)
			{	
				if(available(i))
				{
					REG toReturn = available.get(i-2);
					available.set(i-2, unavailable); //make this register unavailable
					return toReturn;	
				}
			}
			System.out.println("Could not find a 'v' register that is not in use");
			System.out.println("Exiting....");
			System.exit(0);
			return null;
		}
		public REG requestAReg()
		{
			for(int i=4; i<8;i++)
			{
				if(available(i))
				{
					REG toReturn = available.get(i-2);
					available.set(i-2, unavailable); //make this register unavailable
					return toReturn;	
				}
			}
			System.out.println("Could not find an 'a' register that is not in use");
			System.out.println("Exiting....");
			System.exit(0);
			return null;
		}

		public REG requestTReg()
		{
			for(int i=8;i<16;i++)
			{
				if(available(i))
				{
					REG toReturn = available.get(i-2);
					available.set(i-2, unavailable); //make this register unavailable
					return toReturn;	
				}
			}
			return requestSpecialTReg();
		}

		public void markUnavailable(REG reg)
		{

			for(REG r: available)
			{
				if(equals(r,reg))
				{
					available.set(available.indexOf(r), unavailable);
				}
			}
		}

		public REG requestSReg()
		{
			for(int i=16; i<24;i++)
			{
				if(available(i))
				{
					REG toReturn = available.get(i-2);
					available.set(i-2, unavailable); //make this register unavailable
					return toReturn;	
				}
			}
			System.out.println("Could not find an 's' register that is not in use");
			System.out.println("Exiting....");
			System.exit(0);
			return null;
		}

		private REG requestSpecialTReg()
		{
			for(int i=24; i<26;i++)
			{
				if(available(i))
				{
					REG toReturn = available.get(i-2);
					available.set(i-2, unavailable); //make this register unavailable
					return toReturn;	
				}
			}
			System.out.println("Could not find an 's' register that is not in use");
			System.out.println("Exiting....");
			System.exit(0);
			return null;
		}

		public void restore(REG reg)
		{
			if(equals(reg,"$sp") || equals(reg,"$gp") || equals(reg, "$v0"))
				return;

			String indexExtract = reg.reg.toString();
			char[] charArray = indexExtract.toCharArray();
			String num = "";
			for(char c: charArray)
			{
				if(isNum(c))
					num+=c;
			}
			int a = Integer.parseInt(num);
			a-=2;
			if(equals(available.get(a),usedInIrt))
				return;

			available.set(a, reg);
		}

		public void removeReg(REG reg)
		{
			String indexExtract = reg.reg.toString();
			char[] charArray = indexExtract.toCharArray();
			String num = "";
			for(char c: charArray)
			{
				if(isNum(c))
					num+=c;
			}
			int a = Integer.parseInt(num);
			a-=2;
			available.set(a, unavailable);
		}

		private boolean isNum(char c)
		{
			return (c == '0' || c =='1'|| c =='2'|| c =='3'|| c =='4'|| c =='5'|| c =='6'|| c =='7'|| c =='8'|| c =='9');
		}

		private boolean equals(REG one, REG two)
		{
			return regString(one).equals(regString(two));
		}

		private boolean isAvailable(REG reg)
		{
			for(REG r : available)
			{
				if(equals(reg,r) && !equals(reg,usedInIrt))
					return equals(reg, unavailable);
			}
			return false;
		}
		private boolean equals(REG one, String two)
		{
			return regString(one).equals(two);
		}
		public boolean isAnAReg(REG reg)
		{
			return equals(reg,"$4") || equals(reg,"$5") || equals(reg,"$6") || equals(reg,"$7");
		}
		public void clearRegisters()
		{
			for(REG reg: available)
				restore(reg);
		}

	}

	/**
	 * A small GUI used to display which registers are currently being used by the program.
	 * @author Andrew
	 *
	 */
	private class MipsGUI
	{
		private Allocator allocator;
		private JFrame frame;
		private int TICK_RATE;
		private boolean _visible;
		private GridLayout gridLayout;
		private JPanel panel;

		public MipsGUI(Allocator alloc, boolean visible)
		{

			TICK_RATE = 5000;
			_visible = visible;
			gridLayout = new GridLayout(27,1);

			frame = new JFrame("Visual Register Gui");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(gridLayout);

			panel = new JPanel();
			panel.setLayout(gridLayout);

			//panel.setPreferredSize(new Dimension(500,500));

			allocator = alloc;

			for(REG reg: allocator.getAvailable())
			{
				JButton button = new JButton(reg.reg.toString());
				panel.add(button);
			}

			frame.add(panel);
			frame.setVisible(true);
			frame.pack();

		}

		public void update(Allocator alloc)
		{
			frame.remove(panel);
			panel = new JPanel();
			panel.setLayout(gridLayout);
			allocator = alloc;

			for(REG reg: allocator.getAvailable())
			{
				JButton button = new JButton(reg.reg.toString());
				panel.add(button);
			}

			frame.add(panel);
			frame.setVisible(true);
			frame.pack();	

		}

		public void tick(Allocator alloc)
		{
			if(_visible)
			{	

				update(alloc);

				try{

					Thread.sleep(TICK_RATE);
				}

				catch(Exception e)
				{
					System.out.println("Threading error");
					e.printStackTrace();
				}
			}
		}

		public void setTickRate(int tickrate)
		{
			TICK_RATE = tickrate;
		}

	}
	private void parseHeader(File file)
	{
		try{
			Scanner scanner = new Scanner(file);

			while(scanner.hasNextLine())
			{
				String line =  scanner.nextLine();
				headerFile+=line;
				headerFile+=newLine;
			}

		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}

	}

	private boolean equals(REG one, REG two)
	{
		return regString(one).equals(regString(two));
	}

	private boolean equals(REG one, String two)
	{
		return regString(one).equals(two);
	}
	public String parseClassNameFromLabel(String label)
	{
		if(label.equals("main"))
			return label;

		char[] a = label.toCharArray();
		String ret = "";

		for(int i=0;i<label.length();i++)
		{
			ret+=a[i];

			if(a[i+1]=='.')
				break;
		}

		return ret;
	}

	public String parseMethodNameFromLabel(String label)
	{
		char a[] = label.toCharArray();
		int index = 0;
		for(int i=0;i<a.length;i++)
		{
			index=i;
			if(a[i]=='.')
				break;

		}
		String methodName = label.substring(index+1, label.length());

		return methodName;
	}

}
