package jzcore

import chisel3._
import chisel3.util._

// 旁路单元
class Forwarding extends Module {
  val io = IO(new Bundle {
    val isBr      = Input(Bool()) // idu, 检测出分支指令
    val exuRd     = Input(UInt(5.W))
    val exuRegWen = Input(Bool())
    val brUse     = Output(Bool()) // 分支指令依赖于执行阶段的计算结果

    // 目的寄存器编号
    val lsuRd     = Input(UInt(5.W))
    val wbuRd     = Input(UInt(5.W))

    val lsuRegWen = Input(Bool())
    val wbuRegWen = Input(Bool())

    // 译码阶段的源寄存器编号
    val idRs1    = Input(UInt(5.W))
    val idRs2    = Input(UInt(5.W))
    // 执行阶段的源寄存器编号
    val exRs1    = Input(UInt(5.W))
    val exRs2    = Input(UInt(5.W))

    // 旁路控制信号，用于控制alu的两个源操作数
    val idForwardA = Output(Forward())
    val idForwardB = Output(Forward())

    val exForwardA = Output(Forward())
    val exForwardB = Output(Forward())
  })

  // 当译码阶段分支计算的源操作数依赖于执行阶段的结果时停顿一拍，只旁路访存阶段和写回阶段的结果（利用流水线寄存器防止亚稳态）
  val brUse1 = io.exuRegWen && io.exuRd =/= 0.U(5.W) && io.exuRd === io.idRs1
  val brUse2 = io.exuRegWen && io.exuRd =/= 0.U(5.W) && io.exuRd === io.idRs2
  io.brUse := io.isBr & (brUse1 | brUse2)

  // idu阶段的旁路
  val idForwardALsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.idRs1
  val idForwardBLsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.idRs2
  val idForwardAWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.idRs1
  val idForwardBWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.idRs2
  io.idForwardA := Mux(idForwardALsu, Forward.lsuData, Mux(idForwardAWbu, Forward.wbuData, Forward.normal))
  io.idForwardB := Mux(idForwardBLsu, Forward.lsuData, Mux(idForwardBWbu, Forward.wbuData, Forward.normal))

  // exu阶段的旁路
  val exForwardALsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.exRs1
  val exForwardBLsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.exRs2
  val exForwardAWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.exRs1
  val exForwardBWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.exRs2
  io.exForwardA := Mux(exForwardALsu, Forward.lsuData, Mux(exForwardAWbu, Forward.wbuData, Forward.normal))
  io.exForwardB := Mux(exForwardBLsu, Forward.lsuData, Mux(exForwardBWbu, Forward.wbuData, Forward.normal))
}