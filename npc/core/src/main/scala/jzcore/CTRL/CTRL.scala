package jzcore

import chisel3._
import chisel3.util._

// 集中式控制模块
class CTRL extends Module {
  val io = IO(new Bundle {
    val icStall     = Input(Bool())
    val lsuReady    = Input(Bool())
    val exuReady    = Input(Bool())

    val exuCsr      = Input(Bool())
    val lsuCsr      = Input(Bool())
    val wbuCsr      = Input(Bool())

    // 分支指令需要flush流水线
    val branch      = Input(Bool())
  
    // stall pipline reg and pc
    val stallPc     = Output(Bool())
    val stallICache = Output(Bool())
    val stallIduReg = Output(Bool())
    val stallExuReg = Output(Bool())
    val stallLsuReg = Output(Bool())
    val stallWbuReg = Output(Bool())
    val stallExu    = Output(Bool())

    // flush pipline reg
    val flushICache = Output(Bool())
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

  // 当取指未完成时停顿之前所有阶段，当前面指令有csr操作是时候停顿后面阶段的指令
  io.stallICache := !io.lsuReady | (loadUse & !io.branch) | !io.exuReady | (io.exuCsr & !io.branch) | io.lsuCsr | io.wbuCsr
  io.stallPc     := !io.lsuReady | (loadUse & !io.branch) | (io.icStall & !io.branch) | !io.exuReady | (io.exuCsr & !io.branch) | io.lsuCsr | io.wbuCsr
  io.stallIduReg := !io.lsuReady | (loadUse & !io.branch) | !io.exuReady | (io.exuCsr & !io.branch) | io.lsuCsr | io.wbuCsr
  io.stallExuReg := !io.lsuReady | !io.exuReady
  io.stallLsuReg := !io.lsuReady | !io.exuReady
  io.stallWbuReg := !io.lsuReady | !io.exuReady
  io.stallExu    := !io.lsuReady

  // 当取指未完成或者发现是分支指令时flush idu_reg
  io.flushICache := io.branch
  io.flushIduReg := io.branch
  io.flushExuReg := io.branch | loadUse | io.exuCsr | io.lsuCsr | io.wbuCsr
}

