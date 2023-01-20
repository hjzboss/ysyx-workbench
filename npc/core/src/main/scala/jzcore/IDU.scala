package jzcore

import chisel3._
import chisel3.util._
import utils._

class IDU extends Module with HasOpDecode with HasSrcDecode with AluCtrlDecode with HasResetVector {
  val io = IO(new Bundle {
    val fetch     = Flipped(new InstrFetch)
    val regWrite  = Flipped(new RFWriteIO)

    val datasrc   = new DataSrcIO
    val aluCtrl   = new AluIO
    val ctrl      = new Ctrl
  })

  val rf      = Module(new RF)
  val inst    = io.fetch.inst
  val op      = inst(6, 0)
  val rs1     = inst(19, 15)
  val rs2     = inst(24, 20)
  val rd      = inst(11, 7)
  val funct3  = inst(14, 12)
  val funct7  = inst(31, 25)
  val imm = LookupTree(op, List(
    ItypeL  -> SignExt(inst(31, 20), 64),
    ItypeA  -> SignExt(inst(31, 20), 64),
    ItypeW  -> SignExt(inst(31, 20), 64),
    ItypeJ  -> SignExt(inst(31, 20), 64),
    Stype   -> SignExt(Cat(inst(31, 25), inst(11, 7)), 64),
    Btype   -> SignExt(Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)), 64),
    UtypeL  -> SignExt(Cat(inst(31, 12), 0.U(12.W)), 64),
    UtypeU  -> SignExt(Cat(inst(31, 12), 0.U(12.W)), 64),
    Jtype   -> SignExt(Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)), 64)
  ))

  // 确定alu源操作数的类型
  val aluSrc1 = Mux(op === ItypeJ || op === Jtype || op === UtypeU, SrcPc, Mux(op === UtypeL, SrcNull, SrcReg))
  val aluSrc2 = Mux(op === Rtype || op === RtypeW || op === Btype, SrcReg, Mux(op === ItypeJ || op === Jtype, SrcPlus4, SrcImm))
  //todo
  val aluOp = Add

  // registerfile
  rf.io.read.rs1     := rs1
  rf.io.read.rs2     := rs2
  rf.io.read.ren1    := Mux(op === UtypeL || op === UtypeU, false.B, true.B)
  rf.io.read.ren2    := Mux(op === Rtype, true.B, false.B)
  rf.io.write        := io.regWrite

  io.datasrc.pc       := io.fetch.pc
  io.datasrc.src1     := rf.io.src1
  io.datasrc.src2     := rf.io.src2
  io.datasrc.imm      := imm

  io.ctrl.rd          := rd
  io.ctrl.br          := Mux(op === ItypeJ || op === Jtype || op === Btype, true.B, false.B)
  io.ctrl.regWen      := Mux(op === Btype || op === Stype, false.B, true.B)

  io.aluCtrl.aluSrc1  := aluSrc1
  io.aluCtrl.aluSrc2  := aluSrc2
  io.aluCtrl.aluOp    := aluOp

}