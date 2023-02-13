package jzcore

import chisel3._
import chisel3.util._
import utils._

class Alu extends Module {
  val io = IO(new Bundle {
    val opA     = Input(UInt(64.W))
    val opB     = Input(UInt(64.W))
    val aluOp   = Input(UInt(6.W))
    
    val aluOut  = Output(UInt(64.W))
    val brMark  = Output(Bool())
  })

  val opA = io.opA
  val opB = io.opB
  val aluOp = io.aluOp

  val aluOut = LookupTree(io.aluOp, List(
    AluOp.add       -> (opA + opB),
    AluOp.jump      -> (opA + opB),
    AluOp.sub       -> (opA - opB),
    AluOp.beq       -> Mux(opA === opB, 1.U(64.W), 0.U(64.W)),
    AluOp.bne       -> Mux(opA =/= opB, 1.U(64.W), 0.U(64.W)),
    AluOp.and       -> (opA & opB),
    AluOp.or        -> (opA | opB),
    AluOp.xor       -> (opA ^ opB),
    AluOp.slt       -> Mux(opA.asSInt() < opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    AluOp.blt       -> Mux(opA.asSInt() < opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    AluOp.bge       -> Mux(opA.asSInt() >= opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    AluOp.bgeu      -> Mux(opA >= opB, 1.U(64.W), 0.U(64.W)),
    AluOp.sltu      -> Mux(opA < opB, 1.U(64.W), 0.U(64.W)),
    AluOp.bltu      -> Mux(opA < opB, 1.U(64.W), 0.U(64.W)),
    AluOp.sll       -> (opA << opB(5, 0)),
    AluOp.srl       -> (opA >> opB(5, 0)),
    AluOp.sra       -> (opA.asSInt() >> opB(5, 0)).asUInt(),
    // todo
    AluOp.div       -> (opA / opB),
    AluOp.mul       -> (opA * opB),
    AluOp.rem       -> (opA % opB),

    AluOp.addw      -> (opA + opB),
    AluOp.subw      -> (opA - opB),
    AluOp.mulw      -> (opA * opB),
    AluOp.divw      -> (opA / opB),
    AluOp.sllw      -> (opA << opB(4, 0)),
    AluOp.srlw      -> (opA >> opB(4, 0)),
    AluOp.sraw      -> (opA.asSInt() >> opB(4, 0)).asUInt(),
    AluOp.remw      -> (opA % opB)
  ))

  val isOne = aluOp === 1.U(64.W)
  val isWop = aluOp === AluOp.addw || aluOp === AluOp.subw || aluOp === AluOp.mulw || aluOp === AluOp.divw || aluOp === AluOp.sllw || aluOp === AluOp.srlw || aluOp === AluOp.sraw || aluOp === AluOp.remw
  
  val aluOutw = SignExt(aluOut(31, 0), 64)
  io.aluOut := Mux(isWop, aluOutw, aluOut)
  io.brMark := Mux(aluOp === AluOp.jump, true.B, Mux(aluOp === AluOp.beq || aluOp === AluOp.bne || aluOp === AluOp.blt || aluOp === AluOp.bltu || aluOp === AluOp.bge || aluOp === AluOp.bgeu, isOne, false.B))
}