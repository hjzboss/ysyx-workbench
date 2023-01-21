package jzcore

import chisel3._

class RFReadIO extends Bundle {
  val rs1   = Output(UInt(5.W))
  val rs2   = Output(UInt(5.W))
  val ren1  = Output(Bool())
  val ren2  = Output(Bool())
}

class RFWriteIO extends Bundle {
  val wen   = Output(Bool())
  val rd    = Output(UInt(5.W))
  val value = Output(UInt(64.W))
}

class DataSrcIO extends Bundle {
  val pc    = Output(UInt(64.W))
  val src1  = Output(UInt(64.W))
  val src2  = Output(UInt(64.W))
  val imm   = Output(UInt(64.W))
}

class AluIO extends Bundle {
  val aluSrc1 = Output(UInt(3.W))
  val aluSrc2 = Output(UInt(3.W))
  val aluOp   = Output(UInt(4.W))
}

class Ctrl extends Bundle {
  val br        = Output(Bool())
  val rd        = Output(UInt(5.W))
  val regWen    = Output(Bool())
}

class InstrFetch extends Bundle {
  val pc        = Output(UInt(64.W))
  val inst      = Output(UInt(32.W))
}

class BranchCtrl extends Bundle {
  val brAddr    = Output(UInt(64.W))
  val brCtrl    = Output(Bool())
}

class IDUOut extends Bundle {
  val datasrc   = new DataSrcIO
  val aluCtrl   = new AluIO
  val ctrl      = new Ctrl
}

class EXUOut extends Bundle {
  val regWrite  = new RFWriteIO
  val branch    = new BranchCtrl
}

class JzCoreBundle extends Bundle {
  val iduout = new IDUOut
  val exuout = new EXUOut
}