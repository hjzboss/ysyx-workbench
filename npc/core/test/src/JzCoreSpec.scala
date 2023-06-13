import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chiseltest.formal._
import chiseltest.formal.BoundedCheck
import utest._
import utils._

/*
class BoothTest1 extends Module {
  val io = IO(new Bundle {
    //val sign = Bool()
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val c = Output(UInt(128.W))
  })

  val b1 = io.b(63) ## io.b ## false.B

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
}*/

// 部分积生成器
class PGenerator extends Module {
  val io = IO(new Bundle {
    val yAdd  = Input(Bool())
    val y     = Input(Bool())
    val ySub  = Input(Bool())
    val x     = Input(UInt(68.W))

    val p     = Output(UInt(68.W))
    val c     = Output(Bool())
  })
  
  val x = io.x ## false.B

  val selNegative = io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selPositive = ~io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selDoubleNegative = io.yAdd & ~io.y & ~io.ySub
  val selDoublePositive = ~io.yAdd & io.y & io.ySub

  val p_tmp = VecInit(List.fill(68)(false.B))

  (0 to 67).map(i => (p_tmp(i) := ~(~(selNegative & ~x(i+1)) & ~(selDoubleNegative & ~x(i)) & ~(selPositive & x(i+1)) & ~(selDoublePositive & x(i)))))

  io.p := p_tmp.asUInt()
  io.c := selNegative | selDoubleNegative
} 

class BoothTest2 extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val c = Output(UInt(128.W))
    val ready = Output(Bool())
  })

  val idle :: busy :: Nil = Enum(2)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle -> Mux(io.valid, busy, idle),
    busy -> Mux(io.ready, idle, busy)
  ))

  val result = RegInit(0.U(132.W)) // 结果寄存器
  val multiplicand = RegInit(0.U(132.W)) // 被乘数
  val multiplier = RegInit(0.U(66.W)) // 乘数

  val pg = Module(new PGenerator)
  pg.io.yAdd := multiplier(2)
  pg.io.y    := multiplier(1)
  pg.io.ySub := multiplier(0)
  pg.io.x    := multiplicand

  when(state === idle && io.valid) {
    result := 0.U
    multiplicand := SignExt(io.a, 132) // todo
    multiplier := io.b(63) ## io.a ## false.B
  }.elsewhen(state === busy && !io.ready) {
    result := pg.io.p + result + pg.io.c
    multiplicand := multiplicand << 2.U
    multiplier := multiplier >> 2.U
  }.otherwise {
    result := result
    multiplicand := multiplicand
    multiplier := multiplier
  }

  io.ready  := !multiplier.orR
  io.c      := result
}
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
      testCircuit(new BoothTest2) {
        dut =>
          dut.io.a.poke(894.U(64.W))
          dut.io.b.poke(99.U(64.W))
          var flag = true
          dut.io.valid.poke(true.B)
          while(flag) {
            dut.clock.step()
            when(dut.io.ready.peek() === true.B) {
              flag = false
            }
          }
          dut.io.c.expect((894 * 99).U(128.W))
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