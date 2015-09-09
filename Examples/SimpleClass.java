/*
 * Output should be:
 * 50
 * 100
 * 0
 */

class SimpleClass {
   public static void main(String[] a){
      System.out.println(new Foo().run());
   }
}

class Foo {
   public int print(int val) {
      System.out.println(val);
      return 0;
   }
   public int run() {
      int dummy;
      dummy = this.print(50);   // print 50
      return this.print(100);   // print 100
   }
}
