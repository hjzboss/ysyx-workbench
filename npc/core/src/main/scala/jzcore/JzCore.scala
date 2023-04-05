package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug       = new DebugIO

    // axi访存接口
    val axiRaddrIO  = new Decoupled(new AddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val ctrl= Module(new Ctrl)

  ifu.io.redirect   <> exu.io.redirect
  ifu.io.fetchReady <> ctrl.io.fetchReady
  ifu.io.axiAddrIO  <> io.axiRaddrIO
  ifu.io.axiDataIO  <> io.axiRdataIO

  idu.io.in         <> ifu.io.out
  idu.io.regWrite   <> exu.io.regWrite
  idu.io.csrWrite   <> exu.io.csrWrite
  idu.io.stall      <> ctrl.io.stallIdu

  exu.io.datasrc    <> idu.io.datasrc
  exu.io.aluCtrl    <> idu.io.aluCtrl
  exu.io.ctrl       <> idu.io.ctrl

  io.debug          <> ifu.io.debug
}