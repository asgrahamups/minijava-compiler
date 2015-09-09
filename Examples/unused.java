class unused {
	public static void main(String[] args) {
		System.out.println(new Test().thing());
	}
}

class Test {
	int classVariable;

	public boolean useClassVar() {
		int j;
		boolean booleanRetVal;

		j = classVariable;
		if(j < 10) 
			booleanRetVal = true;
		else
			booleanRetVal = false;

		return booleanRetVal;
	}

	public int nonexistant() {
		int aVar;

		if(fdjfkjff < 10)
			i = 10;
		else
			i = 20;

		return i;
	}

	public int otherThing(int aParam) {
		int retval;

		if(aParam < 10)
			retval = 10;
		else
			retval = 20;

		return retval;
	}

	public int whileTest() {
		int i;
		int inLoop;

		i = 0;

		while(i < 10) {
			inLoop = i;
			i = i + 1;
		}

		return i;
	}

	public int Thing() {
		int i;
		int j;

		i = 1;

		return 12;
	}
} 