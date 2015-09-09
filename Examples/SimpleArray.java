/*
 * Output should be:
 * 100
 * 10
 * 0
 */

class SimpleArray {
   public static void main(String[] a){
      System.out.println(new Foo().run());
   }
}

class Foo {
   public int run() {
      int[] array;
      array = new int[10];
      array[1+2] = 100;
      System.out.println(array[5-2]);
      System.out.println(array.length);
      return 0;
   }
}