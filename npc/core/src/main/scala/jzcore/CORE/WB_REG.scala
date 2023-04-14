package jzcore

import chisel3._
import chisel3.util._
import utils._

class WB_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val validIn = Input(Bool())
    val validOut = Output(Bool())
  
    val in = Flipped(new lsuOut)
    val out = new lsuOut
  })

  val lsuReset = Wire(new lsuOut)
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

  val lsuReg           = RegInit(lsuReset)
  lsuReg              := io.in

  val validReg         = RegInit(false.B)
  validReg            := Mux(io.flush, false.B, Mux(io.stall, validReg, io.validIn))

  io.out              := lsuReg
  io.validOut         := validReg
}