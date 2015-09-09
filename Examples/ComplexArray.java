/*
 * Output should be:
 * 107
 * 0
 * 1
 * 4
 * 2
 * 100
 * 100
 * 0
 */

class ComplexArray {
   public static void main(String[] a){
      System.out.println(new Foo().run());
   }
}

class Foo {
   
   int[] data;
   
   public int run() {
      int dummy;
      data = new int[5];
      dummy = this.setAll();
      System.out.println(this.sumAll());
      dummy = this.printAll();
      System.out.println(data[data[data[3]]]);
      return 0;
   }
   
   public int setAll() {
      data[0] = 0;
      data[1] = 1;
      data[2] = 4;
      data[3] = 2;
      data[4] = 100;
      return 0;
   }
   
   public int sumAll() {
      return data[0] +
         data[1] +
         data[2] +
         data[3] +
         data[4];
   }
   
   public int printAll() {
      System.out.println(data[0]);
      System.out.println(data[1]);
      System.out.println(data[2]);
      System.out.println(data[3]);
      System.out.println(data[4]);
      return 0;
   }
}
