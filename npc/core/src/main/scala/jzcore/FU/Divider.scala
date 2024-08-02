package jzcore

import chisel3._
import chisel3.util._
import utils._
import top._

abstract class Divider extends Module{
  val io = IO(new Bundle {
    val in      = Flipped(new DivInput)
    val out     = Decoupled(new DivOutput)
  })
}

// 恢复余数法
/*
1. 将被除数前面补32个0， 记为A[63:0]，记除数为B[31:0]，得到的商记为S[31:0]， 余数即为 R[31:0] .
2. 第一次迭代，取 A 的高 33 位，即 A[63:31]，与 B 高位补 0 的结果{1’b0,B[31:0]}做减法：如 果结果为负数，则商的相应位(S[31])为 0，被除数保持不变；如果结果为正数，则商的相应 位记为 1，将被除数的相应位(A[63:31])更新为减法的结果。
3. 随后进行第二次迭代，此时就需要取 A[62:30]与{1’b0,B[31:0]}作减法，依据结果更新 S[30]， 并更新 A[62:30]。
4. 依此类推，直到算完第 0 位。
*/
sealed class RestDivider(len: Int) extends Divider {
  // 取反函数
  def getN(num: UInt): UInt = ~num + 1.U

  val done = WireDefault(false.B) // 除法结束标志
  val outFire = io.out.valid & io.out.ready

  val idle :: compute :: recover :: ok :: Nil = Enum(4)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle    -> Mux(io.in.valid, compute, idle),
    compute -> Mux(done, recover, compute),
    recover -> ok,
    ok      -> Mux(outFire, idle, ok)
  ))

  val neg       = Wire(Bool())
  val dividend  = RegInit(0.U((2*len).W))
  val divisor   = RegInit(0.U(len.W))
  val quotient  = RegInit(0.U(len.W))
  val remainder = RegInit(0.U(len.W))
  val cnt       = RegInit(0.U(8.W))
  cnt          := Mux(state === compute, cnt + 1.U, 0.U)

  io.out.valid := state === ok

  when(io.in.divw) {
    // 半字宽度除法
    val tmp     = WireDefault(0.U((len/2+1).W))
    done       := cnt === (len/2-1).U
    neg        := io.in.dividend(len/2-1) ^ io.in.divisor(len/2-1)
    divisor    := Mux(state === idle && io.in.valid, Mux(io.in.divisor(len/2-1) && io.in.divSigned, getN(io.in.divisor), io.in.divisor), divisor)

    when(state === idle && io.in.valid) {
      quotient  := 0.U(len.W)
      dividend  := Mux(io.in.dividend(len/2-1) && io.in.divSigned, ZeroExt(getN(io.in.dividend(len/2-1, 0)), 2*len), ZeroExt(io.in.dividend(len/2-1, 0), 2*len))
    }.elsewhen(state === compute) {
      tmp := dividend(len-1, len/2-1) - (0.U(1.W) ## divisor(len/2-1, 0))
      when(tmp(len/2)) {
        dividend := dividend ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 0.U(1.W)
      }.otherwise {
        val remTmp = tmp(len/2-1, 0) ## dividend(len/2-2, 0)
        dividend := remTmp(len-2, 0) ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 1.U(1.W)
      }
    }.elsewhen(state === recover && io.in.divSigned) {
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
      remainder := Mux(io.in.divSigned && (dividend(len-1) ^ io.in.dividend(len/2-1)), getN(dividend(len-1, len/2)), dividend(len-1, len/2))
    }.otherwise {
      remainder := remainder
    }

    io.out.bits.quotient := SignExt(quotient(len/2-1, 0), len)
    io.out.bits.remainder := SignExt(remainder(len/2-1, 0), len)
  }.otherwise {
    val tmp     = WireDefault(0.U((len+1).W)) // 中间相减的结果
    done       := cnt === (len-1).U
    neg        := io.in.dividend(len-1) ^ io.in.divisor(len-1)
    divisor    := Mux(state === idle && io.in.valid, Mux(io.in.divisor(len-1) && io.in.divSigned, getN(io.in.divisor), io.in.divisor), divisor)

    when(state === idle && io.in.valid) {
      quotient  := 0.U(len.W)
      // 取绝对值
      dividend := Mux(io.in.dividend(len-1) && io.in.divSigned, 0.U(len.W) ## getN(io.in.dividend), 0.U(len.W) ## io.in.dividend)
    }.elsewhen(state === compute) {
      tmp := dividend(2*len-1, len-1) - (0.U(1.W) ## divisor)
      when(tmp(len)) {
        // 为负数被除数保持不变, 商0
        dividend  := dividend ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 0.U(1.W)
      }.otherwise {
        val remTmp = tmp(len-1, 0) ## dividend(len-2, 0)
        dividend  := remTmp(2*len-2, 0) ## 0.U(1.W) // 左移一位
        quotient  := quotient(len-2, 0) ## 1.U(1.W)
      }
    }.elsewhen(state === recover && io.in.divSigned) {
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
      remainder := Mux(io.in.divSigned && (dividend(2*len-1) ^ io.in.dividend(len-1)), getN(dividend(2*len-1, len)), dividend(2*len-1, len))
    }.otherwise {
      remainder := remainder
    }

    io.out.bits.quotient  := quotient
    io.out.bits.remainder := remainder
  }
}

// fast divider, one cycle
sealed class FastDivider extends Divider {
  val divisor = io.in.divisor
  val dividend = io.in.dividend

  when(io.in.divw) {
    val quotientw = WireDefault(0.U(32.W))
    val remainderw = WireDefault(0.U(32.W))
    when(io.in.divSigned) {
      quotientw := (dividend(31, 0).asSInt / divisor(31, 0).asSInt).asUInt
      remainderw := (dividend(31, 0).asSInt % divisor(31, 0).asSInt).asUInt
    }.otherwise {
      quotientw := dividend(31, 0).asUInt / divisor(31, 0).asUInt
      remainderw := dividend(31, 0).asUInt % divisor(31, 0).asUInt
    }
    io.out.bits.quotient := SignExt(quotientw, 64)
    io.out.bits.remainder := SignExt(remainderw, 64)
  }.otherwise {
    when(io.in.divSigned) {
      io.out.bits.quotient := (dividend.asSInt / divisor.asSInt).asUInt
      io.out.bits.remainder := (dividend.asSInt % divisor.asSInt).asUInt
    }.otherwise {
      io.out.bits.quotient := (dividend / divisor).asUInt
      io.out.bits.remainder := (dividend % divisor).asUInt
    }
  }

  io.out.valid := true.B
}