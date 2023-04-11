package jzcore

import chisel3._
import chisel3.util._
import utils._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class LSU_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val in = Flipped(new ExuOut)
    val out = new ExuOut
  })

  val exuOutReset          = Wire(new ExuOut)
  exuOutReset.lsType      := LsType.nop
  exuOutReset.wmask       := Wmask.nop
  exuOutReset.lsuWen      := false.B
  exuOutReset.lsuRen      := false.B
  exuOutReset.lsuAddr     := resetVector.U(64.W)
  exuOutReset.lsuWdata    := 0.U(64.W)
  exuOutReset.loadMem     := false.B
  exuOutReset.exuOut      := 0.U(64.W)
  exuOutReset.rd          := 0.U(5.W)
  exuOutReset.regWen      := false.B
  exuOutReset.pc          := resetVector.U(64.W)
  exuOutReset.excepNo     := 0.U(4.W)
  exuOutReset.exception   := false.B
  exuOutReset.csrWaddr    := 0.U(2.W)
  exuOutReset.csrWen      := false.B
  exuOutReset.ebreak      := false.B
  exuOutReset.haltRet     := 0.U(64.W)

  val memCtrlReg           = RegInit(exuOutReset)
  memCtrlReg              := io.in

  io.out                  := memCtrlReg
}