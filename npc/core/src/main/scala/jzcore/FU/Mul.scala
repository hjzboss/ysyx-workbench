package jzcore

import chisel3._
import chisel3.util._
import utils._
import top._


class Mul extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(new MultiInput)
    val out     = Decoupled(new MultiOutput)
  })

  val mulType: String = Settings.getString("mul")

  if(mulType == "booth") {
    val booth = Module(new Booth)
    booth.io.flush <> io.flush
    booth.io.in <> io.in
    booth.io.out <> io.out
  } else if(mulType == "wallance") {
    val wallace = Module(new Wallace)
    wallace.io.flush <> io.flush
    wallace.io.in <> io.in
    wallace.io.out <> io.out
  } else {
    val fastMul = Module(new FastMul)
    fastMul.io.flush <> io.flush
    fastMul.io.in <> io.in
    fastMul.io.out <> io.out
  }
}

sealed class FastMul extends Module {
  val io = IO(new Bundle{
    val flush   = Input(Bool())
    val in      = Flipped(new MultiInput)
    val out     = Decoupled(new MultiOutput)
  })

  val multiplicand = io.in.multiplicand
  val multiplier = io.in.multiplier

  when(io.in.mulw) {
    val resultw = Wire(UInt(64.W))
    switch(io.in.mulSigned) {
      is(MulType.uu) {
        resultw := multiplicand(31, 0).asUInt * multiplier(31, 0).asUInt
      }
      is(MulType.ss) {
        resultw := multiplicand(31, 0).asSInt * multiplier(31, 0).asSInt
      }
      is(MulType.su) {
        resultw := multiplicand(31, 0).asSInt * multiplier(31, 0).asUInt
      }
    }
    io.out.bits.resultLo := SignExt(resultw(31, 0), 64)
    io.out.bits.resultHi := SignExt(resultw(63, 32), 64)
  }.otherwise {
    val result = Wire(UInt(128.W))
    switch(io.in.mulSigned) {
      is(MulType.uu) {
        result := multiplicand.asUInt * multiplier.asUInt
      }
      is(MulType.ss) {
        result := multiplicand.asSInt * multiplier.asSInt
      }
      is(MulType.su) {
        result := multiplicand.asSInt * multiplier.asUInt
      }
    }
    io.out.bits.resultLo := result(63, 0)
    io.out.bits.resultHi := result(127, 64)
  }
  io.out.valid := true.B
}