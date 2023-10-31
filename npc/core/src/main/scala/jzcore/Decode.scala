package jzcore

import chisel3._
import chisel3.util._

object SrcType {
  def reg    = "b000".U
  def imm    = "b001".U
  def pc     = "b010".U
  def nul    = "b011".U
  def plus4  = "b100".U

  def apply() = UInt(3.W)
}

object AluOp {
  def add       = "b0000000".U
  def sub       = "b0000001".U
  def and       = "b0000010".U
  def or        = "b0001101".U
  def xor       = "b0011110".U
  def slt       = "b0000101".U
  def sltu      = "b0000110".U
  def sll       = "b0000100".U
  def srl       = "b0001000".U
  def sra       = "b0001001".U
  // todo
  def nop       = "b0001100".U

  def jump      = "b0001110".U

  def beq       = "b0011111".U
  def bne       = "b0010111".U
  def bge       = "b0001011".U
  def blt       = "b0011011".U
  def bltu      = "b0010011".U
  def bgeu      = "b0001111".U

  def addw      = "b1010101".U
  def subw      = "b1010110".U
  def sllw      = "b1011001".U
  def srlw      = "b1011010".U
  def sraw      = "b1011101".U

  def mul       = "b0111000".U
  def mulw      = "b1111101".U
  def mulh      = "b0111110".U
  def mulhsu    = "b0111010".U
  def mulhu     = "b0110000".U

  def div       = "b0101010".U
  def divw      = "b1101010".U
  def divu      = "b0101100".U
  def divuw     = "b1101000".U
  def rem       = "b0101110".U
  def remw      = "b1100010".U
  def remu      = "b0101001".U
  def remuw     = "b1101001".U

  def csrrw     = "b0010110".U
  def csrrs     = "b0001010".U
  def csrrc     = "b0010101".U

  def apply() = UInt(7.W)
  def isWordOp(op: UInt): Bool = op(6)
  def mulOp(op: UInt): Bool = { op(5) & op(4) } 
  def divOp(op: UInt): Bool = { op(5) & ~op(4) }
  def Bop(op: UInt): Bool = { op(0) & op(1) }
  def divSigned(op: UInt): Bool = op(1)
}

object Wmask {
  def double  = "b11111111".U
  def word    = "b00001111".U 
  def half    = "b00000011".U
  def byte    = "b00000001".U
  def nop     = "b00000000".U

  def apply() = UInt(8.W)
}

object LsType {
  def ld      = "b0000".U
  def lw      = "b0001".U
  def lh      = "b0010".U
  def lb      = "b0011".U
  def lbu     = "b0100".U
  def lhu     = "b0101".U
  def lwu     = "b1011".U
  def sd      = "b0110".U
  def sw      = "b0111".U
  def sh      = "b1000".U
  def sb      = "b1001".U
  def nop     = "b1010".U

  def apply() = UInt(4.W)
}

object RegWrite {
  def loadMem = true.B
  def loadAlu = false.B

  def apply() = Bool()
}

object CsrId {
  def mstatus = 0x300.U
  def mtvec   = 0x305.U
  def mepc    = 0x341.U
  def mcause  = 0x342.U
  def mie     = 0x304.U
  def mip     = 0x344.U
  def nul     = 0x000.U

  def apply() = UInt(12.W)
}

/*
object CsrAddr {
  def mstatus = "b000".U
  def mtvec   = "b001".U
  def mepc    = "b010".U
  def mcause  = "b011".U
  def mie     = "b100".U
  def mip     = "b101".U
  def nul     = "b110".U

  def apply() = UInt(3.W)
}*/

object System {
  def nop    = "b00".U
  def mret   = "b01".U
  def ecall  = "b10".U

  def apply() = UInt(2.W)
}

object MemEn {
  def load    = "b00".U
  def store   = "b01".U
  def nop     = "b10".U

  def apply() = UInt(2.W)
}

object Forward {
  def normal      = "b00".U
  def lsuData     = "b01".U
  def wbuData     = "b10".U

  def apply() = UInt(2.W)
}

trait HasInstrType {
  def InstrN  = "b00000".U
  def InstrI  = "b10100".U
  def InstrR  = "b10001".U
  def InstrS  = "b00010".U
  def InstrB  = "b00101".U
  def InstrU  = "b10110".U
  def InstrJ  = "b10111".U
  def InstrIJ = "b10101".U
  def InstrZ  = "b11001".U // csr
  def InstrE  = "b01010".U // exception
  def InstrD  = "b01011".U // ebreak, just for simulation
  def InstrF  = "b01100".U // fencei

  def regWen(instr: UInt): Bool = instr(4)
  def isCsr(instr: UInt): Bool = { instr(4) & instr(3) }
  def isBr(instr: UInt): Bool = { instr(0) & instr(2) }
}

object MulType {
  //def nop = "b00".U
  def ss  = "b11".U
  def su  = "b10".U
  def uu  = "b00".U

  def apply = UInt(2.W)
}

object AxiWidth {
  def byte = "b00".U
  def half = "b01".U
  def word = "b10".U
  def double = "b11".U

  def apply = UInt(2.W)
}