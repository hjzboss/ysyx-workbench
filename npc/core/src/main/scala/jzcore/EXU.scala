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
  
  // todo: forward
  val opAPre = io.datasrc.src1
  val opBPre = io.datasrc.src2

  val opA = Mux(aluSrc1 === SrcType.pc, io.datasrc.pc, Mux(aluSrc1 === SrcType.nul, 0.U(64.W), opAPre))
  val opB = Mux(aluSrc2 === SrcType.reg, opBPre, Mux(aluSrc2 === SrcType.plus4, 4.U(64.W), io.datasrc.imm))

  alu.io.opA           := opA
  alu.io.opB           := opB
  alu.io.aluOp         := io.aluCtrl.aluOp

  io.regWrite.rd    := io.ctrl.rd
  io.regWrite.wen   := io.ctrl.regWen

  // todo, mem
  io.regWrite.value := alu.io.aluOut

  // todo: branch addr
  val brAddrOpA = Mux(io.ctrl.isJalr, opAPre, io.datasrc.pc)
  io.redirect.brAddr   := brAddrOpA + io.datasrc.imm
  io.redirect.valid    := Mux(io.ctrl.br && alu.io.brMark, true.B, false.B)

  // ebreak
  val stop = Module(new stop)
  stop.io.valid := Mux(io.ctrl.break, true.B, false.B)
}