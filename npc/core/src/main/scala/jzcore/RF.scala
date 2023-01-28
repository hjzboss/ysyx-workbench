package jzcore

import chisel3._
import chisel3.util._


class RF extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())

    val rs1   = Input(UInt(5.W))
    val rs2   = Input(UInt(5.W))
    val src1  = Output(UInt(64.W))
    val src2  = Output(UInt(64.W))

    val waddr = Input(UInt(5.W))
    val wen   = Input(Bool())
    val wdata = Input(UInt(64.W))
  })
}