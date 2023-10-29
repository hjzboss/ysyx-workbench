package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

class LS_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool())

    val in = Flipped(new ExuOut)
    val out = new ExuOut

    val debugIn = if(Settings.get("sim")) Some(Flipped(new DebugIO)) else None
    val debugOut = if(Settings.get("sim")) Some(new DebugIO) else None
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
  exuOutReset.excepNo     := 0.U(64.W)
  exuOutReset.exception   := false.B
  exuOutReset.csrWaddr    := CsrId.nul
  exuOutReset.csrWen      := false.B
  exuOutReset.csrValue    := 0.U(64.W)
  exuOutReset.coherence   := false.B
  //exuOutReset.int         := false.B
  exuOutReset.mret        := false.B
  exuOutReset.csrChange   := false.B

  if(Settings.get("sim")) {
    // debug
    exuOutReset.ebreak.get    := false.B
    exuOutReset.haltRet.get   := 0.U(64.W)

    val debugReset = Wire(new DebugIO)
    debugReset.pc := 0.U(32.W)
    debugReset.nextPc := 0.U(32.W)
    debugReset.inst := Instruction.NOP
    debugReset.valid := false.B

    val debugReg = RegInit(debugReset)
    debugReg := Mux(io.stall, debugReg, io.debugIn.get)
    io.debugOut.get := debugReg
  }

  val memCtrlReg           = RegInit(exuOutReset)
  memCtrlReg              := Mux(io.stall, memCtrlReg, io.in)
  io.out                  := memCtrlReg
}