package jzcore

import chisel3._
import chisel3.util._


class CsrReg extends BlackBox {
  val io = IO(new Bundle {
    val clock     = Input(Clock())
    val reset     = Input(Bool())

    // exception
    val exception = Input(Bool())
    val epc       = Input(UInt(64.W))
    val no        = Input(UInt(4.W))

    val raddr     = Input(UInt(3.W))
    val rdata     = Output(UInt(64.W))

    val waddr     = Input(UInt(3.W))
    val wen       = Input(Bool())
    val wdata     = Input(UInt(64.W))
  })
}