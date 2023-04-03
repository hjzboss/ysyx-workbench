package jzcore

import chisel3._

class RFReadIO extends Bundle {
  val rs1   = Output(UInt(5.W))
  val rs2   = Output(UInt(5.W))
}

class RFWriteIO extends Bundle {
  val rd    = Output(UInt(5.W))
  val value = Output(UInt(64.W))
}

class CSRWriteIO extends Bundle {
  val waddr     = Output(UInt(2.W))
  val wdata     = Output(UInt(64.W))
  val exception = Output(Bool())
  val epc       = Output(UInt(64.W))
  val no        = Output(UInt(4.W))
}

class CtrlFlow extends Bundle {
  // ctrl
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
  // alu ctrl
  val aluSrc1       = Output(UInt(3.W))
  val aluSrc2       = Output(UInt(3.W))
  val aluOp         = Output(UInt(6.W))
  // data
  val pc            = Output(UInt(64.W))
  val src1          = Output(UInt(64.W))
  val src2          = Output(UInt(64.W))
  val imm           = Output(UInt(64.W))
}

class InstrFetch extends Bundle {
  val pc        = Output(UInt(64.W))
  val inst      = Output(UInt(32.W))
}

class RedirectIO extends Bundle {
  val brAddr    = Output(UInt(64.W))
}

class AddrIO extends Bundle {
  val addr      = Output(UInt(64.W))
}

class RdataIO extends Bundle {
  val data      = Output(UInt(64.W))
  val rresp     = Output(UInt(2.W))
}

// axi 写数据通道
class WdataIO extends Bundle {
  val data      = Output(UInt(64.W))
  val wstrb     = Output(UInt(8.W))
}

// axi 写回应通道
class BIO extends Bundle {
  val bresp     = Output(UInt(2.W))
}
