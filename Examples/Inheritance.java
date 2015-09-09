

class Inheritance {
   public static void main(String[] a){
      System.out.println(new Run().test());
   }
}


class Foo {
   int fooVal;
   
   public int set(int a) {
      fooVal = a;
      return a;
   }
   
   public int get() {
      return fooVal;
   }
}


class SubFoo extends Foo {
   int subFooVal;
   
   public int setMyVar(int a) {
      subFooVal = a;
      return a;
   }
   
   public int get() {
      return 2 * fooVal;
   }
   
   public int getMyVar() {
      return subFooVal;
   }
}

class Run {
   
   public int test() {
      Foo foo;
      SubFoo subfoo;
      int dummy;
      
      foo = new Foo();
      subfoo = new SubFoo();
      
      // Make sure the basic setters and getters work as
      // expected.
      
      dummy = foo.set(5);
      dummy = subfoo.set(50);
      dummy = subfoo.setMyVar(10);
      
      System.out.println(foo.get());         // 5
      System.out.println(subfoo.get());      // 100
      System.out.println(subfoo.getMyVar()); // 10
      
      // Now test polymorphism.  In a basic (static typing only)
      // implementation, printFooVar will treat every object 
      // passed to it as a Foo instance, even if it's really a
      // SubFoo.
      
      dummy = this.printFooVar(foo);         // 5
      dummy = this.printFooVar(subfoo);      // 50 -- uses wrong get()
      
      return 0;
   }
   
   public int printFooVar(Foo obj) {
      int val;
      val = obj.get();
      System.out.println(val);
      return 0;
   }
}
