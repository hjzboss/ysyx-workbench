package jzcore

import chisel3._

class DebugIO extends Bundle {
  val pc        = Output(UInt(64.W))
  val nextPc    = Output(UInt(64.W))
  val inst      = Output(UInt(32.W))
  val execonce  = Output(Bool())
}

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
  val wen       = Output(Bool())
  val waddr     = Output(UInt(2.W))
  val wdata     = Output(UInt(64.W))
  val exception = Output(Bool())
  val epc       = Output(UInt(64.W))
  val no        = Output(UInt(4.W))
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

class CtrlFlow extends Bundle {
  val br            = Output(Bool())
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())
  val isJalr        = Output(Bool())
  val lsType        = Output(UInt(4.W))
  val wdata         = Output(UInt(64.W))
  val loadMem       = Output(Bool())
  val wmask         = Output(UInt(8.W))
  val isCsr         = Output(Bool())
  val csrWaddr      = Output(UInt(2.W))
  val sysInsType    = Output(UInt(2.W))
  val memWen        = Output(Bool())
  val memRen        = Output(Bool())
}

class MemCtrl extends Bundle {
  val lsType        = Output(UInt(4.W))
  val wmask         = Output(UInt(8.W))
  val wen           = Output(Bool())
  val ren           = Output(Bool())
  val addr          = Output(UInt(64.W))
  val wdata         = Output(UInt(64.W))
  val loadMem       = Output(Bool())

  val exuOut        = Output(UInt(64.W))
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())

  val pc            = Output(UInt(64.W))
  val no            = Output(UInt(4.W))
  val exception     = Output(Bool())
  val csrWaddr      = Output(UInt(2.W))
  val csrWen        = Output(Bool())
}

class LsuOut extends Bundle {
  val exuOut        = Output(UInt(64.W)) // exu的计算结果
  val lsuOut        = Output(UInt(64.W)) // lsu访存结果
  val loadMem       = Output(Bool())
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())
  val pc            = Output(UInt(64.W))
  val no            = Output(UInt(4.W))
  val exception     = Output(Bool())
  val csrWaddr      = Output(UInt(2.W))
  val csrWen        = Output(Bool())
}

class InstrFetch extends Bundle {
  val pc            = Output(UInt(64.W))
  val inst          = Output(UInt(32.W))
}

class RedirectIO extends Bundle {
  val brAddr        = Output(UInt(64.W))
  val valid         = Output(Bool())
}

// axi接口
class AddrIO extends Bundle {
  val addr          = Output(UInt(64.W))
}

class RdataIO extends Bundle {
  val rdata         = Output(UInt(64.W))
  val rresp         = Output(UInt(2.W))
}

class WdataIO extends Bundle {
  val wdata         = Output(UInt(64.W))
  val wstrb         = Output(UInt(8.W))
}

class BrespIO extends Bundle {
  val bresp         = Output(UInt(2.W))
}