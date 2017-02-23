import bitpacker.macros._

/**
  * Created by Andi on 23/02/2017.
  */

@LongClass
class MyClass(@Bits(32) a: Long, @Bits(32) b: Long)

object LongClassTest extends App {

  val l = new MyClass(Long.MaxValue)
  println(l)
  println(l.a)
  println(l.b)

}
