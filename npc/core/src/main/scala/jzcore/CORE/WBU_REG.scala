package jzcore

import chisel3._
import chisel3.util._
import utils._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class WBU_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
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
  lsuReset.csrWaddr   := 0.U(2.W)
  lsuReset.csrWen     := false.B
  lsuReset.ebreak     := false.B
  lsuReset.haltRet    := 0.U(64.W)

  val lsuReg           = RegInit(lsuReset)
  lsuReg              := io.in

  io.out              := lsuReg
}