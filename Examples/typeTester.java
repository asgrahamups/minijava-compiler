class Fac 
{

	int[] k;
    
    public int test()
    {
    	int a;
    	int b;

    	a=0;

    	b=10;

    	k = new int[10];

    	b = k[10] + b;

    	while(a<b && true)
    	{
    		b = 10;
    	}

    	return a;
    }
}


class FacTester extends Fac
{
    int b;

    public int testAgain(int i, boolean j)
    {
        int[] c;


        return 10;
    }
    public int methodTest()

    {
        return 0;
    }
}

class FacTesterTester
{

    public int test()
    {
        FacTester fac;
        Fac facter;
        int a;
        facter = new Fac();
        fac = new FacTester();
        a = fac.testAgain(10,false);
        return 0;
    }

}