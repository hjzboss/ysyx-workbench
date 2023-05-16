package jzcore

import chisel3._
import chisel3.util._
import utils._

class EXU extends Module {
  val io = IO(new Bundle {
    // 来自idu
    val datasrc     = Flipped(new DataSrcIO)
    val aluCtrl     = Flipped(new AluIO)
    val ctrl        = Flipped(new CtrlFlow)
    
    // 传给Lsu
    val out         = new ExuOut

    // 写回ifu,todo:需要移动到idu阶段
    val redirect    = new RedirectIO

    // 旁路数据
    val lsuForward  = Input(UInt(64.W))
    val wbuForward  = Input(UInt(64.W))
    val csrForward  = Input(UInt(64.W))

    // 旁路控制信号
    val forwardA    = Input(UInt(2.W))
    val forwardB    = Input(UInt(2.W))

    val debugIn     = Flipped(new DebugIO)
    val debugOut    = new DebugIO
  })

  val alu   = Module(new Alu)
  val stop  = Module(new Stop)

  val aluSrc1 = io.aluCtrl.aluSrc1
  val aluSrc2 = io.aluCtrl.aluSrc2

  // forward
  val opAPre = MuxLookup(io.forwardA, io.datasrc.src1, List(
    Forward.lsuData -> io.lsuForward,
    Forward.wbuData -> io.wbuForward,
    Forward.csrData -> io.csrForward,
    Forward.normal  -> io.datasrc.src1
  ))
  val opBPre = MuxLookup(io.forwardB, io.datasrc.src2, List(
    Forward.lsuData -> io.lsuForward,
    Forward.wbuData -> io.wbuForward,
    Forward.csrData -> io.csrForward,
    Forward.normal  -> io.datasrc.src2
  ))

  val pc = ZeroExt(io.datasrc.pc, 64)

  val opA = Mux(aluSrc1 === SrcType.pc, pc, Mux(aluSrc1 === SrcType.nul, 0.U(64.W), opAPre))
  val opB = Mux(aluSrc2 === SrcType.reg, opBPre, Mux(aluSrc2 === SrcType.plus4, 4.U(64.W), io.datasrc.imm))
  val aluOut  = alu.io.aluOut

  alu.io.opA           := opA
  alu.io.opB           := opB
  alu.io.aluOp         := io.aluCtrl.aluOp

  // todo: branch addr
  val brAddrOpA         = Mux(io.ctrl.isJalr, opAPre, pc)
  val brAddr            = brAddrOpA + io.datasrc.imm

  // ecall mret
  val brAddrPre         = Mux(io.ctrl.sysInsType === System.ecall, opAPre, Mux(io.ctrl.sysInsType === System.mret, aluOut, brAddr))
  io.redirect.brAddr   := brAddrPre(31, 0)
  io.redirect.valid    := Mux((io.ctrl.br && alu.io.brMark) || io.ctrl.sysInsType === System.ecall || io.ctrl.sysInsType === System.mret, true.B, false.B)

  io.out.lsType        := io.ctrl.lsType
  io.out.wmask         := io.ctrl.wmask
  io.out.lsuWen        := io.ctrl.memWen
  io.out.lsuRen        := io.ctrl.memRen
  io.out.lsuAddr       := aluOut(31, 0)
  io.out.lsuWdata      := opBPre // todo:forward
  io.out.loadMem       := io.ctrl.loadMem
  io.out.exuOut        := aluOut
  io.out.rd            := io.ctrl.rd
  io.out.regWen        := io.ctrl.regWen
  io.out.pc            := io.datasrc.pc
  io.out.excepNo       := io.ctrl.excepNo
  io.out.exception     := io.ctrl.exception
  io.out.csrWaddr      := io.ctrl.csrWaddr
  io.out.csrWen        := io.ctrl.csrWen
  io.out.ebreak        := io.ctrl.ebreak
  io.out.haltRet       := opAPre // todo: forward
  io.out.csrValue      := opAPre

  io.debugOut.inst     := io.debugIn.inst
  io.debugOut.pc       := io.debugIn.pc
  io.debugOut.nextPc   := Mux(io.redirect.valid, brAddr, io.debugIn.nextPc)
}