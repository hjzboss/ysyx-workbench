package jzcore

import chisel3._
import top.Settings
import chisel3.util._

// 集中式控制模块
class CTRL extends Module {
  val io = IO(new Bundle {
    // ifu和lsu都是多周期完成，因此需要ready信号来停顿对应的流水线
    val ifuReady = Input(Bool())
    val lsuReady = Input(Bool())

    // axi仲裁信号
    val ifuGrant = Input(Bool())
    val lsuGrant = Input(Bool())

    // 分支指令需要flush流水线
    val branch   = Input(Bool())

    // stall pipline reg and pc
    val stallPc     = Output(Bool())
    val stallIduReg = Output(Bool())
    val stallExuReg = Output(Bool())
    val stallLsuReg = Output(Bool())

    // flush pipline reg
    val flushIduReg = Output(Bool())
    val flushWbuReg = Output(Bool()) // set when lsu is unready
    val flushExuReg = Output(Bool()) // todo: 是否需要这个信号
  })
  
  // 当取指未完成时停顿之前所有阶段
  io.stallPc     := !io.lsuReady
  io.stallIduReg := !io.lsuReady
  io.stallExuReg := !io.lsuReady
  io.stallLsuReg := !io.lsuReady

  // 当取指未完成或者发现是分支指令时flush idu_reg
  io.flushIduReg := !io.ifuReady || io.branch
  io.flushExuReg := io.branch
  io.flushWbuReg := !io.lsuReady
}