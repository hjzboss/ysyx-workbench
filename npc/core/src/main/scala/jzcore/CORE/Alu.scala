package jzcore

import chisel3._
import chisel3.util._
import utils._


class Alu extends Module {
  val io = IO(new Bundle {
    val stall   = Input(Bool())

    val opA     = Input(UInt(64.W))
    val opB     = Input(UInt(64.W))
    val aluOp   = Input(AluOp())

    val aluOut  = Output(UInt(64.W))

    val ready   = Output(Bool()) // alu操作是否完成，主要作用于乘除法
  })

  val mul = Module(new Mul)
  val div = Module(new Divider)
 
  val aluOp = io.aluOp
  val isWop = AluOp.isWordOp(aluOp)

  val mulOp                      = AluOp.mulOp(aluOp)
  mul.io.in.valid               := mulOp
  mul.io.in.multiplicand        := io.opA
  mul.io.in.multiplier          := io.opB
  mul.io.in.mulw                := isWop
  mul.io.in.mulSigned           := LookupTreeDefault(aluOp, MulType.ss, List(
                                    AluOp.mulhu   -> MulType.uu,
                                    AluOp.mulhsu  -> MulType.su
                                  ))
  mul.io.out.ready              := !io.stall

  val divOp                      = AluOp.divOp(aluOp)
  div.io.in.valid               := divOp
  div.io.in.dividend            := io.opA
  div.io.in.divisor             := io.opB
  div.io.in.divw                := isWop
  div.io.in.divSigned           := AluOp.divSigned(aluOp)
  div.io.out.ready              := !io.stall

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
    AluOp.and       -> (opA & opB),
    AluOp.or        -> (opA | opB),
    AluOp.xor       -> (opA ^ opB),
    AluOp.slt       -> Mux(opA.asSInt() < opB.asSInt(), 1.U(64.W), 0.U(64.W)),
    AluOp.sltu      -> Mux(opA < opB, 1.U(64.W), 0.U(64.W)),
    AluOp.sll       -> (opA << opB(5, 0))(63, 0),
    AluOp.srl       -> (opA >> opB(5, 0))(63, 0),
    AluOp.sra       -> (opA.asSInt() >> opB(5, 0))(63, 0).asUInt(),
    AluOp.sllw      -> (opA << opB(4, 0))(63, 0),
    AluOp.srlw      -> (ZeroExt(opA(31, 0), 64) >> opB(4, 0))(63, 0),
    AluOp.sraw      -> (SignExt(opA(31, 0), 64) >> opB(4, 0))(63, 0),
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
  io.aluOut := Mux(isWop, aluOutw, aluOut)
  io.ready  := Mux(mulOp, mulFire, Mux(divOp, divFire, true.B))
}