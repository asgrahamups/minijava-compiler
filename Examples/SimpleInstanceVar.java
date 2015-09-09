class SimpleInstanceVar {
   public static void main(String[] a){
      // Should print 200
      System.out.println(new Foo().run());
   }
}

class Foo {
   int val;
   
   public int set(int a) {
      val = a;
      return a;
   }
   public int print() {
      // Should print 100
      System.out.println(val);
      return val;
   }
   public int run() {
      int dummy;
      dummy = this.set(100);
      dummy = dummy + this.print();
      return dummy;
   }
}
