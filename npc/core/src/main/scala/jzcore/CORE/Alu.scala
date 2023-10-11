package jzcore

import chisel3._
import chisel3.util._
import utils._


class Alu extends Module {
  val io = IO(new Bundle {
    val stall   = Input(Bool())
    val flush   = Input(Bool())

    val opA     = Input(UInt(64.W))
    val opB     = Input(UInt(64.W))
    val aluOp   = Input(UInt(6.W))

    val aluOut  = Output(UInt(64.W))
    val brMark  = Output(Bool())

    val ready   = Output(Bool()) // alu操作是否完成，主要作用于乘除法
  })

  val mul = Module(new Mul)
  val div = Module(new Divider(64))
 
  val aluOp = io.aluOp
  val isWop = AluOp.isWordOp(aluOp)

  val mulOp                      = aluOp === AluOp.mul || aluOp === AluOp.mulh || aluOp === AluOp.mulw || aluOp === AluOp.mulhsu || aluOp === AluOp.mulhu
  mul.io.in.valid               := mulOp
  mul.io.in.multiplicand        := io.opA
  mul.io.in.multiplier          := io.opB
  mul.io.in.mulw                := isWop
  mul.io.in.mulSigned           := LookupTreeDefault(aluOp, MulType.ss, List(
                                    AluOp.mulhu   -> MulType.uu,
                                    AluOp.mulhsu  -> MulType.su
                                  ))
  mul.io.out.ready              := !io.stall
  mul.io.flush                  := io.flush

  val divOp                      = aluOp === AluOp.div || aluOp === AluOp.divu || aluOp === AluOp.divw || aluOp === AluOp.divuw || aluOp === AluOp.rem || aluOp === AluOp.remu || aluOp === AluOp.remuw || aluOp === AluOp.remw
  div.io.in.valid               := divOp
  div.io.in.dividend            := io.opA
  div.io.in.divisor             := io.opB
  div.io.in.divw                := isWop
  div.io.in.divSigned           := aluOp === AluOp.div || aluOp === AluOp.divw || aluOp === AluOp.rem || aluOp === AluOp.remw
  div.io.out.ready              := !io.stall
  div.io.flush                  := io.flush

  // xlen computation
  val opA = io.opA
  val opB = io.opB
  val aluOut = Wire(UInt(64.W))
  aluOut    := LookupTree(io.aluOp, List(
    AluOp.add       -> (opA + opB),
    AluOp.addw      -> (opA + opB),
    AluOp.jump      -> (opA + opB),
    AluOp.sub       -> (opA - opB),
    AluOp.subw      -> (opA - opB),
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
    AluOp.sllw      -> (opA << opB(4, 0)),
    AluOp.srlw      -> ZeroExt(opA(31, 0), 64) >> opB(4, 0),
    AluOp.sraw      -> SignExt(opA(31, 0), 64) >> opB(4, 0),
    AluOp.div       -> div.io.out.bits.quotient,
    AluOp.divu      -> div.io.out.bits.quotient,
    AluOp.divw      -> div.io.out.bits.quotient,
    AluOp.divuw     -> div.io.out.bits.quotient,
    AluOp.rem       -> div.io.out.bits.remainder,
    AluOp.remu      -> div.io.out.bits.remainder,
    AluOp.remw      -> div.io.out.bits.remainder,
    AluOp.remuw     -> div.io.out.bits.remainder,
    AluOp.mul       -> mul.io.out.bits.resultLo,
    AluOp.mulw      -> mul.io.out.bits.resultLo,
    AluOp.mulh      -> mul.io.out.bits.resultHi,
    AluOp.mulhsu    -> mul.io.out.bits.resultHi,
    AluOp.csrrw     -> opB,
    AluOp.csrrs     -> (opA | opB),
    AluOp.csrrc     -> (opA & ~opB)
  ))

  val mulFire = mul.io.out.valid && mul.io.out.ready
  val divFire = div.io.out.valid && div.io.out.ready

  val aluOutw = SignExt(aluOut(31, 0), 64)
  val isOne = aluOut.asUInt() === 1.U(64.W)
  io.aluOut := Mux(isWop, aluOutw, aluOut)
  io.brMark := Mux(aluOp === AluOp.jump, true.B, Mux(aluOp === AluOp.beq || aluOp === AluOp.bne || aluOp === AluOp.blt || aluOp === AluOp.bltu || aluOp === AluOp.bge || aluOp === AluOp.bgeu, isOne, false.B))
  io.ready  := Mux(mulOp, mulFire, Mux(divOp, divFire, true.B))
}