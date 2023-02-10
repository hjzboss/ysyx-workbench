package jzcore

import chisel3._
import top.Settings
import chisel3.util._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class IFU extends Module with HasResetVector{
  val io = IO(new Bundle {
    // 用于仿真环境
    val pc        = Output(UInt(64.W))
    val nextPc    = Output(UInt(64.W))
    val inst      = Output(UInt(32.W))

    val redirect  = Flipped(new RedirectIO)
    val fetch     = new InstrFetch
  })

  val instFetch = Module(new InstFetch)

  // pc
  val pc  = RegInit(resetVector.U(64.W))
  val npc = Wire(UInt(64.W))

  val snpc = pc + 4.U
  val dnpc = io.redirect.brAddr

  npc := Mux(io.redirect.valid, dnpc, snpc)
  pc  := npc

  instFetch.pc  := pc
  
  io.inst       := instFetch.inst
  io.nextPc     := npc
  io.pc         := pc

  io.fetch.pc   := pc
  io.fetch.inst := instFetch.inst
}