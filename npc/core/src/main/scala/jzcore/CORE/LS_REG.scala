package jzcore

import chisel3._
import chisel3.util._
import utils._

class LS_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool())

    val in = Flipped(new ExuOut)
    val out = new ExuOut

    /*
    val validIn = Input(Bool())
    val validOut = Output(Bool())
    val debugIn = Flipped(new DebugIO)
    val debugOut = new DebugIO*/
  })

  val exuOutReset          = Wire(new ExuOut)
  exuOutReset.lsType      := LsType.nop
  exuOutReset.wmask       := Wmask.nop
  exuOutReset.lsuWen      := false.B
  exuOutReset.lsuRen      := false.B
  exuOutReset.lsuAddr     := 0.U(64.W)
  exuOutReset.lsuWdata    := 0.U(64.W)
  exuOutReset.loadMem     := false.B
  exuOutReset.exuOut      := 0.U(64.W)
  exuOutReset.rd          := 0.U(5.W)
  exuOutReset.regWen      := false.B
  exuOutReset.pc          := 0.U(32.W)
  exuOutReset.excepNo     := 0.U(4.W)
  exuOutReset.exception   := false.B
  exuOutReset.csrWaddr    := CsrId.nul
  exuOutReset.csrWen      := false.B
  exuOutReset.csrValue    := 0.U(64.W)
  exuOutReset.coherence   := false.B
  // debug
  //exuOutReset.ebreak      := false.B
  //exuOutReset.haltRet     := 0.U(64.W)

  val memCtrlReg           = RegInit(exuOutReset)
  //val stallMemCtrl         = Wire(new ExuOut)
  //stallMemCtrl            := Mux(io.stall, memCtrlReg, io.in)
  memCtrlReg              := Mux(io.stall, memCtrlReg, io.in)
  io.out                  := memCtrlReg

  /*
  val validReg             = RegInit(false.B)
  validReg                := Mux(io.stall, validReg, io.validIn)
  io.validOut             := validReg

  val debugReset = Wire(new DebugIO)
  debugReset.pc := 0.U(32.W)
  debugReset.nextPc := 0.U(32.W)
  debugReset.inst := Instruction.NOP

  val debugReg = RegInit(debugReset)
  val stallDebug = dontTouch(Wire(new DebugIO))
  stallDebug := Mux(io.stall, debugReg, io.debugIn)
  debugReg := stallDebug

  io.debugOut := debugReg*/
}