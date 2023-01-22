package jzcore

import chisel3._
import chisel3.util._
import utils._

class IDU extends Module with HasInstrType {
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

  val ctrlList = ListLookup(inst, Instruction.DecodeDefault, RV64IM.table)
  val type = ctrlList.head
  val aluOp = ctrlList.tail
  val aluSrc1 = ctrlList(1)
  val aluSrc2 = ctrlList(2)
  val imm = LookupTree(type, List(
    InstrI    -> SignExt(inst(31, 20), 64),
    InstrIJ   -> SignExt(inst(31, 20), 64),
    InstrS    -> SignExt(Cat(inst(31, 25), inst(11, 7)), 64),
    InstrB    -> SignExt(Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)), 64),
    InstrU    -> SignExt(Cat(inst(31, 12), 0.U(12.W)), 64),
    InstrJ    -> SignExt(Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)), 64)
  ))


  // registerfile
  rf.io.read.rs1     := rs1
  rf.io.read.rs2     := rs2
  rf.io.write        := io.regWrite

  io.datasrc.pc       := io.fetch.pc
  io.datasrc.src1     := rf.io.src1
  io.datasrc.src2     := rf.io.src2
  io.datasrc.imm      := imm

  io.ctrl.rd          := rd
  io.ctrl.br          := Mux(type === InstrIJ || type === InstrJ || type === InstrB, true.B, false.B)
  io.ctrl.regWen      := Mux(type === InstrB || type === InstrS, false.B, true.B)

  io.aluCtrl.aluSrc1  := aluSrc1
  io.aluCtrl.aluSrc2  := aluSrc2
  io.aluCtrl.aluOp    := aluOp

  // ebreak
  val stop = Module(new stop)
  stop.io.valid = Mux(type === InstrD, true.B, false.B)
}