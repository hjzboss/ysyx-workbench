package jzcore

import chisel3._
import chisel3.util._
import utils._

class EXU extends Module {
  val io = IO(new Bundle {
    // 来自idu
    val datasrc   = Flipped(new DataSrcIO)
    val aluCtrl   = Flipped(new AluIO)
    val ctrl      = Flipped(new CtrlFlow)
    
    // 传给Lsu
    val out       = new MemCtrl

/*
    // 写回idu
    val regWrite  = new RFWriteIO
    val csrWrite  = new CSRWriteIO
*/

    // 写回ifu
    val redirect  = new RedirectIO

    val ready     = Output(Bool()) // todo，此时总是true
  })

  val alu   = Module(new Alu)
  val stop  = Module(new Stop)

  val aluSrc1 = io.aluCtrl.aluSrc1
  val aluSrc2 = io.aluCtrl.aluSrc2

  // todo: forward
  val opAPre = io.datasrc.src1
  val opBPre = io.datasrc.src2

  val opA = Mux(aluSrc1 === SrcType.pc, io.datasrc.pc, Mux(aluSrc1 === SrcType.nul, 0.U(64.W), opAPre))
  val opB = Mux(aluSrc2 === SrcType.reg, opBPre, Mux(aluSrc2 === SrcType.plus4, 4.U(64.W), io.datasrc.imm))
  val aluOut  = alu.io.aluOut

  alu.io.opA           := opA
  alu.io.opB           := opB
  alu.io.aluOp         := io.aluCtrl.aluOp

  // todo: branch addr
  val brAddrOpA         = Mux(io.ctrl.isJalr, opAPre, io.datasrc.pc)
  val brAddr            = brAddrOpA + io.datasrc.imm

  // ecall mret
  io.redirect.brAddr   := Mux(io.ctrl.sysInsType === System.ecall, opAPre, Mux(io.ctrl.sysInsType === System.mret, aluOut, brAddr))
  io.redirect.valid    := Mux((io.ctrl.br && alu.io.brMark) || io.ctrl.sysInsType === System.ecall || io.ctrl.sysInsType === System.mret, true.B, false.B)

  io.out.lsType        := io.ctrl.lsType
  io.out.wmask         := io.ctrl.wmask
  io.out.wen           := io.ctrl.memWen
  io.out.ren           := io.ctrl.memRen
  io.out.addr          := aluOut
  io.out.wdata         := io.ctrl.wdata
  io.out.loadMem       := io.ctrl.loadMem
  io.out.exuOut        := aluOut
  io.out.rd            := io.ctrl.rd
  io.out.regWen        := io.ctrl.regWen
  io.out.pc            := io.datasrc.pc
  io.out.no            := Mux(io.ctrl.sysInsType === System.ecall, "hb".U, 0.U)
  io.out.exception     := io.ctrl.sysInsType === System.ecall
  io.out.csrWaddr      := io.ctrl.csrWaddr
  io.out.csrWen        := io.ctrl.isCsr

  io.ready             := true.B

  // ebreak, todo
  stop.io.valid        := Mux(io.ctrl.sysInsType === System.ebreak, true.B, false.B)
  stop.io.haltRet      := io.datasrc.src1
}