package jzcore

import chisel3._
import top.Settings
import chisel3.util._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class IFU extends Module with HasResetVector{
  val io = IO(new Bundle {
    val pc    = Output(UInt(64.W))
    val inst  = Input(UInt(32.W))

    val fetch = new InstrFetch
  })

  // pc
  val pc = RegInit(resetVector.U(64.W))
  val npc = Wire(UInt(64.W))
  val snpc = pc + 4.U
  npc := snpc
  pc := npc

  io.fetch.pc := pc
}