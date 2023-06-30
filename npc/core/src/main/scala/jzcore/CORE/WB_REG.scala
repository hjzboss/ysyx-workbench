package jzcore

import chisel3._
import chisel3.util._
import utils._

class WB_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    //val flush = Input(Bool())
    val stall = Input(Bool())
  
    val in = Flipped(new LsuOut)
    val out = new LsuOut

    /*
    val validIn = Input(Bool())
    val validOut = Output(Bool())
    val debugIn = Flipped(new DebugIO)
    val debugOut = new DebugIO

    val lsFlagIn = Input(Bool())
    val lsFlagOut = Output(Bool())*/
  })

  val lsuReset = Wire(new LsuOut)
  lsuReset.exuOut     := 0.U(64.W)
  lsuReset.lsuOut     := 0.U(64.W)
  lsuReset.loadMem    := false.B
  lsuReset.rd         := 0.U(5.W)
  lsuReset.regWen     := false.B
  lsuReset.pc         := 0.U(32.W)
  lsuReset.excepNo    := 0.U(4.W)
  lsuReset.exception  := false.B
  lsuReset.csrWaddr   := CsrAddr.nul
  lsuReset.csrWen     := false.B
  lsuReset.csrValue   := 0.U(64.W)
  //lsuReset.ebreak     := false.B
  //lsuReset.haltRet    := 0.U(64.W)

  val lsuReg           = RegInit(lsuReset)
  lsuReg              := Mux(io.stall, lsuReg, io.in)
  io.out              := lsuReg

  /*
  val validReg         = RegInit(false.B)
  validReg            := Mux(io.stall, false.B, io.validIn) // todo
  io.validOut         := validReg

  val debugReset = Wire(new DebugIO)
  debugReset.pc := 0.U(32.W)
  debugReset.nextPc := 0.U(32.W)
  debugReset.inst := Instruction.NOP

  val debugReg = RegInit(debugReset)
  debugReg := Mux(io.stall, debugReg, io.debugIn)
  io.debugOut := debugReg

  val lsFlagReg = RegInit(false.B)
  lsFlagReg := Mux(io.stall, lsFlagReg, io.lsFlagIn)
  io.lsFlagOut := lsFlagReg*/
}