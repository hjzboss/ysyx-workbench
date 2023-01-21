package jzcore

import chisel3._
import chisel3.util._

class EXU extends Module with HasSrcDecode {
  val io = IO(new Bundle {
    val datasrc   = Flipped(new DataSrcIO)
    val aluCtrl   = Flipped(new AluIO)
    val ctrl      = Flipped(new Ctrl)
    
    val regWrite  = new RFWriteIO
    val branch    = new BranchCtrl
  })
  
  val alu = Module(new Alu)

  val aluSrc1 = io.aluCtrl.aluSrc1
  val aluSrc2 = io.aluCtrl.aluSrc2
  val opAPre = Mux(aluSrc1 === SrcPc, io.datasrc.pc, Mux(aluSrc1 === SrcNull, 0.U(64.W), io.datasrc.src1))
  val opBPre = Mux(aluSrc2 === SrcReg, io.datasrc.src1, Mux(aluSrc2 === SrcPlus4, 4.U(64.W), io.datasrc.imm))

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
  println("alu_o=%d", alu.io.aluOut)

  // todo: branch addr
  val brAddr = 0.U(64.W)
  io.branch.brCtrl := alu.io.brMark && io.ctrl.br
  io.branch.brAddr := brAddr
}