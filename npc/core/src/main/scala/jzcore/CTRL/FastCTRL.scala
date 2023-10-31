package jzcore

import chisel3._
import chisel3.util._

// 集中式控制模块
class FastCTRL extends Module {
  val io = IO(new Bundle {
    val lsuReady    = Input(Bool())
    val exuReady    = Input(Bool())

    val exuCsr      = Input(Bool())
    val lsuCsr      = Input(Bool())
    val wbuCsr      = Input(Bool())

    // 分支指令需要flush流水线
    val branch      = Input(Bool())
  
    // stall pipline reg and pc
    val stallPc     = Output(Bool())
    val stallIduReg = Output(Bool())
    val stallExuReg = Output(Bool())
    val stallLsuReg = Output(Bool())
    val stallWbuReg = Output(Bool())
    val stallExu    = Output(Bool())

    // flush pipline reg
    val flushIduReg = Output(Bool())
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
  io.stallPc     := !io.lsuReady | (loadUse & !io.branch) | !io.exuReady | io.exuCsr | io.lsuCsr | io.wbuCsr
  io.stallIduReg := !io.lsuReady | (loadUse & !io.branch) | !io.exuReady | io.exuCsr | io.lsuCsr | io.wbuCsr
  io.stallExuReg := !io.lsuReady | !io.exuReady
  io.stallLsuReg := !io.lsuReady | !io.exuReady
  io.stallWbuReg := !io.lsuReady | !io.exuReady
  io.stallExu    := !io.lsuReady

  // 当取指未完成或者发现是分支指令时flush idu_reg
  io.flushIduReg := io.branch
  io.flushExuReg := loadUse | io.exuCsr | io.lsuCsr | io.wbuCsr
}

