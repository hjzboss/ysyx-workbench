package jzcore

import chisel3._
import chisel3.util._
import utils._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class EX_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool())
    val flush = Input(Bool())

    // 数据传递
    val datasrcIn   = Flipped(new DataSrcIO)
    val aluCtrlIn   = Flipped(new AluIO)
    val ctrlIn      = Flipped(new CtrlFlow)

    val datasrcOut  = new DataSrcIO
    val aluCtrlOut  = new AluIO
    val ctrlOut     = new CtrlFlow
  })

  // 复位值
  val datasrcReset         = Wire(new DataSrcIO)
  datasrcReset.pc         := resetVector.U(64.W)
  datasrcReset.src1       := 0.U(64.W)
  datasrcReset.src2       := 0.U(64.W)
  datasrcReset.imm        := 0.U(64.W)

  val aluCtrlReset         = Wire(new AluIO)
  aluCtrlReset.aluSrc1    := SrcType.nul
  aluCtrlReset.aluSrc2    := SrcType.nul
  aluCtrlReset.aluOp      := AluOp.nop

  val ctrlReset            = Wire(new CtrlFlow)
  ctrlReset.br            := false.B
  ctrlReset.rd            := 0.U(5.W)
  ctrlReset.regWen        := false.B
  ctrlReset.isJalr        := false.B
  ctrlReset.lsType        := LsType.nop
  ctrlReset.loadMem       := false.B
  ctrlReset.wmask         := Wmask.nop
  ctrlReset.isCsr         := false.B
  ctrlReset.csrWaddr      := 0.U(2.W)
  ctrlReset.sysInsType    := System.nop
  ctrlReset.memWen        := false.B
  ctrlReset.memRen        := false.B
  ctrlReset.ebreak        := false.B

  val datasrcReg           = RegInit(datasrcReset)
  datasrcReg              := Mux(io.flush, datasrcReset, Mux(io.stall, datasrcReg, io.datasrcIn))

  val aluCtrlReg           = RegInit(aluCtrlReset)
  aluCtrlReg              := Mux(io.flush, aluCtrlReset, Mux(io.stall, aluCtrlReg, io.aluCtrlIn))

  val ctrlReg              = RegInit(ctrlReset)
  ctrlReg                 := Mux(io.flush, ctrlReset, Mux(io.stall, ctrlReg, io.ctrlIn))

  io.datasrcOut           := datasrcReg
  io.aluCtrlOut           := aluCtrlReg
  io.ctrlOut              := ctrlReg
}