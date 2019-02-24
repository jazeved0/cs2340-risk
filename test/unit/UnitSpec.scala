import org.scalatest.FunSpec

class C {
  def x = {
    Thread.sleep(10)
    1
  }
}


class XYZTest extends FunSpec {
  def withTime[T](procName: String, f: => T): T = {
    val start = System.currentTimeMillis()
    val r = f
    val end = System.currentTimeMillis()
    print(s"$procName job done in ${end-start}ms")
    r
  }

  describe("SomeTest") {
    it("rebuild each time") {
      val s = withTime("Reflection from start : ", (0 to 100). map {x =>
        val ru = scala.reflect.runtime.universe
        val m = ru.runtimeMirror(getClass.getClassLoader)
        val methodX = ru.typeOf[C].decl(ru.TermName("x")).asMethod

        val im = m.reflect(new C)
        val mm = im.reflectMethod(methodX)
        mm().asInstanceOf[Int]
      }).sum
      println(s" got $s")
    }
    it("invoke each time") {
      val ru = scala.reflect.runtime.universe
      val m = ru.runtimeMirror(getClass.getClassLoader)
      val methodX = ru.typeOf[C].decl(ru.TermName("x")).asMethod

      val s = withTime("Invoke method reflection: ", (0 to 100). map {x =>
        val im = m.reflect(new C)
        val mm = im.reflectMethod(methodX)
        mm().asInstanceOf[Int]
      }).sum
      println(s" got $s")
    }
    it("invoke directly") {
      val c = new C()
      val s = withTime("No reflection: ", (0 to 100). map {x =>
        c.x
      }).sum
      println(s" got $s")
    }
  }
}