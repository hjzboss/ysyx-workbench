package jzcore

import chisel3._
import top.Settings
import chisel3.util._

trait HasResetVector {
  val resetVector = Settings.getLong("TestVector")
}

class IFU extends Module with HasResetVector{
  val io = IO(new Bundle {
    val pc        = Output(UInt(64.W))
    val inst      = Input(UInt(32.W))
    val redirect  = Flipped(Decoupled(new RedirectIO))
    val fetch     = new InstrFetch
  })

  // pc
  val pc  = RegInit(resetVector.U(64.W))
  val npc = Wire(UInt(64.W))

  val snpc = pc + 4.U
  val dnpc = io.redirect.brAddr

  npc := Mux(io.redirect.valid, dnpc, snpc)
  pc  := npc

  io.pc         := pc
  io.fetch.pc   := pc
  io.fetch.inst := io.inst
}