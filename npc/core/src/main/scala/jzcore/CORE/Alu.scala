package jzcore

import chisel3._
import chisel3.util._
import utils._
import top._


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

  when(state === idle && inFire) {
    result := 0.U
    multiplicand := Mux(io.in.bits.mulSigned === MulType.uu, ZeroExt(io.in.bits.multiplicand, 132), SignExt(io.in.bits.multiplicand, 132))
    multiplier := Mux(io.in.bits.mulSigned === MulType.ss, io.in.bits.multiplier(63) ## io.in.bits.multiplier ## false.B, false.B ## io.in.bits.multiplier ## false.B)
  }.elsewhen(state === busy && !io.out.valid) {
    result := pg.io.p + result + pg.io.c
    multiplicand := multiplicand << 2.U
    multiplier := multiplier >> 2.U
  }.otherwise {
    result := result
    multiplicand := multiplicand
    multiplier := multiplier
  }

  io.in.ready          := state === idle
  io.out.valid         := state === busy & !multiplier.orR
  io.out.bits.resultLo := Mux(io.in.bits.mulw, SignExt(result(31, 0), 64), result(63, 0))
  io.out.bits.resultHi := result(127, 64)
}

// 华莱士树，第一个参数为位宽，第二个参数为流水线寄存器的插入位置
// todo: 无符号数的处理
sealed class Wallace() extends Module {
  val io = IO(new Bundle() {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(new MultiInput))
    val out     = Decoupled(new MultiOutput)
  })

  // 压缩一列
  def addOneColumn(col: Seq[Bool], cin: Seq[Bool]): (Seq[Bool], Seq[Bool], Seq[Bool]) = {
    var sum = Seq[Bool]()
    var cout1 = Seq[Bool]()
    var cout2 = Seq[Bool]()
    col.size match {
      case 1 =>  // do nothing
        sum = col ++ cin
      case 2 =>
        val c22 = Module(new C22)
        c22.io.in := col
        sum = c22.io.out(0).asBool() +: cin
        cout2 = Seq(c22.io.out(1).asBool())
      case 3 =>
        val c32 = Module(new C32)
        c32.io.in := col
        sum = c32.io.out(0).asBool() +: cin
        cout2 = Seq(c32.io.out(1).asBool())
      case 4 =>
        val c53 = Module(new C53)
        for((x, y) <- c53.io.in.take(4) zip col){
          x := y
        }
        c53.io.in.last := (if(cin.nonEmpty) cin.head else 0.U)
        sum = Seq(c53.io.out(0).asBool()) ++ (if(cin.nonEmpty) cin.drop(1) else Nil)
        cout1 = Seq(c53.io.out(1).asBool())
        cout2 = Seq(c53.io.out(2).asBool())
      case n =>
        val cin_1 = if(cin.nonEmpty) Seq(cin.head) else Nil
        val cin_2 = if(cin.nonEmpty) cin.drop(1) else Nil
        val (s_1, c_1_1, c_1_2) = addOneColumn(col take 4, cin_1)
        val (s_2, c_2_1, c_2_2) = addOneColumn(col drop 4, cin_2)
        sum = s_1 ++ s_2
        cout1 = c_1_1 ++ c_2_1
        cout2 = c_1_2 ++ c_2_2
    }
    (sum, cout1, cout2)
  }

  // 返回集合的最大值
  def max(in: Iterable[Int]): Int = in.reduce((a, b) => if(a>b) a else b)

  // 华莱士树压缩算法
  def addAll(cols: Array[Seq[Bool]], depth: Int, valid: Bool, flush: Bool): (UInt, UInt, Bool) = {
    if(max(cols.map(_.size)) <= 2) {
      val sum = Cat(cols.map(_(0)).reverse)
      var k = 0
      while(cols(k).size == 1) k = k+1
      val carry = Cat(cols.drop(k).map(_(1)).reverse)
      (sum, Cat(carry, 0.U(k.W)), valid)
    } else {
      val columns_next = Array.fill(128)(Seq[Bool]())
      var cout1, cout2 = Seq[Bool]()
      for(i <- cols.indices){
        val (s, c1, c2) = addOneColumn(cols(i), cout1)
        columns_next(i) = s ++ cout2
        cout1 = c1
        cout2 = c2
      }

      // 有效信号传递
      val toNextValid = RegInit(false.B)
      toNextValid := Mux(flush, false.B, valid)

      val toNextLayer = columns_next.map(_.map(x => RegEnable(x, regEnables(depth))))
      addAll(toNextLayer, depth+1, valid, flush)
    }
  }

  val validTmp = WireDefault(false.B) // 有效乘法数据的信号

  // 握手信号
  val inFire = io.in.valid & io.in.ready
  val outFire = io.out.valid & io.out.ready 

  val idle :: start :: busy :: ok :: Nil = Enum(4)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle  -> Mux(inFire && !io.flush, start, idle),
    start -> Mux(io.flush, idle, busy),
    busy  -> Mux(io.flush, idle, Mux(validTmp, ok, busy)),
    ok    -> Mux(outFire || io.flush, idle, ok)
  ))

  val (a, b) = (io.in.bits.multiplicand, io.in.bits.multiplier)

  val columns: Array[Seq[Bool]] = Array.fill(128)(Seq()) // todo: 此处是组合逻辑
  var last_x = WireInit(0.U(3.W))
  // 生成华莱士树
  val b_sext, bx2, neg_b, neg_bx2 = Wire(UInt(65.W))
  b_sext := SignExt(b, 65)
  bx2 := b_sext << 1
  neg_b := (~b_sext).asUInt()
  neg_bx2 := neg_b << 1

  for(i <- Range(0, 64, 2)){
    // 生成部分积
    val x = if(i==0) Cat(a(1,0), 0.U(1.W)) else if(i==63) SignExt(a(i, i-1), 3) else a(i+1, i-1)
    val pp_temp = MuxLookup(x, 0.U, Seq(
      1.U -> b_sext,
      2.U -> b_sext,
      3.U -> bx2,
      4.U -> neg_bx2,
      5.U -> neg_b,
      6.U -> neg_b
    ))

    // 部分积符号扩展，todo：当为无符号数时进行无符号扩展
    val s = pp_temp(64)
    val t = MuxLookup(last_x, 0.U(2.W), Seq(
      4.U -> 2.U(2.W),
      5.U -> 1.U(2.W),
      6.U -> 1.U(2.W)
    ))
    last_x = x
    val (pp, weight) = i match {
      case 0 =>
        (Cat(~s, s, s, pp_temp), 0)
      case n if (n==63) || (n==62) =>
        (Cat(~s, pp_temp, t), i-2)
      case _ =>
        (Cat(1.U(1.W), ~s, pp_temp, t), i-2)
    }
    for(j <- columns.indices){
      if(j >= weight && j < (weight + pp.getWidth)){
        columns(j) = columns(j) :+ pp(j-weight)
      }
    }
  }

  val regEnables = Wire(Vec(5, Bool())) // 寄存器使能信号
  (0 to 4).map(i => (regEnables(i) := state === busy | state === start))

  val validIn = state === start
  // todo: 此处为每个时钟周期都调用addAll
  val (sum, carry, validOut) = addAll(cols = columns, depth = 0, valid = validIn, flush=io.flush)
  validTmp := validOut
  val result = sum + carry

  // 锁存有效信号和结果
  //val validReg = RegInit(false.B)
  //validReg := Mux(io.flush, false.B, Mux(state === busy, validOut, validReg))
  val resultReg = RegInit(0.U(128.W))
  resultReg := Mux(io.flush, 0.U(128.W), Mux(state === busy && validTmp, result, resultReg))

  io.out.bits.resultLo := Mux(io.in.bits.mulw, SignExt(resultReg(31, 0), 64), resultReg(63, 0))
  io.out.bits.resultHi := resultReg(127, 64)
  io.out.valid := state === ok
  io.in.ready := state === idle
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

  val mul = if(Settings.get("lowpower")) Module(new Booth) else Module(new Wallace)

  val aluOp = io.aluOp
  // 是否为乘法操作
  val mulOp                = aluOp === AluOp.mul || aluOp === AluOp.mulh || aluOp === AluOp.mulw || aluOp === AluOp.mulhsu || aluOp === AluOp.mulhu
  
  mul.io.in.valid               := mulOp
  mul.io.in.bits.multiplicand   := io.opA
  mul.io.in.bits.multiplier     := io.opB
  mul.io.in.bits.mulw           := aluOp === AluOp.mulw
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