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
    //val csrAddr    = Output(UInt(3.W))

    val lsFlag     = Output(Bool())
  })

  //val rsram = Module(new Sram)
  val sram = Module(new Sram)
  val ram0 = Module(new Ram)
  val ram1 = Module(new Ram)
  val ram2 = Module(new Ram)
  val ram3 = Module(new Ram)
  val core = Module(new JzCore)

  core.io.axiRaddrIO <> sram.io.raddrIO
  core.io.axiRdataIO <> sram.io.rdataIO
  core.io.axiWaddrIO <> sram.io.waddrIO
  core.io.axiWdataIO <> sram.io.wdataIO
  core.io.axiBrespIO <> sram.io.brespIO

  ram0.CLK := Clock
  ram1.CLK := Clock
  ram2.CLK := Clock
  ram3.CLK := Clock

  // ram, dataArray
  core.io.sram0_rdata <> ram0.io.Q
  core.io.sram0_cen <> ram0.io.CEN
  core.io.sram0_wen <> ram0.io.WEN
  core.io.sram0_wmask <> ram0.io.BWEN
  core.io.sram0_addr <> ram0.io.A
  core.io.sram0_wdata <> ram0.io.D

  core.io.sram1_rdata <> ram1.io.Q
  core.io.sram1_cen <> ram1.io.CEN
  core.io.sram1_wen <> ram1.io.WEN
  core.io.sram1_wmask <> ram1.io.BWEN
  core.io.sram1_addr <> ram1.io.A
  core.io.sram1_wdata <> ram1.io.D

  core.io.sram2_rdata <> ram2.io.Q
  core.io.sram2_cen <> ram2.io.CEN
  core.io.sram2_wen <> ram2.io.WEN
  core.io.sram2_wmask <> ram2.io.BWEN
  core.io.sram2_addr <> ram2.io.A
  core.io.sram2_wdata <> ram2.io.D

  core.io.sram3_rdata <> ram3.io.Q
  core.io.sram3_cen <> ram3.io.CEN
  core.io.sram3_wen <> ram3.io.WEN
  core.io.sram3_wmask <> ram3.io.BWEN
  core.io.sram3_addr <> ram3.io.A
  core.io.sram3_wdata <> ram3.io.D

  // 仿真环境
  io.debug        := core.io.debug
  io.valid1       := core.io.axiWaddrIO.valid
  io.valid2       := sram.io.waddrIO.valid
  io.finish       := core.io.finish

  io.lsFlag       := core.io.lsFlag
  //io.csrAddr      := core.io.csrAddr 
}