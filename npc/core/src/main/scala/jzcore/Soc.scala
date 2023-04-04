/*
package jzcore

import chisel3._
import chisel3.util._
import utils._

class Soc extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val pc        = Output(UInt(64.W))
    val nextPc    = Output(UInt(64.W))
    val inst      = Output(UInt(32.W))
    val difftest  = Output(Bool())

    val idu_inst  = Output(UInt(32.W))
    val exu_inst  = Output(UInt(32.W))
  })

  val sram  = Module(new Sram)
  val core  = Module(new JzCore)

  core.io.raddrIO <> sram.io.raddrIO
  core.io.rdataIO <> sram.io.rdataIO

  // 仿真环境
  io.inst         := core.io.inst
  io.pc           := core.io.pc
  io.nextPc       := core.io.nextPc
  io.difftest     := core.io.difftest
  io.idu_inst     := core.io.inst
  io.exu_inst     := core.io.inst
}
*/