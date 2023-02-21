package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    val pc      = Output(UInt(64.W))
    val nextPc  = Output(UInt(64.W))
    val inst    = Output(UInt(32.W))

    val lsType  = Output(UInt(4.W))
    val aluOp   = Output(UInt(6.W))
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)

  ifu.io.redirect := exu.io.redirect

  idu.io.fetch    := ifu.io.fetch
  idu.io.regWrite := exu.io.regWrite

  exu.io.datasrc  := idu.io.datasrc
  exu.io.aluCtrl  := idu.io.aluCtrl
  exu.io.ctrl     := idu.io.ctrl

  io.inst         := ifu.io.inst
  io.pc           := ifu.io.pc
  io.nextPc       := ifu.io.nextPc
  
  io.lsType       := idu.io.lsType
  io.aluOp        := idu.io.aluOp
}