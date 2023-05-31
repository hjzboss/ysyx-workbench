/*
package jzcore

import chisel3._
import chisel3.util._
import utils._


class IcArbiter extends Module {
  val io = IO(new Bundle {
    val stage3addr = Input(UInt(6.W))
    val stage1Addr = Input(UInt(6.W))
    val stage3Cen  = Input(Bool())
    val stage1Cen  = Input(Bool())
    val stage3Wen  = Input(Bool())
    val stage1Wen  = Input(Bool())

    val arbAddr    = Output(UInt(6.W))
    val arbCen     = Output(Bool())
    val arbWen     = Output(Bool())
  })

  io.arbAddr     := Mux(io.stage3Cen, io.stage1Addr, io.stage3Addr)
  io.arbCen      := Mux(io.stage3Cen, io.stage1Cen, io.stage3Cen)
  io.arbWen      := Mux(io.stage3Wen, io.stage1Wen, io.stage3Wen)
}
*/