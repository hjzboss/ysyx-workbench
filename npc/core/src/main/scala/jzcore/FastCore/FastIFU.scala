package jzcore

import chisel3._
import top.Settings
import chisel3.util._

class FastIFU extends Module with HasResetVector {
  val io = IO(new Bundle {
    // 用于仿真环境
    val debug         = if(Settings.get("sim")) Some(new DebugIO) else None
    
    val valid         = Output(Bool()) // 是否是一条有效指令

    // from exu
    val exuRedirect   = Flipped(new RedirectIO)

    // ctrl
    val stall       = Input(Bool()) // 停顿信号，停止pc的变化，并将取指的ready设置为false，保持取出的指令不变

    val out         = new InstrFetch
  })

  val valid        = dontTouch(WireDefault(false.B))
  valid           := !io.stall & !io.exuRedirect.valid
  io.valid        := valid

  // pc
  val pc           = RegInit(resetVector.U(32.W))
  val snpc         = pc + 4.U(32.W)

  pc              := Mux(io.stall, pc, Mux(io.exuRedirect.valid, io.exuRedirect.brAddr, snpc))
  val imem         = Module(new IMEM)
  imem.io.pc      := pc
  io.out.pc       := pc
  io.out.inst     := imem.io.inst


  if(Settings.get("sim")) {
    io.debug.get.nextPc := Mux(io.stall, pc, Mux(io.exuRedirect.valid, io.exuRedirect.brAddr, snpc))
    io.debug.get.pc     := pc
    io.debug.get.inst   := io.out.inst
    io.debug.get.valid  := valid
  }
}