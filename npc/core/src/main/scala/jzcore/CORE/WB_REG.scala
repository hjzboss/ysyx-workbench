package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

class WB_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool())
  
    val in = Flipped(new LsuOut)
    val out = new LsuOut

    val debugIn  = if(Settings.get("sim")) Some(Flipped(new DebugIO)) else None
    val debugOut = if(Settings.get("sim")) Some(new DebugIO) else None
    val lsFlagIn  = if(Settings.get("sim")) Some(Input(Bool())) else None
    val lsFlagOut = if(Settings.get("sim")) Some(Output(Bool())) else None
  })

  val lsuReset = Wire(new LsuOut)
  lsuReset.exuOut     := 0.U(64.W)
  lsuReset.lsuOut     := 0.U(64.W)
  lsuReset.loadMem    := false.B
  lsuReset.rd         := 0.U(5.W)
  lsuReset.regWen     := false.B
  lsuReset.pc         := 0.U(32.W)
  lsuReset.excepNo    := 0.U(64.W)
  lsuReset.exception  := false.B
  lsuReset.csrWaddr   := CsrId.nul
  lsuReset.csrWen     := false.B
  lsuReset.csrValue   := 0.U(64.W)
  //lsuReset.int        := false.B
  lsuReset.mret       := false.B

  if(Settings.get("sim")) {
    lsuReset.ebreak.get     := false.B
    lsuReset.haltRet.get    := 0.U(64.W)

    val debugReset = Wire(new DebugIO)
    debugReset.pc := 0.U(32.W)
    debugReset.nextPc := 0.U(32.W)
    debugReset.inst := Instruction.NOP
    debugReset.valid := false.B

    val debugReg = RegInit(debugReset)
    debugReg := Mux(io.stall, debugReg, io.debugIn.get)
    io.debugOut.get.pc := debugReg.pc
    io.debugOut.get.nextPc := debugReg.nextPc
    io.debugOut.get.inst := debugReg.inst
    io.debugOut.get.valid := debugReg.valid & !io.stall

    val lsFlagReg = RegInit(false.B)
    lsFlagReg := Mux(io.stall, lsFlagReg, io.lsFlagIn.get)
    io.lsFlagOut.get := lsFlagReg
  }

  val lsuReg           = RegInit(lsuReset)
  lsuReg              := Mux(io.stall, lsuReg, io.in)
  io.out              := lsuReg

  when((io.out.rd === 9.U) && io.out.regWen && io.debugOut.get.valid) {
    printf("wbu s1 write: pc=%x, inst=%x, wdata=%x\n", io.debugOut.get.pc, io.debugOut.get.inst, Mux(io.out.csrWen, io.out.csrValue, Mux(io.out.loadMem, io.out.lsuOut, io.out.exuOut)))
  }
}