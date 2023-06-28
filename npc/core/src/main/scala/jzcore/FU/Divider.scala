package jzcore

import chisel3._
import chisel3.util._
import utils._

/*
// 加减交替法
class Divider extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(DivInput))
    val out     = Decoupled(DivOutput)
  })

  val inFire = io.in.valid & io.in.ready
  val outFire = io.out.valid & io.out.ready

  val idle :: busy :: ok :: Nil = Enum(3)
  val state = RegInit(idle)
  state := MuxLookup(
    idle -> Mux(inFire && !io.flush, busy, idle),
    busy -> Mux(),
    ok   -> Mux(outFire || io.in.flush, idle, ok)
  )

  
}
*/

// 恢复余数法
class Divider(len: Int) extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(new DivInput))
    val out     = Decoupled(new DivOutput)
  })

  // 取反函数
  def getN(num: UInt): UInt = ~num + 1.U

  val done = WireDefault(false.B) // 除法结束标志
  val inFire = io.in.valid & io.in.ready
  val outFire = io.out.valid & io.out.ready

  val idle :: compute :: recover :: ok :: Nil = Enum(4)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle    -> Mux(inFire && !io.flush, compute, idle),
    compute -> Mux(io.flush, idle, Mux(done, recover, compute)),
    recover -> Mux(io.flush, idle, ok),
    ok      -> Mux(io.flush || outFire, idle, ok)
  ))

  val neg       = Wire(Bool())
  val dividend  = RegInit(0.U((2*len).W))
  val divisor   = RegInit(0.U(len.W))
  val quotient  = RegInit(0.U(len.W))
  val remainder = RegInit(0.U(len.W))
  val cnt       = RegInit(0.U(8.W))
  cnt          := Mux(state === compute, cnt + 1.U, 0.U)

  io.in.ready := state === idle
  io.out.valid := state === ok

  when(io.in.bits.divw) {
    val tmp     = WireDefault(0.U((len/2+1).W))
    done       := cnt === (len/2-1).U
    neg        := io.in.bits.dividend(len/2-1) ^ io.in.bits.divisor(len/2-1)
    divisor    := Mux(state === idle && inFire, Mux(io.in.bits.divisor(len/2-1) && io.in.bits.divSigned, getN(io.in.bits.divisor), io.in.bits.divisor), divisor)

    when(state === idle && inFire) {
      quotient  := 0.U(len.W)
      dividend := Mux(io.in.bits.dividend(len/2-1) && io.in.bits.divSigned, ZeroExt(getN(io.in.bits.dividend(len/2-1, 0)), 2*len), ZeroExt(io.in.bits.dividend(len/2-1, 0), 2*len))
    }.elsewhen(state === compute) {
      tmp := dividend(len-1, len/2-1) - (0.U(1.W) ## divisor(len/2-1, 0))
      when(tmp(len/2)) {
        dividend := dividend ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 0.U(1.W)
      }.otherwise {
        val remTmp = tmp ## dividend(len/2-2, 0)
        dividend := remTmp(len-2, 0) ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 1.U(1.W)
      }
    }.elsewhen(state === recover) {
      when(neg) {
        quotient := Mux(quotient(len/2-1), quotient, getN(quotient))
      }.otherwise {
        quotient := Mux(quotient(len/2-1), getN(quotient), quotient)
      }
    }.otherwise {
      dividend := dividend
      quotient := quotient
    }

    when(state === idle) {
      remainder := 0.U(len.W)
    }.elsewhen(state === recover) {
      remainder := Mux(dividend(len-1) ^ io.in.bits.dividend(len/2-1), getN(dividend(len-1, len/2)), dividend(len-1, len/2))
    }.otherwise {
      remainder := remainder
    }

    io.out.bits.quotient := SignExt(quotient(len/2-1, 0), len)
    io.out.bits.remainder := SignExt(remainder(len/2-1, 0), len)
  }.otherwise {
    val tmp     = WireDefault(0.U((len+1).W)) // 中间相减的结果
    done       := cnt === (len-1).U
    neg        := io.in.bits.dividend(len-1) ^ io.in.bits.divisor(len-1)
    divisor    := Mux(state === idle && inFire, Mux(io.in.bits.divisor(len-1) && io.in.bits.divSigned, getN(io.in.bits.divisor), io.in.bits.divisor), divisor)

    when(state === idle && inFire) {
      quotient  := 0.U(len.W)
      // 取绝对值
      dividend := Mux(io.in.bits.dividend(len-1) && io.in.bits.divSigned, 0.U(len.W) ## getN(io.in.bits.dividend), 0.U(len.W) ## io.in.bits.dividend)
    }.elsewhen(state === compute) {
      tmp := dividend(2*len-1, len-1) - (0.U(1.W) ## divisor)
      when(tmp(len)) {
        // 为负数被除数保持不变, 商0
        dividend := dividend ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 0.U(1.W)
      }.otherwise {
        val remTmp = tmp ## dividend(len-2, 0)
        dividend := remTmp(2*len-2, 0) ## 0.U(1.W) // 左移一位
        quotient  := quotient(len-2, 0) ## 1.U(1.W)
      }
    }.elsewhen(state === recover) {
      // 修正
      when(neg) {
        quotient := Mux(quotient(len-1), quotient, getN(quotient))
      }.otherwise {
        quotient := Mux(quotient(len-1), getN(quotient), quotient)
      }
    }.otherwise {
      dividend := dividend
      quotient := quotient
    }

    // 余数
    when(state === idle) {
      remainder := 0.U(len.W)
    }.elsewhen(state === recover) {
      remainder := Mux(dividend(2*len-1) ^ io.in.bits.dividend(len-1), getN(dividend(2*len-1, len)), dividend(2*len-1, len))
    }.otherwise {
      remainder := remainder
    }

    io.out.bits.quotient := quotient
    io.out.bits.remainder := remainder
  }
}
