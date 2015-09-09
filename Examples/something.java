class R
{
	public static void main(String[] args)
	{
		System.out.println(new R().addUntilThousand(10));
	}
	public int addUntilThousand(int b)
	{
		if(b==1000)
			return b;

		return addUntilThousand(b+1);
	}
}