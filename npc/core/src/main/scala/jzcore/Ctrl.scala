package jzcore

import chisel3._
import top.Settings
import chisel3.util._

class Ctrl extends Module {
  val io = IO(new Bundle {
    val fetchReady = Input(Bool())
    // val lsuReady = Input(Bool())

    //val stallIfu   = Output(Bool())
    val stallIdu   = Output(Bool())
    //val stallExu   = Output(Bool())
  })

  //todo:lsu的停顿信号
  //io.stallIfu := false.B 
  io.stallIdu := !io.fetchReady
  //io.stallExu := io.fetchReady
}