package jzcore

import chisel3._
import chisel3.util._
import utils._

class Pmem extends BlackBox {
  val io = IO(new Bundle {
    val raddr   = Input(UInt(64.W))
    val rdata   = Output(UInt(64.W))
    val rvalid  = Input(Bool())

    val pmem    = Input(Bool())
    val waddr   = Input(UInt(64.W))
    val wdata   = Input(UInt(64.W))
    val mask    = Input(UInt(8.W))
  })
}
