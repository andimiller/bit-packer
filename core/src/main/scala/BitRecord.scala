package scodec.codecs.mine

import scodec.{Codec, SizeBound}
import scodec.codecs._
import scodec.bits._
import shapeless._
import poly._
import record._
import shapeless.labelled.FieldType
import shapeless.syntax.SingletonOps
import syntax.singleton._
import bitpacker.macros.{Bits, GenerateBitPacking}

/**
  * Created by Andi on 20/01/2017.
  */
object BitRecord extends App {

  object ExtraCodecs {
    val uint3: Codec[Int] = new IntCodec(3, false, ByteOrdering.BigEndian)
    val uint5: Codec[Int] = new IntCodec(5, false, ByteOrdering.BigEndian)
    val uint6: Codec[Int] = new IntCodec(6, false, ByteOrdering.BigEndian)
    val uint7: Codec[Int] = new IntCodec(7, false, ByteOrdering.BigEndian)
    val uint9: Codec[Int] = new IntCodec(9, false, ByteOrdering.BigEndian)
    val uint10: Codec[Int] = new IntCodec(10, false, ByteOrdering.BigEndian)
    val uint11: Codec[Int] = new IntCodec(11, false, ByteOrdering.BigEndian)
    val uint12: Codec[Int] = new IntCodec(12, false, ByteOrdering.BigEndian)

  }
  import ExtraCodecs._

  val format = ("age" ->> uint3) :: ("hasTail" ->> bool) :: ("pin" ->> uint12) :: HNil
  val result = format.values.toCodec.decode(bin"0101010101010101").map(x => x.value.zipWithKeys(format.keys))

  result.map { r =>
    println(s"the record's age is ${r("age")}")
    println(s"the record's tail status is ${r("hasTail")}")
    println(s"the record's pin number is ${r("pin")}")
    //println(s"the record's missing attribute is ${r("nothing")}")
    @GenerateBitPacking
    case class Dog(@Bits(uint3) age: Int, @Bits(bool) hasTail: Boolean, @Bits(uint12) pin: Int)
    val dog = Generic[Dog].from(r.values)
    println(dog)
    println(dog.codec.toCodec.decode(bin"0101010101010101"))
  }
}
