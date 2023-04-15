package jzcore

import chisel3._
import chisel3.util._
import utils._

class Soc extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug      = new DebugIO
    val finish     = Output(Bool())

    // 防止被优化
    val valid1     = Output(Bool())
    val valid2     = Output(Bool())
    val csrAddr    = Output(UInt(3.W))
  })

  //val rsram = Module(new Sram)
  val sram = Module(new Sram)
  val core  = Module(new JzCore)

  core.io.axiRaddrIO <> sram.io.raddrIO
  core.io.axiRdataIO <> sram.io.rdataIO
  core.io.axiWaddrIO <> sram.io.waddrIO
  core.io.axiWdataIO <> sram.io.wdataIO
  core.io.axiBrespIO <> sram.io.brespIO

  // 仿真环境
  io.debug        := core.io.debug
  io.valid1       := core.io.axiWaddrIO.valid
  io.valid2       := sram.io.waddrIO.valid
  io.finish       := core.io.finish

  io.csrAddr      := core.io.csrAddr 
}