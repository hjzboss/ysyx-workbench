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

  // 需要将有符号数和无符号数分开，否则报错
  val aluOut = LookupTree(io.aluOp, List(
    AluOp.add       -> (opA + opB),
    AluOp.jump      -> (opA + opB),
    AluOp.sub       -> (opA - opB),
    AluOp.notEq     -> (opA - opB),
    AluOp.and       -> (opA & opB),
    AluOp.or        -> (opA | opB),
    AluOp.xor       -> (opA ^ opB),
    //LessThan  -> Mux(opA.asSInt() < opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    AluOp.sltu      -> Mux(opA < opB, 1.U(64.W), 0.U(64.W)),
    AluOp.sll       -> (opA << opB(5, 0)),
    AluOp.srl       -> (opA >> opB(5, 0)),
    //ArithMovR -> (opA.asSInt() >> opB),
    // todo
    AluOp.div       -> (opA / opB),
    AluOp.times     -> (opA * opB)
  ))

  val zero = Mux(aluOut === 0.U(64.W), true.B, false.B)

  io.aluOut := aluOut
  io.brMark := Mux(aluOp === AluOp.jump, true.B, Mux(aluOp === AluOp.notEq || aluOp === AluOp.slt || aluOp === AluOp.sltu, ~zero, zero))
}