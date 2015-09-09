package arch;

import tree.*;

public class AccessClassVariable extends Access {
	private int offsetFromBase;

	public AccessClassVariable(int offset) {
		offsetFromBase = offset;
	}

	public int getOffsetFromBase() {
		return offsetFromBase;
	}

	public void setOffsetFromBase(int offset) {
		offsetFromBase = offset;
	}

	public MEM generateIRT() {
		String regname = "base";

		MEM mem = new MEM(new BINOP(BINOP.PLUS, new REG(new Reg(regname)),
				new CONST(offsetFromBase)));
		return mem;
	}

	@Override
	public Exp getTree() {
		return generateIRT();
	}

	@Override
	public Exp getTree(Exp base) {
		// TODO Auto-generated method stub
		return null;
	}

}
