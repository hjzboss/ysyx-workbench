import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chiseltest.formal._
import chiseltest.formal.BoundedCheck
import utest._
import utils._

class Booth extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val c = Output(UInt(128.W))
  })

  val b1 = b ## false.B

  val p = RegInit(0.U(128.W)) // 部分积

  val s = io.a ## 0.U(8.W)
  val sDouble = io.a ## 0.U(9.W) // 2s
  val sMinus = ~io.a + 1.U // -s
  val sMinusDouble = ~(io.a ## 0.U(9.W)) + 1.U // -2s

  val i = RegInit(1.U(7.W)) // 计数器，进行计数
  i := i + 2.U

  val select = io.b(i+1.U) ## io.b(i) ## io.b(i - 1.U)

  val p_tmp = LookupTree(select, p, List(
    0.U   -> p,
    1.U   -> (p + s),
    2.U   -> (p + s),
    3.U   -> (p + sDouble),
    4.U   -> (p + sMinusDouble),
    5.U   -> (p + sMinus),
    6.U   -> (p + sMinus),
    7.U   -> p
  ))

  p := p_tmp.asSInt() >> 2.U
  io.c := p

  when(i === 66.U(7.W)) {
    val ref = io.a * io.b
    chisel3.assert(io.c === ref)
  }
}

object Mul extends TestSuite {
  val tests: Tests = Tests {
    test("mytest") {
      new Formal with HasTestName {
        def getTestName: String = s"mul"
      }.verify(new Booth, Seq(BoundedCheck(65)))
    }
  }
}