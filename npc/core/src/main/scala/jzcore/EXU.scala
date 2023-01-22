package jzcore

import chisel3._
import chisel3.util._

class EXU extends Module {
  val io = IO(new Bundle {
    val datasrc   = Flipped(new DataSrcIO)
    val aluCtrl   = Flipped(new AluIO)
    val ctrl      = Flipped(new Ctrl)
    
    val regWrite  = new RFWriteIO
    val redirect  = new RedirectIO
  })
  
  val alu = Module(new Alu)

  val aluSrc1 = io.aluCtrl.aluSrc1
  val aluSrc2 = io.aluCtrl.aluSrc2
  val opAPre = Mux(aluSrc1 === SrcType.pc, io.datasrc.pc, Mux(aluSrc1 === SrcType.nul, 0.U(64.W), io.datasrc.src1))
  val opBPre = Mux(aluSrc2 === SrcType.reg, io.datasrc.src1, Mux(aluSrc2 === SrcType.plus4, 4.U(64.W), io.datasrc.imm))

  // forward, todo
  val opA = opAPre
  val opB = opBPre 

  alu.io.opA           := opA
  alu.io.opB           := opB
  alu.io.aluOp         := io.aluCtrl.aluOp

  io.regWrite.rd    := io.ctrl.rd
  io.regWrite.wen   := io.ctrl.regWen

  // todo, mem
  io.regWrite.value := alu.io.aluOut

  // todo: branch addr
  val brValid = 0.U(64.W)
  io.redirect.brAddr   := 0.U(64.W)
  io.redirect.valid    := brValid
}