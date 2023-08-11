package jzcore

import chisel3._
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
    val wbuCsrAddr= Input(UInt(12.W))
    val lsuCsrWen = Input(Bool())
    val lsuCsrAddr= Input(UInt(12.W))
    // 执行阶段的csr信号
    val csrRen    = Input(Bool())
    val csrRaddr  = Input(UInt(12.W))

    // mcause和mepc的旁路，用于异常指令之后的指令的旁路
    val lsuException = Input(Bool())
    val wbuException = Input(Bool())

    // idu阶段的mret指令会更新mstatus，之前未写回指令的csr写使能
    val mret       = Input(Bool())
    val flushExuCsr= Output(Bool())
    val flushLsuCsr= Output(Bool())
    val flushWbuCsr= Output(Bool())

    // 旁路控制信号，用于控制alu的两个源操作数
    val forwardA = Output(UInt(4.W))
    val forwardB = Output(UInt(4.W))
  })

  val forwardALsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.rs1
  val forwardBLsu = io.lsuRegWen && io.lsuRd =/= 0.U(5.W) && io.lsuRd === io.rs2
  val forwardAWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.rs1
  val forwardBWbu = io.wbuRegWen && io.wbuRd =/= 0.U(5.W) && io.wbuRd === io.rs2

  // todo：csr信号的判断
  val forwardACsrWbu = io.csrRen && (io.wbuCsrWen || io.wbuException) && io.wbuCsrAddr =/= CsrId.nul && io.csrRaddr === io.wbuCsrAddr
  val forwardACsrLsu = io.csrRen && (io.lsuCsrWen || io.lsuException) && io.lsuCsrAddr =/= CsrId.nul && io.csrRaddr === io.lsuCsrAddr

  val excepSel = Wire(Bool())
  excepSel := Mux(io.csrRaddr === CsrId.mepc, Forward.mepc, Forward.no)
  //io.forwardA := Mux(forwardALsu, Forward.lsuData, Mux(forwardAWbu, Forward.wbuData, Mux(forwardACsrWbu, Forward.csrWbuData, Mux(forwardACsrLsu, Forward.csrLsuData, Forward.normal))))
  io.forwardA := Mux(forwardALsu, Forward.lsuData, Mux(forwardAWbu, Forward.wbuData, Mux(forwardACsrWbu, Mux(io.wbuException, excepSel, Forward.csrWbuData), Mux(forwardACsrLsu, Mux(io.lsuException, excepSel, Forward.csrLsuData), Forward.normal))))
  io.forwardB := Mux(forwardBLsu, Forward.lsuData, Mux(forwardBWbu, Forward.wbuData, Forward.normal))

  io.flushExuCsr := io.mret && io.csrRaddr === CsrId.mstatus
  io.flushLsuCsr := io.mret && io.lsuCsrAddr === CsrId.mstatus
  io.flushWbuCsr := io.mret && io.wbuCsrAddr === CsrId.mstatus
}