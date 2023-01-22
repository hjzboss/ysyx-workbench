package jzcore

import chisel3._
import chisel3.util._

object RV64IM extends HasInstrType {
  def ADDI    = BitPat("b???????_?????_?????_000_?????_0010011")
  def EBREAK  = BitPat("b0000000_00001_00000_000_00000_1110011")

  val table = Array (
    ADDI    -> List(InstrI, SrcType.reg, SrcType.imm, AluOp.add),
    EBREAK  -> List(InstrD, SrcType.nul, SrcType.nul, AluOp.nop)
  )
}

object Instruction extends HasInstrType {
  def NOP = 0x00000013.U
  val DecodeDefault = List(InstrN, AluOp.nop, SrcType.nul, SrcType.nul)
}