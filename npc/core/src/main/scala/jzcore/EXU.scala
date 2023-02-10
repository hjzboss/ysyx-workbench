package jzcore

import chisel3._
import chisel3.util._
import utils._

class EXU extends Module {
  val io = IO(new Bundle {
    val datasrc   = Flipped(new DataSrcIO)
    val aluCtrl   = Flipped(new AluIO)
    val ctrl      = Flipped(new Ctrl)
    
    val regWrite  = new RFWriteIO
    val redirect  = new RedirectIO
  })
  
  val alu   = Module(new Alu)
  val lsu   = Module(new Lsu)
  val stop  = Module(new Stop)

  val aluSrc1 = io.aluCtrl.aluSrc1
  val aluSrc2 = io.aluCtrl.aluSrc2
  
  // todo: forward
  val opAPre = io.datasrc.src1
  val opBPre = io.datasrc.src2

  val opA = Mux(aluSrc1 === SrcType.pc, io.datasrc.pc, Mux(aluSrc1 === SrcType.nul, 0.U(64.W), opAPre))
  val opB = Mux(aluSrc2 === SrcType.reg, opBPre, Mux(aluSrc2 === SrcType.plus4, 4.U(64.W), io.datasrc.imm))

  val lsType  = io.ctrl.lsType
  val rdata   = lsu.io.rdata
  val wmask   = LookupTreeDefault(lsType, Wmask.nop, List(
    LsType.sd   -> Wmask.double,
    LsType.sw   -> Wmask.word,
    LsType.sh   -> Wmask.half,
    LsType.sb   -> Wmask.byte,
  ))
  val lsuOut  = LookupTreeDefault(lsType, rdata, List(
    LsType.ld   -> rdata,
    LsType.lw   -> SignExt(rdata(31, 0), 64),
    LsType.lh   -> SignExt(rdata(15, 0), 64),
    LsType.lb   -> SignExt(rdata(7, 0), 64),
    LsType.lbu  -> Cat(0.U(56.W), rdata(7, 0))
    LsType.lhu  -> ZeroExt(rdata(15, 0), 64),
  ))
  val aluOut  = alu.io.aluOut

  alu.io.opA           := opA
  alu.io.opB           := opB
  alu.io.aluOp         := io.aluCtrl.aluOp

  lsu.io.raddr         := aluOut
  lsu.io.waddr         := aluOut
  lsu.io.wdata         := io.ctrl.wdata
  lsu.io.wmask         := wmask

  io.regWrite.rd       := io.ctrl.rd
  io.regWrite.wen      := io.ctrl.regWen

  // todo, mem
  io.regWrite.value    := Mux(io.ctrl.loadMem, lsuOut, aluOut)

  // todo: branch addr
  val brAddrOpA = Mux(io.ctrl.isJalr, opAPre, io.datasrc.pc)
  io.redirect.brAddr   := brAddrOpA + io.datasrc.imm
  io.redirect.valid    := Mux(io.ctrl.br && alu.io.brMark, true.B, false.B)

  // ebreak
  stop.io.valid := Mux(io.ctrl.break, true.B, false.B)
}