package jzcore

import chisel3._

class RFReadIO extends Bundle {
  val rs1   = Output(UInt(5.W))
  val rs2   = Output(UInt(5.W))
}

class RFWriteIO extends Bundle {
  val wen   = Output(Bool())
  val rd    = Output(UInt(5.W))
  val value = Output(UInt(64.W))
}

class CSRWriteIO extends Bundle {
  val wen   = Output(Bool())
  val waddr = Output(UInt(2.W))
  val wdata = Output(UInt(64.W))
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
  val aluOp   = Output(UInt(6.W))
}

class Ctrl extends Bundle {
  val br            = Output(Bool())
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())
  val break         = Output(Bool())
  val isJalr        = Output(Bool())
  val lsType        = Output(UInt(4.W))
  val wdata         = Output(UInt(64.W))
  val loadMem       = Output(Bool())
  val wmask         = Output(UInt(8.W))
  val isCsr         = Output(Bool())
  val csrWaddr      = Output(UInt(2.W))
}

class InstrFetch extends Bundle {
  val pc        = Output(UInt(64.W))
  val inst      = Output(UInt(32.W))
}

class RedirectIO extends Bundle {
  val brAddr    = Output(UInt(64.W))
  val valid     = Output(Bool())
}
