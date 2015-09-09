class smalltest {
	public static void main(String[] a) {
		System.out.println(new Small().method());
	}
}

class Small {
	public int something() {
		return new Small().val(1, 12, 13);
	}

	public boolean method(int x, int y, int z, boolean s) {
		return (x + y + z) < x;
	}
}

class Thing {
	public int val(int x, int y, int z) {
		return x;
	}
}