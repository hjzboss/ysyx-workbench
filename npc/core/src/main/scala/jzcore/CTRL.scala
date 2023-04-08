package jzcore

import chisel3._
import top.Settings
import chisel3.util._

class CTRL extends Module {
  val io = IO(new Bundle {
    val ifuReady = Input(Bool())
    val iduReady = Input(Bool())
    val exuReady = Input(Bool())
    val lsuReady = Input(Bool())
    val wbuReady = Input(Bool())

    val pcEnable = Output(Bool())

    // 传给仿真环境，单周期有用
    val finish   = Output(Bool())
  })

  //io.stallIfu := io.lsuTrans
  //io.stallLsu := !io.fetchReady

  io.pcEnable := io.iduReady && io.exuReady && io.lsuReady && io.wbuReady

  io.finish   := io.pcEnable && io.ifuReady
}