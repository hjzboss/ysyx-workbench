package jzcore

import chisel3._
import chisel3.util._

class Top extends Module {
  val io = IO(new Bundle {
    val pc    = Output(UInt(64.W))
    val inst  = Input(UInt(32.W))
  })

  val core = Module(new JzCore)

  io.pc := core.io.pc
  core.io.inst := io.inst
}