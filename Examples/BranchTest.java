class BranchTest 
{
	public static void main(String[] a) 
	{
		System.out.println(new Branch().run(4));
	}
}

class Branch 
{
	public int run(int size)
	{
		int j;
		boolean stfjfjf;

		j = 1;
		stfjfjf = true;
		
		while (stfjfjf && j < size) 
		{
	    	System.out.println(j);
	    	j = j + 1;
		}

		return j;
	}
	
}