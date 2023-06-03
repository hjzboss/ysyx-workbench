package jzcore

import chisel3._
import top.Settings
import chisel3.util._

// 旁路单元
class Forwarding extends Module {
  val io = IO(new Bundle {
    // 目的寄存器编号
    val lsuRd     = Input(UInt(5.W))
    val wbuRd     = Input(UInt(5.W))

    val lsuRegWen = Input(Bool())
    val wbuRegWen = Input(Bool())

    // 执行阶段的源寄存器编号
    val rs1       = Input(UInt(5.W))
    val rs2       = Input(UInt(5.W))

    // todo: csr的旁路信号
    val wbuCsrWen = Input(Bool())
    val wbuCsrAddr= Input(UInt(3.W))
    val lsuCsrWen = Input(Bool())
    val lsuCsrAddr= Input(UInt(3.W))
    // 执行阶段的csr信号
    val csrWen    = Input(Bool())
    val csrRaddr  = Input(UInt(3.W))

    // 旁路控制信号，用于控制alu的两个源操作数
    val forwardA = Output(UInt(3.W))
    val forwardB = Output(UInt(3.W))
  })

  val forwardALsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.rs1
  val forwardBLsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.rs2
  val forwardAWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.rs1
  val forwardBWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.rs2

  // todo：csr信号的判断
  val forwardACsrWbu = io.csrWen && io.wbuCsrWen && io.wbuCsrAddr =/= CsrAddr.nul && io.csrRaddr === io.wbuCsrAddr
  val forwardACsrLsu = io.csrWen && io.lsuCsrWen && io.lsuCsrAddr =/= CsrAddr.nul && io.csrRaddr === io.lsuCsrAddr

  io.forwardA := Mux(forwardALsu, Forward.lsuData, Mux(forwardAWbu, Forward.wbuData, Mux(forwardACsrWbu, Forward.csrWbuData, Mux(forwardACsrLsu, Forward.csrLsuData, Forward.normal))))
  io.forwardB := Mux(forwardBLsu, Forward.lsuData, Mux(forwardBWbu, Forward.wbuData, Forward.normal))
}