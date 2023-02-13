package jzcore

import chisel3._
import chisel3.util._

object RV64IM extends HasInstrType {
  def ADD     = BitPat("b0000000_?????_?????_000_?????_0110011")
  def SUB     = BitPat("b0100000_?????_?????_000_?????_0110011")
  def SLL     = BitPat("b0000000_?????_?????_001_?????_0110011")
  def SLT     = BitPat("b0000000_?????_?????_010_?????_0110011")
  def SLTU    = BitPat("b0000000_?????_?????_011_?????_0110011")
  def XOR     = BitPat("b0000000_?????_?????_100_?????_0110011")
  def SRL     = BitPat("b0000000_?????_?????_101_?????_0110011")
  def SRA     = BitPat("b0100000_?????_?????_101_?????_0110011")
  def OR      = BitPat("b0000000_?????_?????_110_?????_0110011")
  def AND     = BitPat("b0000000_?????_?????_111_?????_0110011")
  def MUL     = BitPat("b0000001_?????_?????_000_?????_0110011")
  def DIV     = BitPat("b0000001_?????_?????_100_?????_0110011")

  def ADDW    = BitPat("b0000000_?????_?????_000_?????_0111011")
  def SUBW    = BitPat("b0100000_?????_?????_000_?????_0111011")
  def MULW    = BitPat("b0000001_?????_?????_000_?????_0111011")
  def DIVW    = BitPat("b0000001_?????_?????_100_?????_0111011")
  def SLLW    = BitPat("b0000000_?????_?????_001_?????_0111011")
  def SRLW    = BitPat("b0000000_?????_?????_101_?????_0111011")
  def SRAW    = BitPat("b0100000_?????_?????_101_?????_0111011")
  def REMW    = BitPat("b0000001_?????_?????_110_?????_0111011")

  def LD      = BitPat("b???????_?????_?????_011_?????_0000011")
  def LW      = BitPat("b???????_?????_?????_010_?????_0000011")
  def LH      = BitPat("b???????_?????_?????_001_?????_0000011")
  def LB      = BitPat("b???????_?????_?????_000_?????_0000011")
	def LBU     = BitPat("b???????_?????_?????_100_?????_0000011")
  def LHU     = BitPat("b???????_?????_?????_101_?????_0000011")

  def ADDI    = BitPat("b???????_?????_?????_000_?????_0010011")
  def SLTI    = BitPat("b???????_?????_?????_010_?????_0010011")
  def SLTIU   = BitPat("b???????_?????_?????_011_?????_0010011")
  def XORI    = BitPat("b???????_?????_?????_100_?????_0010011")
  def ORI     = BitPat("b???????_?????_?????_110_?????_0010011")
  def ANDI    = BitPat("b???????_?????_?????_111_?????_0010011")
  def SLLI    = BitPat("b000000?_?????_?????_001_?????_0010011")
  def SRLI    = BitPat("b000000?_?????_?????_101_?????_0010011")
  def SRAI    = BitPat("b010000?_?????_?????_101_?????_0010011")
  def ADDIW   = BitPat("b???????_?????_?????_000_?????_0011011")
  def SLLIW   = BitPat("b0000000_?????_?????_001_?????_0011011")
  def SRLIW   = BitPat("b0000000_?????_?????_101_?????_0011011")
  def SRAIW   = BitPat("b0100000_?????_?????_101_?????_0011011")

  def EBREAK  = BitPat("b0000000_00001_00000_000_00000_1110011")

  def AUIPC   = BitPat("b???????_?????_?????_???_?????_0010111")
  def LUI     = BitPat("b???????_?????_?????_???_?????_0110111")

  def JAL     = BitPat("b???????_?????_?????_???_?????_1101111")
  def JALR    = BitPat("b???????_?????_?????_???_?????_1100111")

  def BEQ     = BitPat("b???????_?????_?????_000_?????_1100011")
  def BNE     = BitPat("b???????_?????_?????_001_?????_1100011")
  def BLT     = BitPat("b???????_?????_?????_100_?????_1100011")
  def BGE     = BitPat("b???????_?????_?????_101_?????_1100011")
  def BLTU    = BitPat("b???????_?????_?????_110_?????_1100011")
  def BGEU    = BitPat("b???????_?????_?????_111_?????_1100011")

  def SD      = BitPat("b???????_?????_?????_011_?????_0100011")
  def SW      = BitPat("b???????_?????_?????_010_?????_0100011")
  def SH      = BitPat("b???????_?????_?????_001_?????_0100011")
  def SB      = BitPat("b???????_?????_?????_000_?????_0100011")


