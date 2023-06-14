package jzcore

import chisel3._
import chisel3.util._
import utils._


// 部分积生成器
sealed class PGenerator extends Module {
  val io = IO(new Bundle {
    val yAdd  = Input(Bool())
    val y     = Input(Bool())
    val ySub  = Input(Bool())
    val x     = Input(UInt(132.W))

    val p     = Output(UInt(132.W))
    val c     = Output(Bool())
  })
  
  val x = io.x ## false.B

  val selNegative = io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selPositive = ~io.yAdd & (io.y & ~io.ySub | ~io.y & io.ySub)
  val selDoubleNegative = io.yAdd & ~io.y & ~io.ySub
  val selDoublePositive = ~io.yAdd & io.y & io.ySub

  val p_tmp = VecInit(List.fill(132)(false.B))

  (0 to 131).map(i => (p_tmp(i) := ~(~(selNegative & ~x(i+1)) & ~(selDoubleNegative & ~x(i)) & ~(selPositive & x(i+1)) & ~(selDoublePositive & x(i)))))
  io.p := p_tmp.asUInt()
  io.c := selNegative | selDoubleNegative
} 

// booth2位乘法器
sealed class Booth extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(new MultiInput))
    val out     = Decoupled(new MultiOutput)
  })

  val inFire = io.in.valid & io.in.ready
  val outFire = io.out.valid & io.out.ready 

  val idle :: busy :: Nil = Enum(2)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle -> Mux(inFire && !io.flush, busy, idle),
    busy -> Mux(outFire || io.flush, idle, busy)
  ))

  val result = RegInit(0.U(132.W)) // 结果寄存器
  val multiplicand = RegInit(0.U(132.W)) // 被乘数
  val multiplier = RegInit(0.U(66.W)) // 乘数

  val pg = Module(new PGenerator)
  pg.io.yAdd := multiplier(2)
  pg.io.y    := multiplier(1)
  pg.io.ySub := multiplier(0)
  pg.io.x    := multiplicand

  when(state === idle && io.in.valid) {
    result := 0.U
    multiplicand := Mux(io.in.mulSigned === MulType.uu, ZeroExt(io.in.multiplicand, 132), SignExt(io.in.multiplicand, 132))
    multiplier := Mux(io.in.mulSigned === MulType.ss, io.b(63) ## io.in.multiplier ## false.B, false.B ## io.in.multiplier ## false.B)
  }.elsewhen(state === busy && !io.out.valid) {
    result := pg.io.p + result + pg.io.c
    multiplicand := multiplicand << 2.U
    multiplier := multiplier >> 2.U
  }.otherwise {
    result := result
    multiplicand := multiplicand
    multiplier := multiplier
  }

  io.in.ready := state === idle
  io.out.valid := state === busy & !multiplier.orR
  io.out.valid  := !multiplier.orR && state === busy
  io.out.resultLo := Mux(io.in.mulw, SignExt(result(31, 0), 64), result(63, 0))
  io.out.resultHi := result(127, 64)
}


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

  val mul = Module(new Booth) // todo: low_power->booth, hign-performance->wallance

  val aluOp = io.aluOp
  // 是否为乘法操作
  val mulOp                = aluOp === AluOp.mul || aluOp === AluOp.mulh || aluOp === AluOp.mulw || aluOp === AluOp.mulhsu || aluOp === AluOp.mulhu
  
  mul.io.in.valid               := mulOp
  mul.io.in.bits.multiplicand   := io.opA
  mul.io.in.bits.multiplier     := io.opB
  mul.io.in.bits.mulw           := AluOp === mulw
  mul.io.in.bits.mulSigned      := LookupTreeDefault(aluOp, MulType.ss, List(
                                    AluOp.mulhu   -> MulType.uu,
                                    AluOp.mulhsu  -> MulType.su
                                  ))
  mul.io.out.ready              := !io.stall
  mul.io.flush                  := io.flush

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
    AluOp.mul       -> mul.io.out.bits.resultLo,
    AluOp.mulw      -> mul.io.out.bits.resultLo,
    //AluOp.mulh      -> ((SignExt(opA, 128).asSInt() * SignExt(opB, 128).asSInt()).asSInt() >> 64.U)(63, 0).asUInt(), // todo
    AluOp.mulh      -> mul.io.out.bits.resultHi,
    AluOp.mulhsu    -> mul.io.out.bits.resultHi,
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
    //AluOp.mulw      -> (opAw * opBw),
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
  // isWop 不会包含mulw的情况，mulw由乘法器的输出决定
  val isWop = aluOp === AluOp.addw || aluOp === AluOp.subw || aluOp === AluOp.divw || aluOp === AluOp.sllw || aluOp === AluOp.srlw || aluOp === AluOp.sraw || aluOp === AluOp.remw || aluOp === AluOp.divuw || aluOp === AluOp.remuw
  io.aluOut := Mux(isWop, aluOutw, aluOut)
  io.brMark := Mux(aluOp === AluOp.jump, true.B, Mux(aluOp === AluOp.beq || aluOp === AluOp.bne || aluOp === AluOp.blt || aluOp === AluOp.bltu || aluOp === AluOp.bge || aluOp === AluOp.bgeu, isOne, false.B))
  io.ready  := Mux(mulOp, mul.io.out.valid, true.B) // todo
}