package jzcore

import chisel3._
import chisel3.util._
import utils._

class LS_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool())

    val validIn = Input(Bool())
    val validOut = Output(Bool())

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
  exuOutReset.csrWaddr    := CsrAddr.nul
  exuOutReset.csrWen      := false.B
  exuOutReset.csrValue    := 0.U(64.W)
  exuOutReset.ebreak      := false.B
  exuOutReset.haltRet     := 0.U(64.W)

  val memCtrlReg           = RegInit(exuOutReset)
  memCtrlReg              := Mux(io.stall, memCtrlReg, io.in)

  val validReg             = RegInit(false.B)
  validReg                := Mux(io.stall, validReg, io.validIn)

  io.out                  := memCtrlReg
  io.validOut             := validReg
}