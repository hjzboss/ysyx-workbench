package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    val pc    = Output(UInt(64.W))
    val inst  = Input(UInt(32.W))
  })

  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)

  io.pc := ifu.io.pc
  ifu.inst := io.inst

  ifu.io <> idu.io
  idu.io <> exu.io
}