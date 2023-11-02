package jzcore

import chisel3._
import chisel3.util._
import utils._

// 部分积生成器
sealed class PGenerator extends Module {
  val io = IO(new Bundle {
    val yAdd  = Input(Bool())
    val y     = Input(Bool())
    val ySub  = Input(Bool())
    val x     = Input(UInt(132.W))

    val p     = Output(UInt(132.W))
    val c     = Output(Bool())
  })
  
  val x = io.x ## false.B

  val selNegative = io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selPositive = ~io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selDoubleNegative = io.yAdd & ~io.y & ~io.ySub
  val selDoublePositive = ~io.yAdd & io.y & io.ySub

  val p_tmp = VecInit(List.fill(132)(false.B))

  (0 to 131).map(i => (p_tmp(i) := ~(~(selNegative & ~x(i+1)) & ~(selDoubleNegative & ~x(i)) & ~(selPositive & x(i+1)) & ~(selDoublePositive & x(i)))))
  io.p := p_tmp.asUInt()
  io.c := selNegative | selDoubleNegative
} 

// booth2位乘法器
class Booth extends Module {
  val io = IO(new Bundle {
    val in      = Flipped(new MultiInput)
    val out     = Decoupled(new MultiOutput)
  })

  val outFire = io.out.valid & io.out.ready 

  val idle :: busy :: Nil = Enum(2)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle -> Mux(io.in.valid, busy, idle),
    busy -> Mux(outFire, idle, busy)
  ))

  val result = RegInit(0.U(132.W)) // 结果寄存器
  val multiplicand = RegInit(0.U(132.W)) // 被乘数
  val multiplier = RegInit(0.U(66.W)) // 乘数

  val pg = Module(new PGenerator)
  pg.io.yAdd := multiplier(2)
  pg.io.y    := multiplier(1)
  pg.io.ySub := multiplier(0)
  pg.io.x    := multiplicand

  when(state === idle && io.in.valid) {
    result := 0.U
    multiplicand := Mux(io.in.mulSigned === MulType.uu, ZeroExt(io.in.multiplicand, 132), SignExt(io.in.multiplicand, 132))
    multiplier := Mux(io.in.mulSigned === MulType.ss, io.in.multiplier(63) ## io.in.multiplier ## false.B, false.B ## io.in.multiplier ## false.B)
  }.elsewhen(state === busy && !io.out.valid) {
    result := pg.io.p + result + pg.io.c
    multiplicand := multiplicand << 2.U
    multiplier := multiplier >> 2.U
  }.otherwise {
    result := result
    multiplicand := multiplicand
    multiplier := multiplier
  }

  io.out.valid         := state === busy & !multiplier.orR
  io.out.bits.resultLo := Mux(io.in.mulw, SignExt(result(31, 0), 64), result(63, 0))
  io.out.bits.resultHi := result(127, 64)
}