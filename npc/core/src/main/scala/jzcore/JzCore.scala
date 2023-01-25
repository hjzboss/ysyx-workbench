package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    val pc    = Output(UInt(64.W))
    val inst  = Input(UInt(32.W))

    //val datasrc = new DataSrcIO
    //val aluCtrl = new AluIO
    //val ctrl    = new Ctrl

    //val regWrite = new RFWriteIO
    //val redirect = new RedirectIO
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)

  ifu.io.inst     := io.inst
  ifu.io.redirect := exu.io.redirect

  idu.io.fetch    := ifu.io.fetch
  idu.io.regWrite := exu.io.regWrite

  exu.io.datasrc  := idu.io.datasrc
  exu.io.aluCtrl  := idu.io.aluCtrl
  exu.io.ctrl     := idu.io.ctrl

  io.pc           := ifu.io.pc
  //io.redirect     := exu.io.redirect
  //io.regWrite     := exu.io.regWrite
  //io.datasrc      := idu.io.datasrc
  //io.aluCtrl      := idu.io.aluCtrl
  //io.ctrl         := idu.io.ctrl
}