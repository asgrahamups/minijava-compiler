class OneMethod {
    public static void main(String[] a){
        System.out.println(new Foo().run(5, 10));
    }
}

class Foo {
    public int run(int n1, int n2) 
    {
        int result;
        result = n1 + n2;
        System.out.println(result);
        return result;
    }
}
