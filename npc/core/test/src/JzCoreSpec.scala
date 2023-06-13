import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chiseltest.formal._
import chiseltest.formal.BoundedCheck
import utest._
import utils._

class BoothTest1 extends Module {
  val io = IO(new Bundle {
    //val sign = Bool()
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val c = Output(UInt(128.W))
  })

  val b1 = io.b(64) ## io.b ## false.B

  val p = RegInit(0.S(128.W)) // 部分积

  val s = (io.a ## 0.U(64.W)).asSInt()
  val sDouble = (io.a ## 0.U(65.W)).asSInt() // 2s
  val sMinus = (~io.a + 1.U).asSInt() // -s
  val sMinusDouble = (~(io.a ## 0.U(65.W)) + 1.U).asSInt() // -2s

  val i = RegInit(1.U(7.W)) // 计数器，进行计数
  i := i + 2.U
  when(i === 1.U) {
    printf("s=%x, sDouble=%x, sMinus=%x, sMinusDouble=%x\n", s, sDouble, sMinus, sMinusDouble)
  }

  val select = b1(i+1.U) ## b1(i) ## b1(i - 1.U)

  val p_tmp = LookupTree(select, List(
    0.U   -> p,
    1.U   -> (p + s),
    2.U   -> (p + s),
    3.U   -> (p + sDouble),
    4.U   -> (p + sMinusDouble),
    5.U   -> (p + sMinus),
    6.U   -> (p + sMinus),
    7.U   -> p
  )).asSInt()

  p := p_tmp >> 2.U
  io.c := p.asUInt()

  printf("p_tmp=%x, select=%d\n", p_tmp, select)

  when(i === 65.U(7.W)) {
    val ref = io.a * io.b
    printf("io.c=%x, ref=%x\n", io.c, ref)
    chisel3.assert(io.c === ref)
  }
}

/*
class PGenerator extends Module {
  val io = IO(new Bundle {
    val yAdd  = Input(Bool())
    val y     = Input(Bool())
    val ySub  = Input(Bool())
    val x     = Input(UInt(68.W))

    val p     = Output(UInt(68.W))
    val c     = Output(Bool())
  })

  val selNegative = io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selPositive = ~io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selDoubleNegative = io.yAdd & ~io.y & ~io.ySub
  val selDoublePositive = ~io.yAdd & io.y & io.ySub

  

  assign pi = ~(~(sel_negative & ~x) & ~(sel_double_negative & ~x_sub) 
            & ~(sel_positive & x ) & ~(sel_double_positive &  x_sub));

} 

class BoothTest2 extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val c = Output(UInt(128.W))
  })


}*/
/*
object JzCoreSpec extends TestSuite {
  val tests: Tests = Tests {
    test("mytest") {
      new Formal with HasTestName {
        def getTestName: String = s"mul"
      }.verify(new Booth, Seq(BoundedCheck(65)))
    }
  }
}*/

object JzCoreSpec extends ChiselUtestTester {
  val tests = Tests {
    test("mul") {
      testCircuit(new BoothTest1) {
        dut =>
          var i = 1
          dut.io.a.poke(89464.U(64.W))
          dut.io.b.poke(99.U(64.W))
          while(i < 67) {
            dut.clock.step()
            i += 2
          }
          //dut.io.c.expect(96252.U(128.W))
      }
    }
  }
}


/*
import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chiseltest.formal._
import chiseltest.formal.BoundedCheck
import utest._

class Sub extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val c = Output(UInt(4.W))
  })
  //val cnt = Counter(true.B, 16)._1
  //io.c := io.a + ~io.b + Mux(cnt === 3.U, 2.U, 1.U)
  io.c := io.a + ~io.b + 1.U
  val ref = io.a - io.b
  chisel3.assert(io.c === ref)
}

object Sub extends TestSuite {
  val tests: Tests = Tests {
    test("mytest") {
      new Formal with HasTestName {
        def getTestName: String = s"sub"
      }.verify(new Sub, Seq(BoundedCheck(1)))
    }
  }
}
*/