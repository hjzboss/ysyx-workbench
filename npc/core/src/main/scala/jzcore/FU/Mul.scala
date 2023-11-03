package jzcore

import chisel3._
import chisel3.util._
import utils._
import top._


abstract class Mul extends Module {
  val io = IO(new Bundle {
    val in      = Flipped(new MultiInput)
    val out     = Decoupled(new MultiOutput)
  })
}

sealed class FastMul extends Mul {
  val multiplicand = io.in.multiplicand
  val multiplier = io.in.multiplier

  when(io.in.mulw) {
    val resultw = WireDefault(0.U(64.W))
    switch(io.in.mulSigned) {
      is(MulType.uu) {
        resultw := (multiplicand(31, 0).asUInt * multiplier(31, 0).asUInt).asUInt
      }
      is(MulType.ss) {
        resultw := (multiplicand(31, 0).asSInt * multiplier(31, 0).asSInt).asUInt
      }
      is(MulType.su) {
        resultw := (multiplicand(31, 0).asSInt * multiplier(31, 0).asUInt).asUInt
      }
    }
    io.out.bits.resultLo := SignExt(resultw(31, 0), 64).asUInt
    io.out.bits.resultHi := SignExt(resultw(63, 32), 64).asUInt
  }.otherwise {
    val result = WireDefault(0.U(128.W))
    switch(io.in.mulSigned) {
      is(MulType.uu) {
        result := (multiplicand.asUInt * multiplier.asUInt).asUInt
      }
      is(MulType.ss) {
        result := (multiplicand.asSInt * multiplier.asSInt).asUInt
      }
      is(MulType.su) {
        result := (multiplicand.asSInt * multiplier.asUInt).asUInt
      }
    }
    io.out.bits.resultLo := result(63, 0).asUInt
    io.out.bits.resultHi := result(127, 64).asUInt
  }
}