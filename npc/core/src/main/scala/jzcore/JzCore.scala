package jzcore

import chisel3._
import chisel3.util._
import utils._

class JzCore extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug       = new DebugIO
    val finish      = Output(Bool())

    // icache data array
    val sram0_rdata     = Input(UInt(128.W))
    val sram0_cen       = Output(Bool())
    val sram0_wen       = Output(Bool())
    val sram0_wmask     = Output(UInt(128.W))
    val sram0_addr      = Output(UInt(6.W))
    val sram0_wdata     = Output(UInt(128.W)) 

    val sram1_rdata     = Input(UInt(128.W))
    val sram1_cen       = Output(Bool())
    val sram1_wen       = Output(Bool())
    val sram1_wmask     = Output(UInt(128.W))
    val sram1_addr      = Output(UInt(6.W))
    val sram1_wdata     = Output(UInt(128.W)) 

    val sram2_rdata     = Input(UInt(128.W))
    val sram2_cen       = Output(Bool())
    val sram2_wen       = Output(Bool())
    val sram2_wmask     = Output(UInt(128.W))
    val sram2_addr      = Output(UInt(6.W))
    val sram2_wdata     = Output(UInt(128.W)) 

    val sram3_rdata     = Input(UInt(128.W))
    val sram3_cen       = Output(Bool())
    val sram3_wen       = Output(Bool())
    val sram3_wmask     = Output(UInt(128.W))
    val sram3_addr      = Output(UInt(6.W))
    val sram3_wdata     = Output(UInt(128.W)) 

    // dcache data array
    val sram4_rdata     = Input(UInt(128.W))
    val sram4_cen       = Output(Bool())
    val sram4_wen       = Output(Bool())
    val sram4_wmask     = Output(UInt(128.W))
    val sram4_addr      = Output(UInt(6.W))
    val sram4_wdata     = Output(UInt(128.W)) 

    val sram5_rdata     = Input(UInt(128.W))
    val sram5_cen       = Output(Bool())
    val sram5_wen       = Output(Bool())
    val sram5_wmask     = Output(UInt(128.W))
    val sram5_addr      = Output(UInt(6.W))
    val sram5_wdata     = Output(UInt(128.W)) 

    val sram6_rdata     = Input(UInt(128.W))
    val sram6_cen       = Output(Bool())
    val sram6_wen       = Output(Bool())
    val sram6_wmask     = Output(UInt(128.W))
    val sram6_addr      = Output(UInt(6.W))
    val sram6_wdata     = Output(UInt(128.W)) 

    val sram7_rdata     = Input(UInt(128.W))
    val sram7_cen       = Output(Bool())
    val sram7_wen       = Output(Bool())
    val sram7_wmask     = Output(UInt(128.W))
    val sram7_addr      = Output(UInt(6.W))
    val sram7_wdata     = Output(UInt(128.W)) 

    // axi访存接口
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))   

    val lsFlag      = Output(Bool())
  })

  val ifu     = Module(new IFU)
  val idu     = Module(new IDU)
  val exu     = Module(new EXU)
  val lsu     = Module(new LSU)
  val wbu     = Module(new WBU)
  val ctrl    = Module(new CTRL)
  val arbiter = Module(new AxiArbiter) // todo:仲裁器
  val icache  = Module(new ICache)
  val dcache  = Module(new DCache)

  val idReg   = Module(new ID_REG)
  val exReg   = Module(new EX_REG)
  val lsReg   = Module(new LS_REG)
  val wbReg   = Module(new WB_REG)
  val forward = Module(new Forwarding)

  //io.csrAddr  := idu.io.csrAddr
  // 仲裁
  arbiter.io.ifuReq   <> icache.io.axiReq
  arbiter.io.grantIfu <> icache.io.axiGrant
  arbiter.io.ifuReady <> icache.io.axiReady
  arbiter.io.lsuReq   <> dcache.io.axiReq
  arbiter.io.grantLsu <> dcache.io.axiGrant
  arbiter.io.lsuReady <> dcache.io.axiReady

  // axi访问接口
  val grant = Cat(arbiter.io.grantIfu, arbiter.io.grantLsu)
  when(grant === 2.U) {
    io.axiRaddrIO <> icache.io.axiRaddrIO
    io.axiRdataIO <> icache.io.axiRdataIO
    io.axiWaddrIO <> icache.io.axiWaddrIO
    io.axiWdataIO <> icache.io.axiWdataIO
    io.axiBrespIO <> icache.io.axiBrespIO

    dcache.io.axiRaddrIO.ready   := false.B
    dcache.io.axiRdataIO.valid   := false.B
    dcache.io.axiRdataIO.bits.rdata := 0.U
    dcache.io.axiRdataIO.bits.rresp   := 0.U
    dcache.io.axiRdataIO.bits.rlast   := true.B
    dcache.io.axiWaddrIO.ready   := false.B
    dcache.io.axiWdataIO.ready   := false.B
    dcache.io.axiBrespIO.valid   := false.B
    dcache.io.axiBrespIO.bits.bresp   := 0.U
  }
  .elsewhen(grant === 1.U || grant === 3.U) {
    io.axiRaddrIO <> dcache.io.axiRaddrIO
    io.axiRdataIO <> dcache.io.axiRdataIO
    io.axiWaddrIO <> dcache.io.axiWaddrIO
    io.axiWdataIO <> dcache.io.axiWdataIO
    io.axiBrespIO <> dcache.io.axiBrespIO

    icache.io.axiRaddrIO.ready   := false.B
    icache.io.axiRdataIO.valid   := false.B
    icache.io.axiRdataIO.bits.rdata := 0.U
    icache.io.axiRdataIO.bits.rlast := true.B
    icache.io.axiRdataIO.bits.rresp   := 0.U
    icache.io.axiWaddrIO.ready   := false.B
    icache.io.axiWdataIO.ready   := false.B
    icache.io.axiBrespIO.valid   := false.B
    icache.io.axiBrespIO.bits.bresp   := 0.U
  }
  .otherwise {
    // 没有请求
    io.axiRaddrIO.valid       := false.B
    io.axiRaddrIO.bits.addr   := 0.U
    io.axiRaddrIO.bits.len    := 0.U
    io.axiRaddrIO.bits.size   := 0.U
    io.axiRaddrIO.bits.burst  := 0.U
    io.axiWaddrIO.bits.len    := 0.U
    io.axiWaddrIO.bits.size   := 0.U
    io.axiWaddrIO.bits.burst  := 0.U
    io.axiRdataIO.ready       := false.B
    io.axiWaddrIO.valid       := false.B
    io.axiWaddrIO.bits.addr   := 0.U
    io.axiWdataIO.valid       := false.B
    io.axiWdataIO.bits.wdata  := 0.U
    io.axiWdataIO.bits.wstrb  := 0.U
    io.axiWdataIO.bits.wlast  := true.B
    io.axiBrespIO.ready       := false.B

    icache.io.axiRaddrIO.ready   := false.B
    icache.io.axiRdataIO.valid   := false.B
    icache.io.axiRdataIO.bits.rdata := 0.U
    icache.io.axiRdataIO.bits.rresp   := 0.U
    icache.io.axiRdataIO.bits.rlast   := true.B
    icache.io.axiWaddrIO.ready   := false.B
    icache.io.axiWdataIO.ready   := false.B
    icache.io.axiBrespIO.valid   := false.B
    icache.io.axiBrespIO.bits.bresp   := 0.U

    dcache.io.axiRaddrIO.ready   := false.B
    dcache.io.axiRdataIO.valid   := false.B
    dcache.io.axiRdataIO.bits.rdata := 0.U
    dcache.io.axiRdataIO.bits.rlast   := true.B
    dcache.io.axiRdataIO.bits.rresp   := 0.U
    dcache.io.axiWaddrIO.ready   := false.B
    dcache.io.axiWdataIO.ready   := false.B
    dcache.io.axiBrespIO.valid   := false.B
    dcache.io.axiBrespIO.bits.bresp   := 0.U
  }
  
  // ram, dataArray
  icache.io.sram0_rdata <> io.sram0_rdata
  icache.io.sram0_cen <> io.sram0_cen
  icache.io.sram0_wen <> io.sram0_wen
  icache.io.sram0_wmask <> io.sram0_wmask
  icache.io.sram0_addr <> io.sram0_addr
  icache.io.sram0_wdata <> io.sram0_wdata

  icache.io.sram1_rdata <> io.sram1_rdata
  icache.io.sram1_cen <> io.sram1_cen
  icache.io.sram1_wen <> io.sram1_wen
  icache.io.sram1_wmask <> io.sram1_wmask
  icache.io.sram1_addr <> io.sram1_addr
  icache.io.sram1_wdata <> io.sram1_wdata

  icache.io.sram2_rdata <> io.sram2_rdata
  icache.io.sram2_cen <> io.sram2_cen
  icache.io.sram2_wen <> io.sram2_wen
  icache.io.sram2_wmask <> io.sram2_wmask
  icache.io.sram2_addr <> io.sram2_addr
  icache.io.sram2_wdata <> io.sram2_wdata

  icache.io.sram3_rdata <> io.sram3_rdata
  icache.io.sram3_cen <> io.sram3_cen
  icache.io.sram3_wen <> io.sram3_wen
  icache.io.sram3_wmask <> io.sram3_wmask
  icache.io.sram3_addr <> io.sram3_addr
  icache.io.sram3_wdata <> io.sram3_wdata

  // dcache
  dcache.io.sram4_rdata <> io.sram4_rdata
  dcache.io.sram4_cen <> io.sram4_cen
  dcache.io.sram4_wen <> io.sram4_wen
  dcache.io.sram4_wmask <> io.sram4_wmask
  dcache.io.sram4_addr <> io.sram4_addr
  dcache.io.sram4_wdata <> io.sram4_wdata

  dcache.io.sram5_rdata <> io.sram5_rdata
  dcache.io.sram5_cen <> io.sram5_cen
  dcache.io.sram5_wen <> io.sram5_wen
  dcache.io.sram5_wmask <> io.sram5_wmask
  dcache.io.sram5_addr <> io.sram5_addr
  dcache.io.sram5_wdata <> io.sram5_wdata

  dcache.io.sram6_rdata <> io.sram6_rdata
  dcache.io.sram6_cen <> io.sram6_cen
  dcache.io.sram6_wen <> io.sram6_wen
  dcache.io.sram6_wmask <> io.sram6_wmask
  dcache.io.sram6_addr <> io.sram6_addr
  dcache.io.sram6_wdata <> io.sram6_wdata

  dcache.io.sram7_rdata <> io.sram7_rdata
  dcache.io.sram7_cen <> io.sram7_cen
  dcache.io.sram7_wen <> io.sram7_wen
  dcache.io.sram7_wmask <> io.sram7_wmask
  dcache.io.sram7_addr <> io.sram7_addr
  dcache.io.sram7_wdata <> io.sram7_wdata

  ifu.io.out        <> idReg.io.in
  ifu.io.redirect   <> exu.io.redirect
  ifu.io.stall      <> ctrl.io.stallPc
  ifu.io.valid      <> idReg.io.validIn
  ifu.io.icacheCtrl <> icache.io.ctrlIO
  ifu.io.icacheRead <> icache.io.rdataIO
  ifu.io.icacheWrite<> icache.io.wdataIO
  exReg.io.validIn  <> idReg.io.validOut
  lsReg.io.validIn  <> exReg.io.validOut
  wbReg.io.validIn  <> lsReg.io.validOut

  // 控制模块
  ctrl.io.ifuReady  <> ifu.io.ready
  ctrl.io.lsuReady  <> lsu.io.ready
  ctrl.io.ifuGrant  := arbiter.io.grantIfu
  ctrl.io.lsuGrant  := arbiter.io.grantLsu
  ctrl.io.branch    := exu.io.redirect.valid
  ctrl.io.stallIduReg <> idReg.io.stall
  ctrl.io.stallExuReg <> exReg.io.stall
  ctrl.io.stallLsuReg <> lsReg.io.stall
  ctrl.io.flushIduReg <> idReg.io.flush
  ctrl.io.flushWbuReg <> wbReg.io.flush
  ctrl.io.flushExuReg <> exReg.io.flush

  forward.io.lsuRd  := lsReg.io.out.rd
  forward.io.wbuRd  := wbReg.io.out.rd
  forward.io.lsuRegWen := lsReg.io.out.regWen
  forward.io.wbuRegWen := wbReg.io.out.regWen
  forward.io.rs1    := exReg.io.ctrlOut.rs1
  forward.io.rs2    := exReg.io.ctrlOut.rs2
  forward.io.wbuCsrWen := wbReg.io.out.csrWen
  forward.io.wbuCsrAddr := wbReg.io.out.csrWaddr
  forward.io.csrRaddr := exReg.io.ctrlOut.csrWaddr

  idu.io.in         <> idReg.io.out
  idu.io.regWrite   <> wbu.io.regWrite
  idu.io.csrWrite   <> wbu.io.csrWrite
  idu.io.datasrc    <> exReg.io.datasrcIn
  idu.io.aluCtrl    <> exReg.io.aluCtrlIn
  idu.io.ctrl       <> exReg.io.ctrlIn

  exu.io.datasrc    <> exReg.io.datasrcOut
  exu.io.aluCtrl    <> exReg.io.aluCtrlOut
  exu.io.ctrl       <> exReg.io.ctrlOut
  exu.io.lsuForward := lsReg.io.out.exuOut
  exu.io.wbuForward := wbu.io.regWrite.value // 可能三lsuout或者exuout
  exu.io.csrForward := wbReg.io.out.csrValue
  exu.io.out        <> lsReg.io.in
  exu.io.forwardA   <> forward.io.forwardA
  exu.io.forwardB   <> forward.io.forwardB

  lsu.io.dcacheCtrl <> dcache.io.ctrlIO
  lsu.io.dcacheRead <> dcache.io.rdataIO
  lsu.io.dcacheWrite<> dcache.io.wdataIO
  lsu.io.in         <> lsReg.io.out
  lsu.io.out        <> wbReg.io.in

  wbReg.io.lsFlagIn <> lsu.io.lsFlag
  io.lsFlag         <> wbReg.io.lsFlagOut // 仿真环境
  wbu.io.in         <> wbReg.io.out

  idReg.io.debugIn  <> ifu.io.debug
  exReg.io.debugIn  <> idReg.io.debugOut
  exu.io.debugIn    <> exReg.io.debugOut
  lsReg.io.debugIn  <> exu.io.debugOut
  wbReg.io.debugIn  <> lsReg.io.debugOut
  io.debug          <> wbReg.io.debugOut
  io.finish         <> wbReg.io.validOut
}