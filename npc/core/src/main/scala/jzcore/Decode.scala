package jzcore

import chisel3._
import chisel3.util._

trait HasOpDecode {
  def ItypeL  = "b0000011".U  // load
  def ItypeA  = "b0010011".U  // I type alu
  def ItypeW  = "b0011011".U  // I type word
  def ItypeJ  = "b1100111".U  // jalr
  def Rtype   = "b0110011".U
  def RtypeW  = "b0111011".U  // R type word
  def UtypeL  = "b0110111".U  // lui
  def UtypeU  = "b0010111".U  // auipc
  def Jtype   = "b1101111".U  // jal
  def Stype   = "b0100011".U  // store
  def Btype   = "b1100011".U
}

trait HasSrcDecode {
  def SrcReg    = "b000".U
  def SrcImm    = "b001".U
  def SrcPc     = "b010".U
  def SrcNull   = "b011".U
  def SrcPlus4  = "b100".U
}

trait AluCtrlDecode {
  def Add       = "b0000".U
  def Sub       = "b0001".U
  def And       = "b0010".U
  def Or        = "b0011".U
  def Xor       = "b0100".U
  def LessThan  = "b0101".U
  def LessThanU = "b0110".U
  def MoveLeft  = "b0111".U
  def LogicMovR = "b1000".U
  def ArithMovR = "b1001".U
  // todo
  def Div       = "b1010".U
  def Times     = "b1011".U
}
