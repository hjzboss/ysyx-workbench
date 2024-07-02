package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings
import javax.xml.transform.OutputKeys

class DebugIO extends Bundle {
  val pc        = Output(UInt(32.W))
  val nextPc    = Output(UInt(64.W))
  val inst      = Output(UInt(32.W))
  val valid     = Output(Bool())
}

class AxiMaster extends Bundle {
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awid    = Output(UInt(4.W))
  val awaddr  = Output(UInt(32.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))
  val wready  = Input(Bool())
  val wvalid  = Output(Bool())
  val wdata   = Output(UInt(64.W))
  val wstrb   = Output(UInt(8.W))
  val wlast   = Output(Bool())
  val bready  = Output(Bool())
  val bvalid  = Input(Bool())
  val bid     = Input(UInt(4.W))
  val bresp   = Input(UInt(2.W))
  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val arid    = Output(UInt(4.W))
  val araddr  = Output(UInt(32.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))
  val rready  = Output(Bool())
  val rvalid  = Input(Bool())
  val rid     = Input(UInt(4.W))
  val rdata   = Input(UInt(64.W))
  val rresp   = Input(UInt(2.W))
  val rlast   = Input(Bool())
}

class RamIO extends Bundle {
  val rdata     = Input(UInt(128.W))
  val cen       = Output(Bool())
  val wen       = Output(Bool())
  val wmask     = Output(UInt(128.W))
  val addr      = Output(UInt(6.W))
  val wdata     = Output(UInt(128.W)) 
}

// icache pipline
class Stage1IO extends Bundle {
  val addr      = Output(UInt(32.W))
  val npc       = Output(UInt(32.W))
  val cacheable = Output(Bool())
}

class Stage2IO extends Bundle {
  val pc        = Output(UInt(32.W))
  val npc       = Output(UInt(32.W))
  val cacheable = Output(Bool())
  val index     = Output(UInt(6.W))
  val tag       = Output(UInt(22.W))
  val align     = Output(UInt(2.W))
}

class Stage3IO extends Bundle {
  val npc       = Output(UInt(32.W))
  val pc        = Output(UInt(32.W))
  val index     = Output(UInt(6.W))
  val tag       = Output(UInt(22.W))
  val align     = Output(UInt(2.W))
  val cacheline = Output(UInt(128.W))
  val hit       = Output(Bool())
  val allocAddr = Output(UInt(32.W))
  val cacheable = Output(Bool())
  val victim    = Output(UInt(2.W))
}

class MetaAllocIO extends Bundle {
  val tag       = Output(UInt(22.W))
  val index     = Output(UInt(6.W))
  val victim    = Output(UInt(2.W))
  val valid     = Output(Bool())
}

// todo: cpu需要返回一个ready信号给cache，代表成功接受数据，此bundle需要进一步分离
class CacheCtrlIO extends Bundle {
  val addr      = Output(UInt(32.W))
  val wen       = Output(Bool())
  val cacheable = Output(Bool()) // cacheable or uncacheable, just for dcache
  val size      = Output(UInt(2.W)) // 外设访问时需要指定宽度
}

class CacheWriteIO extends Bundle {
  val wdata = Output(UInt(64.W))
  val wmask = Output(UInt(8.W))
}

class CacheReadIO extends Bundle {
  val rdata = Output(UInt(64.W))
}

class MetaData extends Bundle {
  val valid         = Bool()
  val dirty         = Bool()
  val tag           = UInt(22.W)
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
  val waddr     = Output(UInt(12.W))
  val wdata     = Output(UInt(64.W))
  val exception = Output(Bool())
  val epc       = Output(UInt(32.W))
  val no        = Output(UInt(64.W))
  val mret      = Output(Bool())
}

class DataSrcIO extends Bundle {
  val pc    = Output(UInt(32.W))
  val src1  = Output(UInt(64.W))
  val src2  = Output(UInt(64.W))
  val imm   = Output(UInt(64.W))
}

class AluIO extends Bundle {
  val aluSrc1 = Output(UInt(3.W))
  val aluSrc2 = Output(UInt(3.W))
  val aluOp   = Output(AluOp())
}

class CtrlFlow extends Bundle {
  val rd            = Output(UInt(5.W)) // 目的寄存器
  val regWen        = Output(Bool()) // 寄存器写使能
  val lsType        = Output(UInt(4.W)) // 访存指令类型
  //val loadMem       = Output(Bool()) // 写回的值是否来自存储器
  val wmask         = Output(UInt(8.W)) // 写腌码
  val csrWen        = Output(Bool()) // csr写使能
  val csrWaddr      = Output(UInt(12.W))
  val excepNo       = Output(UInt(64.W))
  val exception     = Output(Bool()) // 系统指令的类型
  val memWen        = Output(Bool()) // 存储器写使能
  val memRen        = Output(Bool()) // 存储器读使能
  val ebreak        = if(Settings.get("sim")) Some(Output(Bool())) else None
  val haltRet       = if(Settings.get("sim")) Some(Output(UInt(64.W))) else None
  val coherence     = Output(Bool())
  val rs1           = Output(UInt(5.W))
  val rs2           = Output(UInt(5.W))
  val mret          = Output(Bool())
  val csrChange     = Output(Bool())
}

