class Add{
	public static void main(String[] args)
	{	
		System.out.println(new Addition().subtract(1000,2));
	}
}

class Addition
{
	public int multiply(int numOne, int numTwo)
	{
		int sum;
		sum = numOne * numTwo;
		return sum;
	}
	public int subtract(int numOne, int numTwo)
	{
		int result;
		result = numOne-numTwo;
		return result;
	}
}