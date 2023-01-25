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
  def add       = "b0000".U
  def sub       = "b0001".U
  def and       = "b0010".U
  def or        = "b0011".U
  def xor       = "b0100".U
  def slt       = "b0101".U
  def lstu      = "b0110".U
  def sll       = "b0111".U
  def srl       = "b1000".U
  def sra       = "b1001".U
  // todo
  def div       = "b1010".U
  def times     = "b1011".U
  def nop       = "b1100".U
  def jump      = "b1101".U
  def notEq     = "b1110".U

  def apply() = UInt(4.W)
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
  def InstrD  = "b1001".U
}