class ExuOut extends Bundle {
  val lsType        = Output(UInt(4.W))
  val wmask         = Output(UInt(8.W))
  val lsuWen        = Output(Bool())
  val lsuRen        = Output(Bool())
  val lsuAddr       = Output(UInt(32.W))
  val lsuWdata      = Output(UInt(64.W))
  //val loadMem       = Output(Bool())

  val exuOut        = Output(UInt(64.W))
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())

  val pc            = Output(UInt(32.W))
  val excepNo       = Output(UInt(64.W))
  val exception     = Output(Bool())
  val csrWaddr      = Output(UInt(12.W))
  val csrWen        = Output(Bool())
  val csrValue      = Output(UInt(64.W))
  val coherence     = Output(Bool())
  val csrChange     = Output(Bool())
  val mret          = Output(Bool())
  val ebreak        = if(Settings.get("sim")) Some(Output(Bool())) else None
  val haltRet       = if(Settings.get("sim")) Some(Output(UInt(64.W))) else None
}

class LsuOut extends Bundle {
  val exuOut        = Output(UInt(64.W)) // exu的计算结果
  val lsuOut        = Output(UInt(64.W)) // lsu访存结果
  val loadMem       = Output(Bool())
  val rd            = Output(UInt(5.W))
  val regWen        = Output(Bool())
  val pc            = Output(UInt(32.W))
  val excepNo       = Output(UInt(64.W))
  val exception     = Output(Bool())
  val csrWaddr      = Output(UInt(12.W))
  val csrWen        = Output(Bool())
  val csrValue      = Output(UInt(64.W))
  val csrChange     = Output(Bool())
  val mret          = Output(Bool())
  val ebreak        = if(Settings.get("sim")) Some(Output(Bool())) else None
  val haltRet       = if(Settings.get("sim")) Some(Output(UInt(64.W))) else None
}

class InstrFetch extends Bundle {
  val pc            = Output(UInt(32.W))
  val npc           = Output(UInt(32.W))
  val inst          = Output(UInt(32.W))
}

class RedirectIO extends Bundle {
  val brAddr        = Output(UInt(32.W))
  val valid         = Output(Bool())
}

// axi接口
class RaddrIO extends Bundle {
  val addr          = Output(UInt(32.W))
  // 读突发信号
  val len           = Output(UInt(8.W))
  val size          = Output(UInt(3.W))
  val burst         = Output(UInt(2.W))
}

class WaddrIO extends Bundle {
  val addr          = Output(UInt(32.W))
  // 写突发信号
  val len           = Output(UInt(8.W))
  val size          = Output(UInt(3.W))
  val burst         = Output(UInt(2.W))
}

class RdataIO extends Bundle {
  val rdata         = Output(UInt(64.W))
  val rresp         = Output(UInt(2.W))
  val rlast         = Output(Bool())
}

class WdataIO extends Bundle {
  val wdata         = Output(UInt(64.W))
  val wstrb         = Output(UInt(8.W))
  val wlast         = Output(Bool())
}

class BrespIO extends Bundle {
  val bresp         = Output(UInt(2.W))
}

class MultiInput extends Bundle {
  val valid           = Output(Bool())
  val multiplicand    = Output(UInt(64.W)) // 被乘数
  val multiplier      = Output(UInt(64.W)) // 乘数
  val mulw            = Output(Bool()) // 是否为32位乘法
  val mulSigned       = Output(UInt(2.W)) // 乘法类型
}

class MultiOutput extends Bundle {
  val resultHi        = Output(UInt(64.W))
  val resultLo        = Output(UInt(64.W))
}

class DivInput extends Bundle {
  val valid           = Output(Bool())
  val dividend        = Output(UInt(64.W))
  val divisor         = Output(UInt(64.W))
  val divw            = Output(Bool())
  val divSigned       = Output(Bool())
}

class DivOutput extends Bundle {
  val quotient        = Output(UInt(64.W))
  val remainder       = Output(UInt(64.W))
}

class CoherenceIO extends Bundle {
  val valid           = Output(Bool())
  val ready           = Input(Bool())
}

class ClintIO extends Bundle {
  val addr            = Input(UInt(32.W))
  val rdata           = Output(UInt(64.W))
  val wen             = Input(Bool())
  val wdata           = Input(UInt(64.W))
  val wmask           = Input(UInt(8.W))
}

class BTBPredIO extends Bundle {
  // predict
  val brType          = Output(BrType())
  val target          = Output(UInt(32.W))
  val hit             = Output(Bool())
}

class BPUTrainIO extends Bundle {
  val train           = Input(Bool())
  val invalid         = Input(Bool())
  val pc              = Input(UInt(32.W))
  val target          = Input(UInt(32.W))
  val brType          = Input(BrType())
}

class PerfIO extends Bundle {
  val icacheHitCnt = Output(UInt(64.W))
  val icacheReqCnt = Output(UInt(64.W))
  val dcacheHitCnt = Output(UInt(64.W))
  val dcacheReqCnt = Output(UInt(64.W))
  val bpuMissCnt   = Output(UInt(64.W))
  val bpuReqCnt    = Output(UInt(64.W))
}