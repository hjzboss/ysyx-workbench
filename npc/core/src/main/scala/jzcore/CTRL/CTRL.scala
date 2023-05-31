package jzcore

import chisel3._
import top.Settings
import chisel3.util._

// 集中式控制模块
class CTRL extends Module {
  val io = IO(new Bundle {
    // ifu和lsu都是多周期完成，因此需要ready信号来停顿对应的流水线
    //val ifuReady = Input(Bool())
    val icStall     = Input(Bool())
    val lsuReady    = Input(Bool())

    // 分支指令需要flush流水线
    val branch      = Input(Bool())

    // stall pipline reg and pc
    val stallPc     = Output(Bool())
    val stallICache = Output(Bool())
    val stallIduReg = Output(Bool())
    val stallExuReg = Output(Bool())
    val stallLsuReg = Output(Bool())

    // flush pipline reg
    val flushICache = Output(Bool())
    val flushIduReg = Output(Bool())
    val flushWbuReg = Output(Bool()) // set when lsu is unready
    val flushExuReg = Output(Bool()) // todo: 是否需要这个信号

    // todo: load-use停顿处理
    val memRen      = Input(Bool()) // 来自exu
    val exRd        = Input(UInt(5.W))
    // 译码阶段的源寄存器编号
    val rs1         = Input(UInt(5.W))
    val rs2         = Input(UInt(5.W))
  })

  val loadUse     = dontTouch(WireDefault(false.B))
  loadUse        := io.memRen && (io.exRd === io.rs1 || io.exRd === io.rs2)

  // 当取指未完成时停顿之前所有阶段
  io.stallICache := !io.lsuReady | loadUse
  io.stallPc     := !io.lsuReady | io.icStall | loadUse
  io.stallIduReg := !io.lsuReady | loadUse
  io.stallExuReg := !io.lsuReady
  io.stallLsuReg := !io.lsuReady

  // 当取指未完成或者发现是分支指令时flush idu_reg
  io.flushICache := io.branch
  io.flushIduReg := io.branch
  io.flushExuReg := io.branch | loadUse
  io.flushWbuReg := !io.lsuReady // todo
}