  val table = Array(
    ADD     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.add, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SUB     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.sub, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLL     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.sll, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLT     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.slt, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLTU    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.sltu, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    XOR     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.xor, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRL     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.srl, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRA     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.sra, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    OR      -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.or, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    AND     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.and, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    MUL     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.mul, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    DIV     -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.div, LsType.nop, RegWrite.loadAlu, Wmask.nop),

    ADDW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.addw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SUBW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.subw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    MULW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.mulw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    DIVW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.divw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLLW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.sllw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRLW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.srlw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRAW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.sraw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    REMW    -> List(InstrR, SrcType.reg, SrcType.reg, AluOp.remw, LsType.nop, RegWrite.loadAlu, Wmask.nop),

    LD      -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add, LsType.ld, RegWrite.loadMem, Wmask.nop),
    LW      -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add, LsType.lw, RegWrite.loadMem, Wmask.nop),
    LH      -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add, LsType.lh, RegWrite.loadMem, Wmask.nop),
    LB      -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add, LsType.lb, RegWrite.loadMem, Wmask.nop),
    LBU     -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add, LsType.lbu, RegWrite.loadMem, Wmask.nop),
    LHU     -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add, LsType.lhu, RegWrite.loadMem, Wmask.nop),
    ADDI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLTI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.slt, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLTIU   -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.sltu, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    XORI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.xor, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    ORI     -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.or, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    ANDI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.and, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLLI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.sll, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRLI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.srl, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRAI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.sra, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    ADDIW   -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.addw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SLLIW   -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.sllw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRLIW   -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.srlw, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    SRAIW   -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.sraw, LsType.nop, RegWrite.loadAlu, Wmask.nop),

    EBREAK  -> List(InstrD, SrcType.nul, SrcType.nul, AluOp.nop, LsType.nop, RegWrite.loadAlu, Wmask.nop),

    AUIPC   -> List(InstrU, SrcType.pc, SrcType.imm, AluOp.add, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    LUI     -> List(InstrU, SrcType.nul, SrcType.imm, AluOp.add, LsType.nop, RegWrite.loadAlu, Wmask.nop),

    JAL     -> List(InstrJ, SrcType.pc, SrcType.plus4, AluOp.jump, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    JALR    -> List(InstrIJ, SrcType.pc, SrcType.plus4, AluOp.jump, LsType.nop, RegWrite.loadAlu, Wmask.nop),

    BEQ     -> List(InstrB, SrcType.pc, SrcType.imm, AluOp.beq, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    BNE     -> List(InstrB, SrcType.pc, SrcType.imm, AluOp.bne, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    BLT     -> List(InstrB, SrcType.pc, SrcType.imm, AluOp.blt, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    BGE     -> List(InstrB, SrcType.pc, SrcType.imm, AluOp.bge, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    BLTU    -> List(InstrB, SrcType.pc, SrcType.imm, AluOp.bltu, LsType.nop, RegWrite.loadAlu, Wmask.nop),
    BGEU    -> List(InstrB, SrcType.pc, SrcType.imm, AluOp.bgeu, LsType.nop, RegWrite.loadAlu, Wmask.nop),

    SD      -> List(InstrS, SrcType.reg, SrcType.imm, AluOp.add, LsType.sd, RegWrite.loadAlu, Wmask.double),
    SW      -> List(InstrS, SrcType.reg, SrcType.imm, AluOp.add, LsType.sw, RegWrite.loadAlu, Wmask.word),
    SH      -> List(InstrS, SrcType.reg, SrcType.imm, AluOp.add, LsType.sh, RegWrite.loadAlu, Wmask.half),
    SB      -> List(InstrS, SrcType.reg, SrcType.imm, AluOp.add, LsType.sb, RegWrite.loadAlu, Wmask.byte),
  )

/*
  val lsTable = Array(
    LD      -> List(LsType.ld, true.B),
    LW      -> List(LsType.lw, true.B),
    LH      -> List(LsType.lh, true.B),
    LB      -> List(LsType.lb, true.B),
    LBU     -> List(LsType.lbu, true.B),
    LHU     -> List(LsType.lhu, true.B),

    SD      -> List(LsType.sd, false.B),
    SW      -> List(LsType.sw, false.B),
    SH      -> List(LsType.sh, false.B),
    SB      -> List(LsType.sb, false.B),
  )
  */
}

object Instruction extends HasInstrType {
  def NOP = 0x00000013.U
  val DecodeDefault = List(InstrN, AluOp.nop, SrcType.nul, SrcType.nul, LsType.nop, RegWrite.loadAlu, Wmask.nop)
  //val LsDefault     = List(LsType.nop, false.B)
}