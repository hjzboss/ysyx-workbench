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


    // axi访存接口
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))   

    //val csrAddr     = Output(UInt(3.W)) 

    val lsFlag      = Output(Bool())
  })

  val ifu     = Module(new IFU)
  val idu     = Module(new IDU)
  val exu     = Module(new EXU)
  val lsu     = Module(new LSU)
  val wbu     = Module(new WBU)
  val ctrl    = Module(new CTRL)
  val arbiter = Module(new AxiArbiter) // todo:仲裁器
  val icache  = Module(new Cache)

  val idReg   = Module(new ID_REG)
  val exReg   = Module(new EX_REG)
  val lsReg   = Module(new LS_REG)
  val wbReg   = Module(new WB_REG)
  val forward = Module(new Forwarding)

  //io.csrAddr  := idu.io.csrAddr
  // 仲裁
  arbiter.io.ifuReq   <> icache.io.axiReq
  //arbiter.io.ifuReq   <> ifu.io.axiReq
  arbiter.io.grantIfu <> icache.io.axiGrant
  arbiter.io.ifuReady <> icache.io.axiReady
  arbiter.io.lsuReq   <> lsu.io.axiReq
  arbiter.io.grantLsu <> lsu.io.axiGrant
  arbiter.io.lsuReady <> lsu.io.axiReady

  // axi访问接口
  val grant = Cat(arbiter.io.grantIfu, arbiter.io.grantLsu)
  when(grant === 2.U) {
    io.axiRaddrIO <> icache.io.axiRaddrIO
    io.axiRdataIO <> icache.io.axiRdataIO
    io.axiWaddrIO <> icache.io.axiWaddrIO
    io.axiWdataIO <> icache.io.axiWdataIO
    io.axiBrespIO <> icache.io.axiBrespIO

    lsu.io.axiRaddrIO.ready   := false.B
    lsu.io.axiRdataIO.valid   := false.B
    lsu.io.axiRdataIO.bits.rdata := 0.U
    lsu.io.axiRdataIO.bits.rresp   := 0.U
    lsu.io.axiRdataIO.bits.rlast   := true.B
    lsu.io.axiWaddrIO.ready   := false.B
    lsu.io.axiWdataIO.ready   := false.B
    lsu.io.axiBrespIO.valid   := false.B
    lsu.io.axiBrespIO.bits.bresp   := 0.U
  }
  .elsewhen(grant === 1.U || grant === 3.U) {
    io.axiRaddrIO <> lsu.io.axiRaddrIO
    io.axiRdataIO <> lsu.io.axiRdataIO
    io.axiWaddrIO <> lsu.io.axiWaddrIO
    io.axiWdataIO <> lsu.io.axiWdataIO
    io.axiBrespIO <> lsu.io.axiBrespIO

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

    lsu.io.axiRaddrIO.ready   := false.B
    lsu.io.axiRdataIO.valid   := false.B
    lsu.io.axiRdataIO.bits.rdata := 0.U
    lsu.io.axiRdataIO.bits.rlast   := true.B
    lsu.io.axiRdataIO.bits.rresp   := 0.U
    lsu.io.axiWaddrIO.ready   := false.B
    lsu.io.axiWdataIO.ready   := false.B
    lsu.io.axiBrespIO.valid   := false.B
    lsu.io.axiBrespIO.bits.bresp   := 0.U
  }
  
  
/*
  switch (grant) {
    is (0.U) {
      // 没有请求
      io.axiRaddrIO.valid       := false.B
      io.axiRdataIO.ready       := false.B
      io.axiWaddrIO.valid       := false.B
      io.axiWdataIO.bits.wdata  := 0.U
      io.axiWdataIO.bits.wstrb  := 0.U
      io.axiBrespIO.ready       := false.B

      ifu.io.axiRaddrIO.ready   := false.B
      ifu.io.axiRdataIO.valid   := false.B
      ifu.io.axiRdataIO.bits.rresp   := 0.U
      ifu.io.axiWaddrIO.ready   := false.B
      ifu.io.axiWdataIO.ready   := false.B
      ifu.io.axiBrespIO.valid   := false.B
      ifu.io.axiBrespIO.bits.bresp   := false.B

      lsu.io.axiRaddrIO.ready   := false.B
      lsu.io.axiRdataIO.valid   := false.B
      lsu.io.axiRdataIO.bits.rresp   := 0.U
      lsu.io.axiWaddrIO.ready   := false.B
      lsu.io.axiWdataIO.ready   := false.B
      lsu.io.axiBrespIO.valid   := false.B
      lsu.io.axiBrespIO.bits.bresp   := false.B
    }
    is (2.U) {
      io.axiRaddrIO <> ifu.io.axiRaddrIO
      io.axiRdataIO <> ifu.io.axiRdataIO
      io.axiWaddrIO <> ifu.io.axiWaddrIO
      io.axiWdataIO <> ifu.io.axiWdataIO
      io.axiBrespIO <> ifu.io.axiBrespIO

      lsu.io.axiRaddrIO.ready   := false.B
      lsu.io.axiRdataIO.valid   := false.B
      lsu.io.axiRdataIO.bits.rresp   := 0.U
      lsu.io.axiWaddrIO.ready   := false.B
      lsu.io.axiWdataIO.ready   := false.B
      lsu.io.axiBrespIO.valid   := false.B
      lsu.io.axiBrespIO.bits.bresp   := false.B
    }
    is (1.U) {
      io.axiRaddrIO <> lsu.io.axiRaddrIO
      io.axiRdataIO <> lsu.io.axiRdataIO
      io.axiWaddrIO <> lsu.io.axiWaddrIO
      io.axiWdataIO <> lsu.io.axiWdataIO
      io.axiBrespIO <> lsu.io.axiBrespIO

      ifu.io.axiRaddrIO.ready   := false.B
      ifu.io.axiRdataIO.valid   := false.B
      ifu.io.axiRdataIO.bits.rresp   := 0.U
      ifu.io.axiWaddrIO.ready   := false.B
      ifu.io.axiWdataIO.ready   := false.B
      ifu.io.axiBrespIO.valid   := false.B
      ifu.io.axiBrespIO.bits.bresp   := false.B
    }
    is (3.U) {
      io.axiRaddrIO <> lsu.io.axiRaddrIO
      io.axiRdataIO <> lsu.io.axiRdataIO
      io.axiWaddrIO <> lsu.io.axiWaddrIO
      io.axiWdataIO <> lsu.io.axiWdataIO
      io.axiBrespIO <> lsu.io.axiBrespIO

      ifu.io.axiRaddrIO.ready   := false.B
      ifu.io.axiRdataIO.valid   := false.B
      ifu.io.axiRdataIO.bits.rresp   := 0.U
      ifu.io.axiWaddrIO.ready   := false.B
      ifu.io.axiWdataIO.ready   := false.B
      ifu.io.axiBrespIO.valid   := false.B
      ifu.io.axiBrespIO.bits.bresp   := false.B
    }
  }
*/
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

  lsu.io.in         <> lsReg.io.out
  lsu.io.out        <> wbReg.io.in

  wbReg.io.lsFlagIn   <> lsu.io.lsFlag
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