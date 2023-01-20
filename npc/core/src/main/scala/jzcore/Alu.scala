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
  })

  val opA = io.opA
  val opB = io.opB
  val aluOp = io.aluOp
  val aluOut = LookupTree(io.aluOp, List(
    Add       -> (opA + opB),
    Sub       -> (opA - opB),
    And       -> (opA & opB),
    Or        -> (opA | opB),
    Xor       -> (opA ^ opB),
    //LessThan  -> Mux(opA.asSInt() < opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    LessThanU -> Mux(opA < opB, 1.U(64.W), 0.U(64.W)),
    MoveLeft  -> (opA << opB),
    LogicMovR -> (opA >> opB),
    //ArithMovR -> (opA.asSInt() >> opB),
    // todo
    Div       -> (opA / opB),
    Times     -> (opA * opB)
  ))

  io.aluOut := aluOut
}