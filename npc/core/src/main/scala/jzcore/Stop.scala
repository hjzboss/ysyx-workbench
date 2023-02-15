package jzcore

import chisel3._
import chisel3.util._

class Stop extends BlackBox {
    val io = IO(new Bundle {
      val valid = Input(Bool())
      val haltRet = Input(UInt(64.W))
    })
}