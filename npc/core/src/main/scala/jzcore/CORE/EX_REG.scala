package jzcore

import chisel3._
import chisel3.util._
import utils._

class EX_REG extends Module with HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool())
    val flush = Input(Bool())

    // just for debug
    val validIn = Input(Bool())
    val validOut = Output(Bool())

    // 数据传递
    val datasrcIn   = Flipped(new DataSrcIO)
    val aluCtrlIn   = Flipped(new AluIO)
    val ctrlIn      = Flipped(new CtrlFlow)

    val datasrcOut  = new DataSrcIO
    val aluCtrlOut  = new AluIO
    val ctrlOut     = new CtrlFlow

    val debugIn = Flipped(new DebugIO)
    val debugOut = new DebugIO
  })

  // 复位值
  val datasrcReset         = Wire(new DataSrcIO)
  datasrcReset.pc         := resetVector.U(32.W)
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
  ctrlReset.csrWen        := false.B
  ctrlReset.excepNo       := 0.U(4.W)
  ctrlReset.exception     := false.B
  ctrlReset.csrWaddr      := CsrAddr.nul
  ctrlReset.memWen        := false.B
  ctrlReset.memRen        := false.B
  ctrlReset.ebreak        := false.B
  ctrlReset.sysInsType    := System.nop
  ctrlReset.rs1           := 0.U(5.W)
  ctrlReset.rs2           := 0.U(5.W)

  val datasrcReg           = RegInit(datasrcReset)
  //val stallDatasrc         = dontTouch(Wire(new DataSrcIO))
  //stallDatasrc            := Mux(io.stall, datasrcReg, io.datasrcIn)
  //datasrcReg              := Mux(io.flush, datasrcReset, stallDatasrc)
  datasrcReg              := Mux(io.stall, datasrcReg, Mux(io.flush, datasrcReset, io.datasrcIn))

  val aluCtrlReg           = RegInit(aluCtrlReset)
  //val stallAluCtrl         = dontTouch(Wire(new AluIO))
  //stallAluCtrl            := Mux(io.stall, aluCtrlReg, io.aluCtrlIn)
  //aluCtrlReg              := Mux(io.flush, aluCtrlReset, stallAluCtrl)
  aluCtrlReg              := Mux(io.stall, aluCtrlReg, Mux(io.flush, aluCtrlReset, io.aluCtrlIn))

  val ctrlReg              = RegInit(ctrlReset)
  //val stallCtrl            = dontTouch(Wire(new CtrlFlow))
  //stallCtrl               := Mux(io.stall, ctrlReg, io.ctrlIn)
  //ctrlReg                 := Mux(io.flush, ctrlReset, stallCtrl)
  ctrlReg                 := Mux(io.stall, ctrlReg, Mux(io.flush, ctrlReset, io.ctrlIn))

  val validReg             = RegInit(false.B)
  //validReg                := Mux(io.flush, false.B, Mux(io.stall, validReg, io.validIn))
  validReg                := Mux(io.stall, validReg, Mux(io.flush, false.B, io.validIn))

  io.datasrcOut           := datasrcReg
  io.aluCtrlOut           := aluCtrlReg
  io.ctrlOut              := ctrlReg
  io.validOut             := validReg

  val debugReset = Wire(new DebugIO)
  debugReset.pc := resetVector.U(32.W)
  debugReset.nextPc := resetVector.U(32.W)
  debugReset.inst := Instruction.NOP

  val debugReg = RegInit(debugReset)
  //val stallDebug = dontTouch(Wire(new DebugIO))
  //stallDebug := Mux(io.stall, debugReg, io.debugIn)
  //debugReg := Mux(io.flush, debugReset, stallDebug)
  debugReg := Mux(io.stall, debugReg, Mux(io.flush, debugReset, io.debugIn))


  io.debugOut := debugReg
}