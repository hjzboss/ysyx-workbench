package jzcore

import chisel3._
import chisel3.util._

class JzCore extends Module {
  val io = IO(new Bundle {
    // 仿真环境
    val pc      = Output(UInt(64.W))
    val nextPc  = Output(UInt(64.W))
    val inst    = Output(UInt(32.W))
    val difftest= Output(Bool())

    // axi接口
    // read channel
    val raddrIO = Decoupled(new AddrIO)
    val rdataIO = Flipped(Decoupled(new DataIO))
    // write channel
  })

  val ifu   = Module(new IFU)
  val idu   = Module(new IDU)
  val exu   = Module(new EXU)
  // todo：仲裁器

  ifu.io.out      <> idu.io.in
  ifu.io.redirect <> exu.io.redirect

  io.raddrIO      <> ifu.io.addrIO
  io.rdataIO      <> ifu.io.dataIO

  idu.io.regWrite <> exu.io.regWrite
  idu.io.csrWrite <> exu.io.csrWrite

  exu.io.in       <> idu.io.out

  // 仿真环境所需
  io.inst         := ifu.io.inst
  io.pc           := ifu.io.pc
  io.nextPc       := ifu.io.nextPc
  io.difftest     := exu.io.difftest
}