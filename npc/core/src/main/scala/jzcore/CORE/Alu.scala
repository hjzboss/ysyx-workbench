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

  val aluOp = io.aluOp

  // xlen computation
  val opA = io.opA
  val opB = io.opB
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
    AluOp.div       -> (opA.asSInt() / opB.asSInt()).asUInt(),
    AluOp.divu      -> (opA / opB),
    AluOp.mul       -> (opA * opB),
    AluOp.mulh      -> ((SignExt(opA, 128).asSInt() * SignExt(opB, 128).asSInt()).asSInt() >> 64.U)(63, 0).asUInt(), // todo
    AluOp.rem       -> (opA.asSInt() % opB.asSInt()).asUInt(),
    AluOp.remu      -> (opA % opB),
    AluOp.csrrw     -> opB,
    AluOp.csrrs     -> (opA | opB),
    AluOp.csrrc     -> (opA & ~opB)
  ))

  // word computation
  val opAw = opA(31, 0)
  val opBw = opB(31, 0)
  val aluW = LookupTree(io.aluOp, List(
    AluOp.addw      -> (opAw + opBw),
    AluOp.subw      -> (opAw.asSInt() - opBw.asSInt()).asUInt(),
    AluOp.mulw      -> (opAw * opBw),
    AluOp.divw      -> (opAw.asSInt() / opBw.asSInt()).asUInt(), // todo
    AluOp.divuw     -> (opAw / opBw),
    AluOp.sllw      -> (opAw << opBw(4, 0)),
    AluOp.srlw      -> (opAw >> opBw(4, 0)),
    AluOp.sraw      -> (opAw.asSInt() >> opBw(4, 0)).asUInt(),
    AluOp.remw      -> (opAw.asSInt() % opBw.asSInt()).asUInt(),
    AluOp.remuw     -> (opAw % opBw),
  ))

  val aluOutw = SignExt(aluW(31, 0), 64)
  val isOne = aluOut.asUInt() === 1.U(64.W)
  val isWop = aluOp === AluOp.addw || aluOp === AluOp.subw || aluOp === AluOp.mulw || aluOp === AluOp.divw || aluOp === AluOp.sllw || aluOp === AluOp.srlw || aluOp === AluOp.sraw || aluOp === AluOp.remw || aluOp === AluOp.divuw || aluOp === AluOp.remuw
  io.aluOut := Mux(isWop, aluOutw, aluOut)
  io.brMark := Mux(aluOp === AluOp.jump, true.B, Mux(aluOp === AluOp.beq || aluOp === AluOp.bne || aluOp === AluOp.blt || aluOp === AluOp.bltu || aluOp === AluOp.bge || aluOp === AluOp.bgeu, isOne, false.B))
}