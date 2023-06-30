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
  def add       = "b000000".U
  def sub       = "b000001".U
  def and       = "b000010".U
  def or        = "b000011".U
  def xor       = "b000100".U
  def slt       = "b000101".U
  def sltu      = "b000110".U
  def sll       = "b000111".U
  def srl       = "b001000".U
  def sra       = "b001001".U
  def rem       = "b001101".U
  // todo
  def div       = "b001010".U
  def mul       = "b001011".U

  def nop       = "b001100".U

  def jump      = "b001110".U

  def beq       = "b001111".U
  def bne       = "b010000".U
  def bge       = "b010001".U
  def blt       = "b010010".U
  def bltu      = "b010011".U
  def bgeu      = "b010100".U

  def addw      = "b010101".U
  def subw      = "b010110".U
  def mulw      = "b010111".U
  def divw      = "b011000".U
  def sllw      = "b011001".U
  def srlw      = "b011010".U
  def sraw      = "b011011".U
  def remw      = "b011100".U

  def mulh      = "b011101".U
  def mulhsu    = "b011110".U
  def mulhu     = "b011111".U
  def divu      = "b100000".U
  def remu      = "b100001".U
  def divuw     = "b100010".U
  def remuw     = "b100011".U

  def csrrw     = "b100100".U
  def csrrs     = "b100101".U
  def csrrc     = "b100110".U

  def apply() = UInt(6.W)
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
  def mstatus = "h300".U
  def mtvec   = "h305".U
  def mepc    = "h341".U
  def mcause  = "h342".U

  def apply() = UInt(12.W)
}

object CsrAddr {
  def mstatus = "b000".U
  def mtvec   = "b001".U
  def mepc    = "b010".U
  def mcause  = "b011".U
  def nul     = "b100".U

  def apply() = UInt(3.W)
}

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
  def normal      = "b000".U
  def lsuData     = "b001".U
  def wbuData     = "b010".U
  def csrWbuData  = "b011".U
  def csrLsuData  = "b100".U

  def apply() = UInt(3.W)
}

trait HasInstrType {
  def InstrN  = "b0000".U
  def InstrI  = "b0100".U
  def InstrR  = "b0101".U
  def InstrS  = "b0010".U
  def InstrB  = "b0001".U
  def InstrU  = "b0110".U
  def InstrJ  = "b0111".U
  def InstrIJ = "b1000".U
  def InstrZ  = "b1001".U // csr
  def InstrE  = "b1010".U // exception
  def InstrD  = "b1011".U // ebreak, just for simulation
  def InstrF  = "b1100".U // fencei
}

object MulType {
  //def nop = "b00".U
  def ss  = "b11".U
  def su  = "b10".U
  def uu  = "b00".U

  def apply = UInt(2.W)
}