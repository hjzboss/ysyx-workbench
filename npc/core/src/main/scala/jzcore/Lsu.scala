package jzcore

import chisel3._
import chisel3.util._


class Lsu extends BlackBox {
  val io = IO(new Bundle {
    val raddr = Input(UInt(64.W))
    val rdata = Output(UInt(64.W))

    val waddr = Input(UInt(64.W))
    val wdata = Input(UInt(64.W))
    val wmask = Input(UInt(8.W))
  })
}