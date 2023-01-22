package jzcore

import chisel3._
import chisel3.util._
import utils._

class Alu extends Module {
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
    AluOp.add       -> (opA + opB),
    AluOp.sub       -> (opA - opB),
    AluOp.and       -> (opA & opB),
    AluOp.or        -> (opA | opB),
    AluOp.xor       -> (opA ^ opB),
    //LessThan  -> Mux(opA.asSInt() < opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    AluOp.lessThanU -> Mux(opA < opB, 1.U(64.W), 0.U(64.W)),
    AluOp.moveLeft  -> (opA << opB(5, 0)),
    AluOp.logicMovR -> (opA >> opB(5, 0)),
    //ArithMovR -> (opA.asSInt() >> opB),
    // todo
    AluOp.div       -> (opA / opB),
    AluOp.times     -> (opA * opB),
  ))

  io.aluOut := aluOut
  io.brMark := brMark
}