package jzcore

import chisel3._
import chisel3.util._
import utils._

class Alu extends Module with AluCtrlDecode {
  val io = IO(new Bundle {
    val opA     = Input(UInt(64.W))
    val opB     = Input(UInt(64.W))
    val aluOp   = Input(UInt(4.W))
    
    val aluOut  = Output(UInt(64.W))
    val brMark  = Output(Bool())
  })

  val opA = io.opA
  val opB = io.opB
  val aluOp = io.aluOp

  // todo
  val brMark = false.B

  // 需要将有符号数和无符号数分开，否则报错
  val aluOut = LookupTree(io.aluOp, List(
    add       -> (opA + opB),
    sub       -> (opA - opB),
    and       -> (opA & opB),
    or        -> (opA | opB),
    xor       -> (opA ^ opB),
    //LessThan  -> Mux(opA.asSInt() < opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    lessThanU -> Mux(opA < opB, 1.U(64.W), 0.U(64.W)),
    moveLeft  -> (opA << opB(5, 0)),
    logicMovR -> (opA >> opB(5, 0)),
    //ArithMovR -> (opA.asSInt() >> opB),
    // todo
    div       -> (opA / opB),
    times     -> (opA * opB),
  ))

  io.aluOut := aluOut
  io.brMark := brMark
}