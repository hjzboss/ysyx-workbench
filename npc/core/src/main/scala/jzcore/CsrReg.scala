package jzcore

import chisel3._
import chisel3.util._


class CsrReg extends BlackBox {
  val io = IO(new Bundle {
    val clock   = Input(Clock())
    val reset   = Input(Bool())

    val raddr   = Input(UInt(2.W))
    val csrSrc  = Output(UInt(64.W))

    val waddr   = Input(UInt(2.W))
    val wen     = Input(Bool())
    val wdata   = Input(UInt(64.W))
  })
}