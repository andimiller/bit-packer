package bitpacker.macros

import scodec.Codec

import scala.collection.immutable.Seq
import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.meta.Term.{ApplyInfix, Arg}
import scala.meta._

class Bytes[T](c: Codec[T]) extends StaticAnnotation {}

class Bits(i: Int) extends StaticAnnotation {}

class GenerateBitPacking extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case cls @ Defn.Class(_, _, _, Ctor.Primary(_, _, paramss), template) =>
        val params = paramss match {
          case params :: Nil => params
          case _ => abort("no, sorry")
        }
        // hlist type
        val hlistTypes: Type = params.foldRight[Type](t"HNil") {
          case (Term.Param(_, _, Some(decltpe: Type), _), accum) =>
            t"$decltpe :: $accum"
        }
        val hlistType =
          q"type hlistType = $hlistTypes"
        // hlist term
        val format = params.foldRight[Term](q"HNil") {
          case (Term.Param(_, name, Some(dcltpe: Type), _), accum) =>
            q"${Term.Name(name.value)} :: $accum"
        }
        val formatDef =
          q"def toFormat = $format"
        // codecs
        val codecs = params.foldRight[Term](q"HNil") {
          case (Term.Param(mods, name, Some(dcltpe: Type), _), accum) =>
            val annotations = mods.collectFirst{
              case Mod.Annot(q"$annot($value)") =>
                println(s"$annot was set to $value on $name of type $dcltpe")
                value.toString()
            }
            val t = Term.Name(annotations.getOrElse(s"Codec[$dcltpe]"))
            q"$t :: $accum"
        }
        val getCodec =
          q"def toCodec = $codecs"
        // record type

        val templateStats: Seq[Stat] = List(hlistType, formatDef, getCodec) ++  template.stats.getOrElse(Nil)
        cls.copy(templ = template.copy(stats = Some(templateStats)))
      case _ =>
        println(defn.structure)
        abort("@BitFormatted must annotate a class.")
    }
  }
}

class LongClass extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case cls @ Defn.Class(_, _, _, Ctor.Primary(_, _, paramss), template) =>
        val params = paramss match {
          case params :: Nil => params
          case _ => abort("no, sorry")
        }
        // codecs
        val parameters = params.map {
          case Term.Param(mods, name, Some(dcltpe: Type), _) =>
            val size = mods.collectFirst{
              case Mod.Annot(q"$annot($value)") =>
                println(s"$annot was set to $value on $name of type $dcltpe")
                value.toString().toInt
            }
            val finalsize = size.getOrElse { dcltpe match {
              case t"Int" => 32
              case t"Long" => 64
              case t"Boolean" => 1
              }
            }
            (name, finalsize)
        }
        val getters = parameters.foldLeft(0, List.empty[Defn.Def]) { case ((offset, results), (name, size)) =>
          val getter = q"def ${Term.Name(name.value)} = v >>> ${Term.Name(offset.toString)} << ${Term.Name((64-size-offset).toString)}"
          println(getter)
          (size+offset, results :+ getter)
        }

        val internals: Seq[Stat] = getters._2

        q"""class ${cls.name}(val v: Long) extends AnyVal {
            ..$internals
            }

            object ${Term.Name(cls.name.value)} {
              // put stuff here
            }
         """

      case _ =>
        println(defn.structure)
        abort("this must annotate a class.")
    }
  }
}
