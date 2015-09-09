class MainThing {
	public static void main(String[] a) {
		System.out.println(new OtherThing().doThing(6, 17));
	}
}

class OtherThing {
	public int[] doThing(int a, int b) {
		int[] x;
		x = new int[10 * b];
		return x;
	}
}