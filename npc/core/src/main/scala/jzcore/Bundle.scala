package jzcore

import chisel3._

class DebugIO extends Bundle {
  val pc        = Output(UInt(64.W))
  val nextPc    = Output(UInt(64.W))
  val inst      = Output(UInt(32.W))
  //val execonce  = Output(Bool())
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
  val waddr     = Output(UInt(3.W))
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
  val br            = Output(Bool()) // 分支指令信号
  val rd            = Output(UInt(5.W)) // 目的寄存器
  val regWen        = Output(Bool()) // 寄存器写使能
  val isJalr        = Output(Bool()) // 是否三jalr指令
  val lsType        = Output(UInt(4.W)) // 访存指令类型
  //val wdata         = Output(UInt(64.W)) // 写数据
  val loadMem       = Output(Bool()) // 写回的值是否来自存储器
  val wmask         = Output(UInt(8.W)) // 写腌码
  val csrWen        = Output(Bool()) // 是否是csr指令
  val csrWaddr      = Output(UInt(3.W))
  val excepNo       = Output(UInt(4.W))
  val exception     = Output(Bool()) // 系统指令的类型
  val memWen        = Output(Bool()) // 存储器写使能
  val memRen        = Output(Bool()) // 存储器读使能
  val ebreak        = Output(Bool()) // ebreak指令，用于停止仿真

  // 用于送给旁路单元
  val rs1           = Output(UInt(5.W))
  val rs2           = Output(UInt(5.W))
}

class ExuOut extends Bundle {
  val lsType        = Output(UInt(4.W))
  val wmask         = Output(UInt(8.W))
  val lsuWen        = Output(Bool())
  val lsuRen        = Output(Bool())
  val lsuAddr       = Output(UInt(64.W))
  val lsuWdata      = Output(UInt(64.W))
  val loadMem       = Output(Bool())

  val exuOut        = Output(UInt(64.W))
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())

  val pc            = Output(UInt(64.W))
  val excepNo       = Output(UInt(4.W))
  val exception     = Output(Bool())
  val csrWaddr      = Output(UInt(3.W))
  val csrWen        = Output(Bool())
  val csrValue      = Output(UInt(64.W))

  val ebreak        = Output(Bool()) // ebreak指令，用于停止仿真
  val haltRet       = Output(UInt(64.W))
}

class LsuOut extends Bundle {
  val exuOut        = Output(UInt(64.W)) // exu的计算结果
  val lsuOut        = Output(UInt(64.W)) // lsu访存结果
  val loadMem       = Output(Bool())
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())
  val pc            = Output(UInt(64.W))
  val excepNo       = Output(UInt(4.W))
  val exception     = Output(Bool())
  val csrWaddr      = Output(UInt(3.W))
  val csrWen        = Output(Bool())
  val csrValue      = Output(UInt(64.W))

  val ebreak        = Output(Bool()) // ebreak指令，用于停止仿真
  val haltRet       = Output(UInt(64.W))
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
class RaddrIO extends Bundle {
  val addr          = Output(UInt(64.W))
}

class WaddrIO extends Bundle {
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