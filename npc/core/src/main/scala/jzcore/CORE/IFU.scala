package jzcore

import chisel3._
import top.Settings
import chisel3.util._


trait HasResetVector {
  val resetVector = if(Settings.get("sim")) Settings.getLong("ResetVector") else Settings.getLong("SocResetVector")
}

class IFU extends Module with HasResetVector {
  val io = IO(new Bundle {
    // 用于仿真环境
    val debug         = if(Settings.get("sim")) Some(new DebugIO) else None
    val valid         = Output(Bool()) // 是否是一条有效指令

    // from idu
    val iduRedirect   = Flipped(new RedirectIO)
    val bpuTrain      = new BPUTrainIO

    val icRedirect    = Flipped(new RedirectIO)

    // to idu
    val out           = new Stage1IO
 
    // ctrl
    val stall       = Input(Bool()) // 停顿信号，停止pc的变化，并将取指的ready设置为false，保持取出的指令不变
  })

  // pc
  val pc           = RegInit(resetVector.U(32.W))
  val bpu          = Module(new BPU)

  // 分支预测
  //bpu.io.pc       := pc
  //bpu.io.bpuTrain := io.bpuTrain
  val snpc         = pc + 4.U
  val dnpc         = Mux(io.stall, pc, Mux(io.iduRedirect.valid, io.iduRedirect.brAddr, Mux(io.icRedirect.valid, io.icRedirect.brAddr, snpc)))
  pc              := dnpc

  if(Settings.get("sim")) {
    io.out.cacheable := true.B
  } else {
    io.out.cacheable := pc <= "hffff_ffff".U && pc >= "h8000_0000".U
  }

  io.out.addr     := pc
  io.out.npc      := dnpc

  val valid        = dontTouch(WireDefault(false.B))
  valid           := !io.stall && !io.iduRedirect.valid && !io.icRedirect.valid
  io.valid        := valid

  if(Settings.get("sim")) {
    io.debug.get.nextPc := Mux(io.stall, pc, Mux(io.iduRedirect.valid, io.iduRedirect.brAddr, Mux(io.icRedirect.valid, io.icRedirect.brAddr, snpc)))
    io.debug.get.pc     := pc
    io.debug.get.inst   := Instruction.NOP
    io.debug.get.valid  := valid
  }
}