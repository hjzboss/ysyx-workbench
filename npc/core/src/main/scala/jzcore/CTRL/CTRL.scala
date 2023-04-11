package jzcore

import chisel3._
import top.Settings
import chisel3.util._

// 集中式控制模块
class CTRL extends Module {
  val io = IO(new Bundle {
    val ifuReady = Input(Bool())
    //val iduReady = Input(Bool())
    //val exuReady = Input(Bool())
    val lsuReady = Input(Bool())
    val branch   = Input(Bool())
    //val wbuReady = Input(Bool())

    //val pcEnable = Output(Bool())

    // 传给仿真环境，单周期有用
    //val finish   = Output(Bool())

    // stall pipline reg
    val stallIfu    = Output(Bool()) // keep instruction
    val stallIduReg = Output(Bool())
    val stallExuReg = Output(Bool())
    val stallLsuReg = Output(Bool())

    // flush pipline reg
    val flushIduReg = Output(Bool())
    //val flushExuReg = Output(Bool())
    val flushWbuReg = Output(Bool()) // set when lsu is unready
  })

  //io.pcEnable := io.iduReady && io.exuReady && io.lsuReady && io.wbuReady // todo

  //io.finish   := io.iduReady && io.exuReady && io.lsuReady && io.wbuReady && io.ifuReady
  io.stallIfu := !io.lsuReady
  io.stallIduReg := !io.lsuReady
  io.stallExuReg := !io.lsuReady
  io.stallLsuReg := !io.lsuReady

  // todo: branch
  io.flushIduReg := !io.ifuReady
  io.flushWbuReg := !io.lsuReady
}