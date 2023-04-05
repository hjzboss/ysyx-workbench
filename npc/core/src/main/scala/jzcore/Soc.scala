package jzcore

import chisel3._
import chisel3.util._
import utils._

class Soc extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug     = new DebugIO
  })

  val sram  = Module(new Sram)
  val core  = Module(new JzCore)

  core.io.axiRaddrIO <> sram.io.raddrIO
  core.io.axiRdataIO <> sram.io.rdataIO

  sram.io.waddrIO.valid := false.B
  sram.io.waddrIO.bits.addr := "h80000000".U
  sram.io.wdataIO.valid := false.B
  sram.io.wdataIO.bits.wdata := 0.U
  sram.io.wdataIO.bits.wstrb := 0.U
  sram.io.brespIO.ready := false.B

  // 仿真环境
  io.debug        := core.io.debug
}