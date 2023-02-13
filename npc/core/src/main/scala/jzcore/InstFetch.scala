package jzcore

import chisel3._
import chisel3.util._


class InstFetch extends BlackBox {
  val io = IO(new Bundle {
    val pc      = Input(UInt(64.W))
    val inst    = Output(UInt(32.W))
  })
}