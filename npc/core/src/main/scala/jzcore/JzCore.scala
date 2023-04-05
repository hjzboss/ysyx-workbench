package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug       = new DebugIO

    // axi访存接口:ifu
    val axiRaddrIO  = Decoupled(new AddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))

    // axi访存接口:lsu
    val raddrIO     = Decoupled(new AddrIO)
    val rdataIO     = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new AddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val lsu = Module(new LSU)
  val wbu = Module(new WBU)
  val ctrl= Module(new CTRL)

  ifu.io.redirect   <> exu.io.redirect
  ifu.io.fetchReady <> ctrl.io.fetchReady
  ifu.io.axiAddrIO  <> io.axiRaddrIO
  ifu.io.axiDataIO  <> io.axiRdataIO
  ifu.io.stall      <> ctrl.io.stallIfu

  idu.io.in         <> ifu.io.out
  idu.io.regWrite   <> wbu.io.regWrite
  idu.io.csrWrite   <> wbu.io.csrWrite
  idu.io.stall      <> ctrl.io.stallIdu

  exu.io.datasrc    <> idu.io.datasrc
  exu.io.aluCtrl    <> idu.io.aluCtrl
  exu.io.ctrl       <> idu.io.ctrl

  lsu.io.in         <> exu.io.out
  lsu.io.lsuReady   <> ctrl.io.lsuReady
  lsu.io.stall      <> ctrl.io.stallLsu
  lsu.io.raddrIO    <> io.raddrIO
  lsu.io.rdataIO    <> io.rdataIO
  lsu.io.waddrIO    <> io.axiWaddrIO
  lsu.io.wdataIO    <> io.axiWdataIO
  lsu.io.brespIO    <> io.axiBrespIO

  wbu.io.in         <> lsu.io.out

  io.debug          <> ifu.io.debug
}