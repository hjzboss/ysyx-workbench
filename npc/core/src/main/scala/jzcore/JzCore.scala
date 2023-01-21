package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    val pc    = Output(UInt(64.W))
    val inst  = Input(UInt(32.W))

    val datasrc = new DataSrcIO
    val aluCtrl = new AluIO
    val ctrl    = new Ctrl

    val regWrite = new RFWriteIO
    val branch   = new BranchCtrl
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)

  io.pc           := ifu.io.pc
  ifu.io.inst     := io.inst
  ifu.io.branch   := exu.io.branch

  idu.io.fetch    := ifu.io.fetch
  idu.io.regWrite := exu.io.regWrite

  exu.io.datasrc  := idu.io.datasrc
  exu.io.aluCtrl  := idu.io.aluCtrl
  exu.io.ctrl     := idu.io.ctrl

  io.branch       := exu.io.branch
  io.regWrite     := exu.io.regWrite
  io.datasrc      := idu.io.datasrc
  io.aluCtrl      := idu.io.aluCtrl
  io.ctrl         := idu.io.ctrl
}