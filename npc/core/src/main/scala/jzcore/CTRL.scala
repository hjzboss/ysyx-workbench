package jzcore

import chisel3._
import top.Settings
import chisel3.util._

class CTRL extends Module {
  val io = IO(new Bundle {
    val fetchReady = Input(Bool())
    val lsuReady   = Input(Bool())

    val stallIfu   = Output(Bool())
    val stallIdu   = Output(Bool())
    val stallLsu   = Output(Bool())
    //val stallExu   = Output(Bool())
  })

  //todo:lsu的停顿信号
  io.stallIfu := !io.lsuReady
  io.stallIdu := !io.fetchReady || !io.lsuReady
  io.stallLsu := !io.fetchReady
  //io.stallExu := io.fetchReady
}