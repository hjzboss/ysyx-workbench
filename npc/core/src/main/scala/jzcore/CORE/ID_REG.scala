package jzcore

import chisel3._
import chisel3.util._
import utils._

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

    val debugIn = Flipped(new DebugIO)
    val debugOut = new DebugIO
  })

  // 复位值
  val regReset = Wire(new InstrFetch)
  regReset.pc := resetVector.U(64.W)
  regReset.inst := Instruction.NOP

  // 流水线寄存器
  val idReg = RegInit(regReset)
  idReg := Mux(io.flush, regReset, Mux(io.stall, idReg, io.in))

  val validReg = RegInit(false.B)
  validReg := Mux(io.flush, false.B, Mux(io.stall, validReg, io.validIn))

  io.out := idReg
  io.validOut := validReg

  val debugReset = Wire(new DebugIO)
  debugReset.pc := resetVector.U(64.W)
  debugReset.nextPc := resetVector.U(64.W)
  debugReset.inst := Instruction.NOP

  val debugReg = RegInit(debugReset)
  debugReg := Mux(io.flush, debugReset, Mux(io.stall, debugReg, io.debugIn))
  io.debugOut := debugReg
}