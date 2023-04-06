package jzcore

import chisel3._
import top.Settings
import chisel3.util._

class CTRL extends Module {
  val io = IO(new Bundle {
    val fetchReady = Input(Bool())
    val lsuReady   = Input(Bool())
    val lsuTrans   = Input(Bool())

    val stallIfu   = Output(Bool())
    val stallLsu   = Output(Bool())
  })

  io.stallIfu := io.lsuTrans
  io.stallLsu := !io.fetchReady
}