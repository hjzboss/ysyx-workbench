package jzcore

import chisel3._
import chisel3.util._
import utils._

// ifu_idu的流水线寄存器
class ID_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool()) // 停顿信号，用于阻塞流水线
    val flush = Input(Bool()) // 清空流水线

    //val validIn = Input(Bool())
    //val validOut = Output(Bool())

    // 传递数据
    val in    = Flipped(new InstrFetch)
    val out   = new InstrFetch

    //val debugIn = Flipped(new DebugIO)
    //val debugOut = new DebugIO
  })

  // 复位值
  val regReset = Wire(new InstrFetch)
  regReset.pc := 0.U(32.W)
  regReset.inst := Instruction.NOP

  // 流水线寄存器
  val idReg = RegInit(regReset)
  //val stallIdReg = dontTouch(Wire(new InstrFetch)) // 防止信号被优化
  //stallIdReg := Mux(io.stall, idReg, io.in)
  //idReg := Mux(io.flush, regReset, stallIdReg)
  idReg := Mux(io.stall, idReg, Mux(io.flush, regReset, io.in))

  //val validReg = RegInit(false.B)
  //validReg := Mux(io.stall, validReg, Mux(io.flush, false.B, io.validIn))

  //io.out := idReg
  //io.validOut := validReg

  /*
  val debugReset = Wire(new DebugIO)
  debugReset.pc := 0.U(32.W)
  debugReset.nextPc := 0.U(32.W)
  debugReset.inst := Instruction.NOP

  val debugReg = RegInit(debugReset)
  //val stallDebug = dontTouch(Wire(new DebugIO))
  //stallDebug := Mux(io.stall, debugReg, io.debugIn)
  //debugReg := Mux(io.flush, debugReset, stallDebug)
  debugReg := Mux(io.stall, debugReg, Mux(io.flush, debugReset, io.debugIn))
  io.debugOut := debugReg
  */
}