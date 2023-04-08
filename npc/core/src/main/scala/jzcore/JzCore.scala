package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug       = new DebugIO

    // axi访存接口:ifu
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))

    // axi访存接口:lsu
    val raddrIO     = Decoupled(new RaddrIO)
    val rdataIO     = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    

    val align     = Output(UInt(6.W))
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
  ifu.io.lsuReady   <> lsu.io.lsuReady

  idu.io.in         <> ifu.io.out
  idu.io.regWrite   <> wbu.io.regWrite
  idu.io.csrWrite   <> wbu.io.csrWrite

  exu.io.datasrc    <> idu.io.datasrc
  exu.io.aluCtrl    <> idu.io.aluCtrl
  exu.io.ctrl       <> idu.io.ctrl

  lsu.io.in         <> exu.io.out
  lsu.io.lsuTrans   <> ctrl.io.lsuTrans
  lsu.io.stall      <> ctrl.io.stallLsu
  lsu.io.raddrIO    <> io.raddrIO
  lsu.io.rdataIO    <> io.rdataIO
  lsu.io.waddrIO    <> io.axiWaddrIO
  lsu.io.wdataIO    <> io.axiWdataIO
  lsu.io.brespIO    <> io.axiBrespIO

  wbu.io.in         <> lsu.io.out

  io.debug          <> ifu.io.debug
  io.align          <> lsu.io.align
}