package jzcore

import chisel3._
import chisel3.util._
import utils._

class IMEM extends BlackBox {
  val io = IO(new Bundle {
    val pc      = Input(UInt(32.W))
    val inst    = Output(UInt(32.W))
  })
}
