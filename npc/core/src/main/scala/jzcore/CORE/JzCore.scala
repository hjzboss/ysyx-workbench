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

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val lsu = Module(new LSU)
  val wbu = Module(new WBU)
  val ctrl= Module(new CTRL)
  val arbiter = Module(new AxiArbiter) // todo:仲裁器

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
    lsu.io.axiBrespIO.bits.bresp   := false.B
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
    ifu.io.axiBrespIO.bits.bresp   := false.B
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
    ifu.io.axiBrespIO.bits.bresp   := false.B

    lsu.io.axiRaddrIO.ready   := false.B
    lsu.io.axiRdataIO.valid   := false.B
    lsu.io.axiRdataIO.bits.rresp   := 0.U
    lsu.io.axiWaddrIO.ready   := false.B
    lsu.io.axiWdataIO.ready   := false.B
    lsu.io.axiBrespIO.valid   := false.B
    lsu.io.axiBrespIO.bits.bresp   := false.B
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

  // 控制模块
  ctrl.io.ifuReady  <> ifu.io.ready
  ctrl.io.iduReady  <> idu.io.ready
  ctrl.io.exuReady  <> exu.io.ready
  ctrl.io.lsuReady  <> lsu.io.ready
  ctrl.io.wbuReady  <> wbu.io.ready

  ifu.io.redirect   <> exu.io.redirect
  ifu.io.pcEnable   <> ctrl.io.pcEnable

  idu.io.in         <> ifu.io.out
  idu.io.regWrite   <> wbu.io.regWrite
  idu.io.csrWrite   <> wbu.io.csrWrite

  exu.io.datasrc    <> idu.io.datasrc
  exu.io.aluCtrl    <> idu.io.aluCtrl
  exu.io.ctrl       <> idu.io.ctrl

  lsu.io.in         <> exu.io.out

  wbu.io.in         <> lsu.io.out

  io.debug          <> ifu.io.debug
  io.finish         <> ctrl.io.finish
}