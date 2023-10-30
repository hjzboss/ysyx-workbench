package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings


class FastCore extends Module {
  val io = IO(new Bundle {
    val debug           = if(Settings.get("sim")) Some(new DebugIO) else None
    val lsFlag          = if(Settings.get("sim")) Some(Output(Bool())) else None

    val interrupt      = Input(Bool())
  })

  val ifu     = Module(new FastIFU)
  val idu     = Module(new IDU)
  val exu     = Module(new EXU)
  val lsu     = Module(new FastLSU)
  val wbu     = Module(new WBU)
  val ctrl    = Module(new FastCTRL)
  val clint   = Module(new Clint)

  val idReg   = Module(new ID_REG)
  val exReg   = Module(new EX_REG)
  val lsReg   = Module(new LS_REG)
  val wbReg   = Module(new WB_REG)
  val forward = Module(new Forwarding)

  clint.io.clintIO    <> lsu.io.clintIO
  clint.io.int        <> idu.io.timerInt

  ifu.io.out          <> idReg.io.in
  ifu.io.exuRedirect  <> exu.io.redirect
  ifu.io.valid        <> idReg.io.validIn
  idReg.io.validOut   <> idu.io.validIn

  // 控制模块
  ctrl.io.exuCsr      := exReg.io.ctrlOut.csrChange
  ctrl.io.lsuCsr      := lsReg.io.out.csrChange
  ctrl.io.wbuCsr      := wbReg.io.out.csrChange
  ctrl.io.memRen      := exReg.io.ctrlOut.memRen
  ctrl.io.exRd        := exReg.io.ctrlOut.rd
  ctrl.io.rs1         := idu.io.ctrl.rs1
  ctrl.io.rs2         := idu.io.ctrl.rs2
  ctrl.io.lsuReady    <> lsu.io.ready
  ctrl.io.exuReady    <> exu.io.ready
  ctrl.io.branch      := exu.io.redirect.valid
  idReg.io.stall      := ctrl.io.stallIduReg
  idu.io.stall        := ctrl.io.stallIduReg
  ctrl.io.stallExuReg <> exReg.io.stall
  ctrl.io.stallLsuReg <> lsReg.io.stall
  ctrl.io.stallWbuReg <> wbReg.io.stall
  ctrl.io.stallExu    <> exu.io.stall
  ctrl.io.stallPc     <> ifu.io.stall
  idReg.io.flush      := ctrl.io.flushIduReg
  idu.io.flush        := ctrl.io.flushIduReg
  idReg.io.flush      <> ctrl.io.flushIduReg
  exReg.io.flush      := ctrl.io.flushExuReg
  exu.io.flush        := ctrl.io.flushExuReg

  forward.io.lsuRd  := lsReg.io.out.rd
  forward.io.wbuRd  := wbReg.io.out.rd
  forward.io.lsuRegWen := lsReg.io.out.regWen
  forward.io.wbuRegWen := wbReg.io.out.regWen
  forward.io.rs1    := exReg.io.ctrlOut.rs1
  forward.io.rs2    := exReg.io.ctrlOut.rs2

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
  exu.io.wbuForward := wbu.io.regWrite.value
  exu.io.csrWbuForward := wbReg.io.out.csrValue
  exu.io.csrLsuForward := lsReg.io.out.csrValue
  exu.io.out        <> lsReg.io.in
  exu.io.forwardA   <> forward.io.forwardA
  exu.io.forwardB   <> forward.io.forwardB
  exu.io.wbuMepc    := wbReg.io.out.pc
  exu.io.lsuMepc    := lsReg.io.out.pc
  exu.io.lsuNo      := lsReg.io.out.excepNo
  exu.io.wbuNo      := wbReg.io.out.excepNo

  lsu.io.in         <> lsReg.io.out
  lsu.io.out        <> wbReg.io.in
  lsu.io.stall      := ctrl.io.stallLsuReg
  wbu.io.in         <> wbReg.io.out
  
  if(Settings.get("sim")) {
    wbReg.io.lsFlagIn.get <> lsu.io.lsFlag.get
    io.lsFlag.get         <> wbReg.io.lsFlagOut.get
    ifu.io.debug.get      <> idReg.io.debugIn.get
    exReg.io.debugIn.get  <> idReg.io.debugOut.get
    exu.io.debugIn.get    <> exReg.io.debugOut.get
    lsReg.io.debugIn.get  <> exu.io.debugOut.get
    wbReg.io.debugIn.get  <> lsReg.io.debugOut.get
    io.debug.get          <> wbReg.io.debugOut.get
  }
}