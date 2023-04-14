package jzcore

import chisel3._
import chisel3.util._
import utils._

class JzCore extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug       = new DebugIO
    val finish      = Output(Bool())

    // axi访存接口
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    
  })

  val ifu     = Module(new IFU)
  val idu     = Module(new IDU)
  val exu     = Module(new EXU)
  val lsu     = Module(new LSU)
  val wbu     = Module(new WBU)
  val ctrl    = Module(new CTRL)
  val arbiter = Module(new AxiArbiter) // todo:仲裁器

  val idReg   = Module(new ID_REG)
  val exReg   = Module(new EX_REG)
  val lsReg   = Module(new LS_REG)
  val wbReg   = Module(new WB_REG)
  val forward = Module(new Forwarding)

  // 仲裁
  arbiter.io.ifuReq   <> ifu.io.axiReq
  arbiter.io.grantIfu <> ifu.io.axiGrant
  arbiter.io.ifuReady <> ifu.io.axiReady
  arbiter.io.lsuReq   <> lsu.io.axiReq
  arbiter.io.grantLsu <> lsu.io.axiGrant
  arbiter.io.lsuReady <> lsu.io.axiReady

  // axi访问接口
  val grant = Cat(arbiter.io.grantIfu, arbiter.io.grantLsu)
  when (grant === 2.U) {
    io.axiRaddrIO <> ifu.io.axiRaddrIO
    io.axiRdataIO <> ifu.io.axiRdataIO
    io.axiWaddrIO <> ifu.io.axiWaddrIO
    io.axiWdataIO <> ifu.io.axiWdataIO
    io.axiBrespIO <> ifu.io.axiBrespIO

    lsu.io.axiRaddrIO.ready   := false.B
    lsu.io.axiRdataIO.valid   := false.B
    lsu.io.axiRdataIO.bits.rdata := 0.U
    lsu.io.axiRdataIO.bits.rresp   := 0.U
    lsu.io.axiWaddrIO.ready   := false.B
    lsu.io.axiWdataIO.ready   := false.B
    lsu.io.axiBrespIO.valid   := false.B
    lsu.io.axiBrespIO.bits.bresp   := 0.U
  }
  .elsewhen (grant === 1.U || grant === 3.U) {
    io.axiRaddrIO <> lsu.io.axiRaddrIO
    io.axiRdataIO <> lsu.io.axiRdataIO
    io.axiWaddrIO <> lsu.io.axiWaddrIO
    io.axiWdataIO <> lsu.io.axiWdataIO
    io.axiBrespIO <> lsu.io.axiBrespIO

    ifu.io.axiRaddrIO.ready   := false.B
    ifu.io.axiRdataIO.valid   := false.B
    ifu.io.axiRdataIO.bits.rdata := 0.U
    ifu.io.axiRdataIO.bits.rresp   := 0.U
    ifu.io.axiWaddrIO.ready   := false.B
    ifu.io.axiWdataIO.ready   := false.B
    ifu.io.axiBrespIO.valid   := false.B
    ifu.io.axiBrespIO.bits.bresp   := 0.U
  }
  .otherwise {
    // 没有请求
    io.axiRaddrIO.valid       := false.B
    io.axiRaddrIO.bits.addr   := 0.U
    io.axiRdataIO.ready       := false.B
    io.axiWaddrIO.valid       := false.B
    io.axiWaddrIO.bits.addr   := 0.U
    io.axiWdataIO.valid       := false.B
    io.axiWdataIO.bits.wdata  := 0.U
    io.axiWdataIO.bits.wstrb  := 0.U
    io.axiBrespIO.ready       := false.B

    ifu.io.axiRaddrIO.ready   := false.B
    ifu.io.axiRdataIO.valid   := false.B
    ifu.io.axiRdataIO.bits.rdata := 0.U
    ifu.io.axiRdataIO.bits.rresp   := 0.U
    ifu.io.axiWaddrIO.ready   := false.B
    ifu.io.axiWdataIO.ready   := false.B
    ifu.io.axiBrespIO.valid   := false.B
    ifu.io.axiBrespIO.bits.bresp   := 0.U

    lsu.io.axiRaddrIO.ready   := false.B
    lsu.io.axiRdataIO.valid   := false.B
    lsu.io.axiRdataIO.bits.rdata := 0.U
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
  
  ifu.io.out        <> idReg.io.in
  ifu.io.redirect   <> exu.io.redirect
  ifu.io.stall      <> ctrl.io.stallPc
  ifu.io.valid      <> idReg.io.validIn
  exReg.io.validIn  <> idReg.io.validOut
  lsReg.io.validIn  <> exReg.io.validOut
  wbReg.io.validIn  <> lsReg.io.validOut

  // 控制模块
  ctrl.io.ifuReady  <> ifu.io.ready
  ctrl.io.lsuReady  <> lsu.io.ready
  ctrl.io.branch    := exu.io.redirect.valid
  ctrl.io.stallIduReg <> idReg.io.stall
  ctrl.io.stallExuReg <> exReg.io.stall
  ctrl.io.stallLsuReg <> lsReg.io.stall
  ctrl.io.flushIduReg <> idReg.io.flush
  ctrl.io.flushWbuReg <> wbReg.io.flush

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

  exu.io.datasrc    <> idu.io.datasrcOut
  exu.io.aluCtrl    <> idu.io.aluCtrlOut
  exu.io.ctrl       <> idu.io.ctrlOut
  exu.io.lsuForward := lsReg.io.out.exuOut
  exu.io.wbuForward := wbu.io.regWrite.value // 可能三lsuout或者exuout
  exu.io.csrForward := wbReg.io.out.csrValue
  exu.io.out        <> lsReg.io.in
  exu.io.forwardA   <> forward.io.forwardA
  exu.io.forwardB   <> forward.io.forwardB

  lsu.io.in         <> lsReg.io.out
  lsu.io.out        <> wbReg.io.in

  wbu.io.in         <> wbReg.io.out

  io.debug          <> ifu.io.debug
  io.finish         <> wbReg.io.validOut
}