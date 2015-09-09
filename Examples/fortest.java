class fortest {
	public static void main(String[] a) {
		System.out.println(new ft().go(12));
	}
}

class ft {
	public int go(int par1) {
		int counter;

		if(counter < par1) {
			System.out.println(12);
		} else {
			System.out.println(3);
		}

		for(counter = 0; counter < par1; counter = counter + 1) {
			System.out.println(counter);
		}
	
		return 20;
	}
}
