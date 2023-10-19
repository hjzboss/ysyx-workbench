package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

// ifu_idu的流水线寄存器
class ID_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool()) // 停顿信号，用于阻塞流水线
    val flush = Input(Bool()) // 清空流水线

    val validIn = Input(Bool())
    val validOut = Output(Bool())

    // 传递数据
    val in    = Flipped(new InstrFetch)
    val out   = new InstrFetch

    val debugIn  = if(Settings.get("sim")) Some(Flipped(new DebugIO)) else None
    val debugOut = if(Settings.get("sim")) Some(new DebugIO) else None
  })

  // 复位值
  val regReset = Wire(new InstrFetch)
  regReset.pc := 0.U(32.W)
  regReset.inst := Instruction.NOP

  // 流水线寄存器
  val idReg = RegInit(regReset)
  idReg := Mux(io.stall, idReg, Mux(io.flush, regReset, io.in))

  val validReg = RegInit(false.B)
  validReg := Mux(io.stall, validReg, Mux(io.flush, false.B, io.validIn))

  io.out := idReg
  io.validOut := validReg

  if(Settings.get("sim")) {
    val debugReset = Wire(new DebugIO)
    debugReset.pc := 0.U(32.W)
    debugReset.nextPc := 0.U(32.W)
    debugReset.inst := Instruction.NOP
    debugReset.valid := false.B

    val debugReg = RegInit(debugReset)
    debugReg := Mux(io.stall, debugReg, Mux(io.flush, debugReset, io.debugIn))
    io.debugOut := debugReg
  }
}