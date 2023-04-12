package jzcore

import chisel3._
import top.Settings
import chisel3.util._

// 旁路单元
class Forward extends Module {
  val io = IO(new Bundle {
    // 目的寄存器编号
    val lsuRd    = Input(UInt(5.W))
    val wbuRd    = Input(UInt(5.W))
    // 执行阶段的源寄存器编号
    val rs1      = Input(UInt(5.W))
    val rs2      = Input(UInt(5.W))

    // 旁路控制信号，用于控制alu的两个源操作数
    val forwardA = Output(UInt(2.W))
    val forwardB = Output(UInt(2.W))
  })

  io.forwardA := Mux(io.lsuRd === io.rs1, Forward.lsuData, Mux(io.wbuData === io.rs1, Forward.wbuData, Forward.normal))
  io.forwardB := Mux(io.lsuRd === io.rs2, Forward.lsuData, Mux(io.wbuData === io.rs2, Forward.wbuData, Forward.normal))
} 