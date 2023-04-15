package jzcore

import chisel3._
import chisel3.util._
import utils._

class WB_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val flush = Input(Bool())

    val validIn = Input(Bool())
    val validOut = Output(Bool())
  
    val in = Flipped(new LsuOut)
    val out = new LsuOut

    val debugIn = Flipped(new DebugIO)
    val debugOut = new DebugIO
  })

  val lsuReset = Wire(new LsuOut)
  lsuReset.exuOut     := 0.U(64.W)
  lsuReset.lsuOut     := 0.U(64.W)
  lsuReset.loadMem    := false.B
  lsuReset.rd         := 0.U(5.W)
  lsuReset.regWen     := false.B
  lsuReset.pc         := resetVector.U(64.W)
  lsuReset.excepNo    := 0.U(4.W)
  lsuReset.exception  := false.B
  lsuReset.csrWaddr   := CsrAddr.nul
  lsuReset.csrWen     := false.B
  lsuReset.csrValue   := 0.U(64.W)
  lsuReset.ebreak     := false.B
  lsuReset.haltRet    := 0.U(64.W)

  lsuReset.debugPc    := resetVector.U(64.W)
  lsuReset.nextPc     := resetVector.U(64.W)    

  val lsuReg           = RegInit(lsuReset)
  lsuReg              := Mux(io.flush, lsuReset, io.in)

  val validReg         = RegInit(false.B)
  validReg            := Mux(io.flush, false.B, io.validIn)

  io.out              := lsuReg
  io.validOut         := validReg

  val debugReset = Wire(new DebugIO)
  debugReset.pc := resetVector.U(64.W)
  debugReset.nextPc := resetVector.U(64.W)
  debugReset.inst := Instruction.NOP

  val debugReg = RegInit(debugReset)
  debugReg := Mux(io.flush, debugReset, io.debugIn)

  io.debugOut := debugReg
}