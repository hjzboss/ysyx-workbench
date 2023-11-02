package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

class Soc extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug      = new DebugIO
    val lsFlag     = Output(Bool())
  })

  if(Settings.getString("core") == "single") {
    val single = Module(new Single)
    // 仿真环境
    io.debug        <> single.io.debug.get
    io.lsFlag       <> single.io.lsFlag.get
  } else if(Settings.getString("core") == "fast") {
    val fast = Module(new FastCore)
    fast.io.interrupt := false.B
    // 仿真环境
    io.debug        <> fast.io.debug.get
    io.lsFlag       <> fast.io.lsFlag.get
  } else {
    val sram = Module(new Sram)
    /*
    val ram0 = Module(new Ram)
    val ram1 = Module(new Ram)
    val ram2 = Module(new Ram)
    val ram3 = Module(new Ram)*/
    val ram4 = Module(new Ram)
    val ram5 = Module(new Ram)
    val ram6 = Module(new Ram)
    val ram7 = Module(new Ram)

    val core = Module(new JzCore)
    core.io.interrupt := false.B

    core.io.master     <> sram.io.slave

    //ram0.io.CLK := clock
    //ram1.io.CLK := clock
    //ram2.io.CLK := clock
    //ram3.io.CLK := clock
    ram4.io.CLK := clock
    ram5.io.CLK := clock
    ram6.io.CLK := clock
    ram7.io.CLK := clock

    // ram, dataArray
    /*
    core.io.sram0.rdata <> ram0.io.Q
    core.io.sram0.cen <> ram0.io.CEN
    core.io.sram0.wen <> ram0.io.WEN
    core.io.sram0.wmask <> ram0.io.BWEN
    core.io.sram0.addr <> ram0.io.A
    core.io.sram0.wdata <> ram0.io.D

    core.io.sram1.rdata <> ram1.io.Q
    core.io.sram1.cen <> ram1.io.CEN
    core.io.sram1.wen <> ram1.io.WEN
    core.io.sram1.wmask <> ram1.io.BWEN
    core.io.sram1.addr <> ram1.io.A
    core.io.sram1.wdata <> ram1.io.D

    core.io.sram2.rdata <> ram2.io.Q
    core.io.sram2.cen <> ram2.io.CEN
    core.io.sram2.wen <> ram2.io.WEN
    core.io.sram2.wmask <> ram2.io.BWEN
    core.io.sram2.addr <> ram2.io.A
    core.io.sram2.wdata <> ram2.io.D

    core.io.sram3.rdata <> ram3.io.Q
    core.io.sram3.cen <> ram3.io.CEN
    core.io.sram3.wen <> ram3.io.WEN
    core.io.sram3.wmask <> ram3.io.BWEN
    core.io.sram3.addr <> ram3.io.A
    core.io.sram3.wdata <> ram3.io.D*/

    core.io.sram4.rdata <> ram4.io.Q
    core.io.sram4.cen <> ram4.io.CEN
    core.io.sram4.wen <> ram4.io.WEN
    core.io.sram4.wmask <> ram4.io.BWEN
    core.io.sram4.addr <> ram4.io.A
    core.io.sram4.wdata <> ram4.io.D

    core.io.sram5.rdata <> ram5.io.Q
    core.io.sram5.cen <> ram5.io.CEN
    core.io.sram5.wen <> ram5.io.WEN
    core.io.sram5.wmask <> ram5.io.BWEN
    core.io.sram5.addr <> ram5.io.A
    core.io.sram5.wdata <> ram5.io.D

    core.io.sram6.rdata <> ram6.io.Q
    core.io.sram6.cen <> ram6.io.CEN
    core.io.sram6.wen <> ram6.io.WEN
    core.io.sram6.wmask <> ram6.io.BWEN
    core.io.sram6.addr <> ram6.io.A
    core.io.sram6.wdata <> ram6.io.D

    core.io.sram7.rdata <> ram7.io.Q
    core.io.sram7.cen <> ram7.io.CEN
    core.io.sram7.wen <> ram7.io.WEN
    core.io.sram7.wmask <> ram7.io.BWEN
    core.io.sram7.addr <> ram7.io.A
    core.io.sram7.wdata <> ram7.io.D

    core.io.interrupt := false.B
    core.io.slave.araddr := 0.U
    core.io.slave.wvalid := false.B 
    core.io.slave.awvalid := false.B
    core.io.slave.arburst := 0.U
    core.io.slave.awlen := 0.U
    core.io.slave.awsize := 0.U
    core.io.slave.wdata := 0.U
    core.io.slave.wlast := true.B
    core.io.slave.wstrb := 0.U
    core.io.slave.awid := 0.U
    core.io.slave.awaddr := 0.U
    core.io.slave.arlen := 0.U
    core.io.slave.arvalid := 0.U
    core.io.slave.rready := 0.U
    core.io.slave.arid := 0.U
    core.io.slave.bready := true.B
    core.io.slave.arsize := 0.U
    core.io.slave.awburst := 0.U

    // 仿真环境
    io.debug        <> core.io.debug.get
    io.lsFlag       <> core.io.lsFlag.get
  }
}