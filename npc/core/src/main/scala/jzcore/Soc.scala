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

  // 仿真环境
  io.debug        := core.io.debug
}