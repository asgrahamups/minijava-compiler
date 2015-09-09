package arch;

import tree.*;

public class AccessLocal extends Access {
	int offsetFromBase = 0;

	public AccessLocal(int offset) {
		offsetFromBase = offset;
	}

	public int getOffsetFromBase() {
		return offsetFromBase;
	}

	public void setOffsetFromBase(int offsetFromBase) {
		this.offsetFromBase = offsetFromBase;
	}

	public String toString() {
		String regname = "base";

		return "new MEM(\n" + "\tBINOP(PLUS,\n" + "\tREG " + regname + ",\n"
				+ "\tCONST " + offsetFromBase + "))";
	}

	public int getOffset() {
		return offsetFromBase;
	}

	public MEM generateIRT() {
		String regname = "$sp";

		MEM mem = new MEM(new BINOP(BINOP.PLUS, new REG(new Reg(regname)),
				new CONST(offsetFromBase)));
		return mem;
	}

	public MEM generateBsIrt() {
		String regname = "$sp";

		MEM mem = new MEM(new BINOP(BINOP.PLUS, new REG(new Reg(regname)),
				new CONST(4)));
		return mem;
	}

	public MEM generateIRT(String regName) {

		MEM mem = new MEM(new BINOP(BINOP.PLUS, new REG(new Reg(regName)),
				new CONST(offsetFromBase)));
		return mem;
	}

	public MEM generateFieldIRT() {
		MEM mem = new MEM(new BINOP(BINOP.PLUS, new REG(new Reg("$sp")),
				new CONST(offsetFromBase)));
		return mem;
	}

	@Override
	public Exp getTree() {
		return generateIRT();
	}

	public Exp getTreeSP(String regName) {
		return generateIRT(regName);
	}

	@Override
	public Exp getTree(Exp base) {
		// TODO Auto-generated method stub
		return null;
	}

}
