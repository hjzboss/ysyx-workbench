import chisel3._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import chiseltest.formal._
import chiseltest.formal.BoundedCheck
import utest._
import utils._
import jzcore._

/*
class BoothTest1 extends Module {
  val io = IO(new Bundle {
    //val sign = Bool()
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val c = Output(UInt(128.W))
  })

  val b1 = io.b(63) ## io.b ## false.B

  val p = RegInit(0.S(128.W)) // 部分积

  val s = (io.a ## 0.U(64.W)).asSInt()
  val sDouble = (io.a ## 0.U(65.W)).asSInt() // 2s
  val sMinus = (~io.a + 1.U).asSInt() // -s
  val sMinusDouble = (~(io.a ## 0.U(65.W)) + 1.U).asSInt() // -2s

  val i = RegInit(1.U(7.W)) // 计数器，进行计数
  i := i + 2.U
  when(i === 1.U) {
    printf("s=%x, sDouble=%x, sMinus=%x, sMinusDouble=%x\n", s, sDouble, sMinus, sMinusDouble)
  }

  val select = b1(i+1.U) ## b1(i) ## b1(i - 1.U)

  val p_tmp = LookupTree(select, List(
    0.U   -> p,
    1.U   -> (p + s),
    2.U   -> (p + s),
    3.U   -> (p + sDouble),
    4.U   -> (p + sMinusDouble),
    5.U   -> (p + sMinus),
    6.U   -> (p + sMinus),
    7.U   -> p
  )).asSInt()

  p := p_tmp >> 2.U
  io.c := p.asUInt()

  printf("p_tmp=%x, select=%d\n", p_tmp, select)

  when(i === 65.U(7.W)) {
    val ref = io.a * io.b
    printf("io.c=%x, ref=%x\n", io.c, ref)
    chisel3.assert(io.c === ref)
  }
}*/

// 部分积生成器
class PGenerator extends Module {
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

// booth radix4乘法器
class BoothTest2 extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val a = Input(UInt(64.W))
    val b = Input(UInt(64.W))
    val c = Output(UInt(128.W))
    val ready = Output(Bool())
  })

  val idle :: busy :: Nil = Enum(2)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle -> Mux(io.valid, busy, idle),
    busy -> Mux(io.ready, idle, busy)
  ))

  val result = RegInit(0.U(132.W)) // 结果寄存器
  val multiplicand = RegInit(0.U(132.W)) // 被乘数
  val multiplier = RegInit(0.U(66.W)) // 乘数

  val pg = Module(new PGenerator)
  pg.io.yAdd := multiplier(2)
  pg.io.y    := multiplier(1)
  pg.io.ySub := multiplier(0)
  pg.io.x    := multiplicand

  when(state === idle && io.valid) {
    result := 0.U
    multiplicand := SignExt(io.a, 132) // 当被乘数是有符号数时进行符号扩展，否则进行0扩展
    multiplier := io.b(63) ## io.b ## false.B // 当为有符号数时，扩展符号位，否则扩展0
  }.elsewhen(state === busy && !io.ready) {
    result := pg.io.p + result + pg.io.c
    multiplicand := multiplicand << 2.U
    multiplier := multiplier >> 2.U
  }.otherwise {
    result := result
    multiplicand := multiplicand
    multiplier := multiplier
  }

  printf("result=%x, multiplier=%x, p=%x, c=%d\n", result, multiplier, pg.io.p, pg.io.c)
  io.ready  := !multiplier.orR && state === busy
  io.c      := result(127, 0)
  when(io.ready) {
    val ref = io.a * io.b
    chisel3.assert(io.c === ref)
    printf("io.c=%x, ref=%x\n", io.c, ref)
  }
}

// 华莱士树，第一个参数为位宽，第二个参数为流水线寄存器的插入位置
class Wallace(len: Int, doReg: Seq[Int]) extends Module {
  val io = IO(new Bundle() {
    val a, b = Input(UInt(len.W))
    val regEnables = Input(Vec(doReg.size, Bool()))
    val result = Output(UInt((2 * len).W))
  })
  val (a, b) = (io.a, io.b)
  val doRegSorted = doReg.sortWith(_ < _)

  val b_sext, bx2, neg_b, neg_bx2 = Wire(UInt((len+1).W))
  b_sext := SignExt(b, len+1)
  bx2 := b_sext << 1
  neg_b := (~b_sext).asUInt()
  neg_bx2 := neg_b << 1

  val columns: Array[Seq[Bool]] = Array.fill(2*len)(Seq())

  // 生成华莱士树
  var last_x = WireInit(0.U(3.W))
  for(i <- Range(0, len, 2)){
    // 生成部分积
    val x = if(i==0) Cat(a(1,0), 0.U(1.W)) else if(i+1==len) SignExt(a(i, i-1), 3) else a(i+1, i-1)
    val pp_temp = MuxLookup(x, 0.U, Seq(
      1.U -> b_sext,
      2.U -> b_sext,
      3.U -> bx2,
      4.U -> neg_bx2,
      5.U -> neg_b,
      6.U -> neg_b
    ))

    // 部分积符号扩展，todo：当为无符号数时进行无符号扩展
    val s = pp_temp(len)
    val t = MuxLookup(last_x, 0.U(2.W), Seq(
      4.U -> 2.U(2.W),
      5.U -> 1.U(2.W),
      6.U -> 1.U(2.W)
    ))
    last_x = x
    val (pp, weight) = i match {
      case 0 =>
        (Cat(~s, s, s, pp_temp), 0)
      case n if (n==len-1) || (n==len-2) =>
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
  def addAll(cols: Array[Seq[Bool]], depth: Int): (UInt, UInt) = {
    if(max(cols.map(_.size)) <= 2) {
      printf("depth=%d, over\n", depth.U)
      val sum = Cat(cols.map(_(0)).reverse)
      var k = 0
      while(cols(k).size == 1) k = k+1
      val carry = Cat(cols.drop(k).map(_(1)).reverse)
      (sum, Cat(carry, 0.U(k.W)))
    } else {
      val columns_next = Array.fill(2*len)(Seq[Bool]())
      var cout1, cout2 = Seq[Bool]()
      for(i <- cols.indices){
        val (s, c1, c2) = addOneColumn(cols(i), cout1)
        columns_next(i) = s ++ cout2
        cout1 = c1
        cout2 = c2
      }
      val needReg = doRegSorted.contains(depth)
      val toNextLayer = if(needReg)
        columns_next.map(_.map(x => RegEnable(x, io.regEnables(doRegSorted.indexOf(depth)))))
      else
        columns_next

      addAll(toNextLayer, depth+1)
    }
  }

  val (sum, carry) = addAll(cols = columns, depth = 0)
  io.result := sum + carry

  val cnt = RegInit(0.U(3.W))
  cnt := cnt + 1.U
  printf("io.result=%x, i=%x\n", io.result, cnt)

  when(cnt === 5.U) {
    val ref = io.a * io.b
    chisel3.assert(io.result === ref)
  }
}

// booth除法，适用于定点数
class Divider(len: Int) extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(new DivInput))
    val out     = Decoupled(new DivOutput)
  })

  val done = WireDefault(false.B) // 计算完成

  val inFire = io.in.valid & io.in.ready
  val outFire = io.out.valid & io.out.ready

  val idle :: compute :: check :: ok :: Nil = Enum(4)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
      idle    -> Mux(inFire && !io.flush, compute, idle),
      compute -> Mux(io.flush, idle, Mux(done, check, compute)),
      check   -> Mux(io.flush, idle, ok), // 结果校验阶段
      ok      -> Mux(outFire || io.flush, idle, ok)
    )
  )

  val cnt = RegInit(0.U(7.W)) // 除法计数器
  cnt := Mux(io.flush || state =/= compute, 0.U(7.W), cnt + 1.U)
  done := cnt === (len-1).U(7.W)

  // 双写符号位
  val dividend  = SignExt(io.in.bits.dividend, len+1)
  val divisor   = SignExt(io.in.bits.divisor, len+1)
  val divisorN  = ~divisor + 1.U((len+1).W)

  val quotient  = RegInit(0.U(len.W))
  val remainder = RegInit(0.U((len+1).W))

  val signre    = remainder(len, len-1)
  val signdi    = divisor(len, len-1)
  val zero      = !remainder.orR 
  val quoTmp    = WireDefault(0.U(len.W))

  when(state === idle && inFire) {
    quotient  := 0.U(len.W)
    remainder := dividend
  }.elsewhen(state === compute) {
    when(signre === signdi) {
      quoTmp := quotient(len-2, 0) ## 1.U(1.W)
      when(cnt === 7.U(7.W)) {
        quotient  := ~quoTmp(len-1) ## quoTmp(len-2, 0)  // 符号位取反
        remainder := remainder
      }.otherwise {
        quotient  := quoTmp
        remainder := (remainder(len-1, 0) ## 0.U(1.W)) + divisorN
      }
    }.otherwise {
      quoTmp := quotient(len-2, 0) ## 0.U(1.W)
      when(cnt === 7.U(7.W)) {
        quotient  := ~quoTmp(len-1) ## quoTmp(len-2, 0)  // 符号位取反
        remainder := remainder
      }.otherwise {
        quotient  := quoTmp
        remainder := (remainder(len-1, 0) ## 0.U(1.W)) + divisor
      }
    }
  }.elsewhen(state === check) {
    quotient := Mux((zero && signdi === 3.U(2.W)) || (!zero && quotient(len-1)), quotient + 1.U, quotient) // 修正商
    remainder := Mux(zero && remainder(len, len-1) === signdi, remainder + divisorN, Mux(!zero && remainder(len, len-1) =/= signdi, remainder + divisor, remainder)) // 修正余数
  }.otherwise {
    quotient := quotient
    remainder := remainder
  }

  io.in.ready := state === idle
  io.out.valid := state === ok
  io.out.bits.quotient := quotient
  io.out.bits.remainder := remainder

  printf("quotient=%x, remainder=%x\n", quotient, remainder)
  when(state === ok) {
    val ref = io.in.bits.dividend / io.in.bits.divisor
    val rem = io.in.bits.dividend % io.in.bits.divisor
    printf("quotient: dut=%x, ref=%x\n", quotient, ref)
    printf("remainder: dut=%x, ref=%x\n", remainder, rem)
    chisel3.assert(io.out.bits.quotient === ref)
    chisel3.assert(io.out.bits.remainder === rem)
  }
}

// 恢复余数法
class Div(len: Int) extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(new DivInput))
    val out     = Decoupled(new DivOutput)
  })

  // 取反函数
  def getN(num: UInt): UInt = ~num + 1.U

  val done = WireDefault(false.B) // 除法结束标志
  val inFire = io.in.valid & io.in.ready
  val outFire = io.out.valid & io.out.ready

  val idle :: compute :: recover :: ok :: Nil = Enum(4)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle    -> Mux(inFire && !io.flush, compute, idle),
    compute -> Mux(io.flush, idle, Mux(done, recover, compute)),
    recover -> Mux(io.flush, idle, ok),
    ok      -> Mux(io.flush || outFire, idle, ok)
  ))

  val neg       = Wire(Bool())
  val dividend  = RegInit(0.U((2*len).W))
  val divisor   = RegInit(0.U(len.W))
  val quotient  = RegInit(0.U(len.W))
  val remainder = RegInit(0.U(len.W))
  val cnt       = RegInit(0.U(8.W))
  cnt          := Mux(state === compute, cnt + 1.U, 0.U)

  io.in.ready := state === idle
  io.out.valid := state === ok

  when(io.in.bits.divw) {
    val tmp     = WireDefault(0.U((len/2+1).W)) // 中间相减的结果
    done       := cnt === (len/2-1).U
    neg        := io.in.bits.dividend(len/2-1) ^ io.in.bits.divisor(len/2-1)

    divisor    := Mux(state === idle && inFire, Mux(io.in.bits.divisor(len/2-1) && io.in.bits.divSigned, getN(io.in.bits.divisor), io.in.bits.divisor), divisor)

    when(state === idle && inFire) {
      quotient  := 0.U(len.W)
      // 取绝对值
      dividend := Mux(io.in.bits.dividend(len/2-1) && io.in.bits.divSigned, ZeroExt(getN(io.in.bits.dividend(len/2-1, 0)), 2*len), ZeroExt(io.in.bits.dividend(len/2-1, 0), 2*len))
    }.elsewhen(state === compute) {
      tmp := dividend(len-1, len/2-1) - (0.U(1.W) ## divisor(len/2-1, 0))
      when(tmp(len/2)) {
        // 为负数被除数保持不变, 商0
        dividend := dividend ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 0.U(1.W)
      }.otherwise {
        val remTmp = tmp ## dividend(len/2-2, 0)
        dividend := remTmp(len-2, 0) ## 0.U(1.W) // 左移一位
        quotient  := quotient(len-2, 0) ## 1.U(1.W)
      }
    }.elsewhen(state === recover) {
      // 修正
      when(neg) {
        quotient := Mux(quotient(len/2-1), quotient, getN(quotient))
      }.otherwise {
        quotient := Mux(quotient(len/2-1), getN(quotient), quotient)
      }
    }.otherwise {
      dividend := dividend
      quotient := quotient
    }

    // 余数
    when(state === idle) {
      remainder := 0.U(len.W)
    }.elsewhen(state === recover) {
      remainder := Mux(dividend(len-1) ^ io.in.bits.dividend(len/2-1), getN(dividend(len-1, len/2)), dividend(len-1, len/2))
    }.otherwise {
      remainder := remainder
    }

    io.out.bits.quotient := SignExt(quotient(len/2-1, 0), len)
    io.out.bits.remainder := SignExt(remainder(len/2-1, 0), len)

    when(state === ok) {
      //val ref = io.in.bits.dividend / io.in.bits.divisor
      //val rem = io.in.bits.dividend % io.in.bits.divisor
      val ref = "h0000000000375f00".U(64.W)
      val rem = 0.U(64.W)
      printf("quotient: dut=%x, ref=%x\n", quotient, ref)
      printf("remainder: dut=%x, ref=%x\n", remainder, rem)
      chisel3.assert(io.out.bits.quotient === ref)
      chisel3.assert(io.out.bits.remainder === rem)
    }
  }.otherwise {
    val tmp     = WireDefault(0.U((len+1).W)) // 中间相减的结果
    done       := cnt === (len-1).U
    neg        := io.in.bits.dividend(len-1) ^ io.in.bits.divisor(len-1)

    divisor    := Mux(state === idle && inFire, Mux(io.in.bits.divisor(len-1) && io.in.bits.divSigned, getN(io.in.bits.divisor), io.in.bits.divisor), divisor)

    when(state === idle && inFire) {
      quotient  := 0.U(len.W)
      // 取绝对值
      dividend := Mux(io.in.bits.dividend(len-1) && io.in.bits.divSigned, 0.U(len.W) ## getN(io.in.bits.dividend), 0.U(len.W) ## io.in.bits.dividend)
    }.elsewhen(state === compute) {
      tmp := dividend(2*len-1, len-1) - (0.U(1.W) ## divisor)
      when(tmp(len)) {
        // 为负数被除数保持不变, 商0
        dividend := dividend ## 0.U(1.W)
        quotient  := quotient(len-2, 0) ## 0.U(1.W)
      }.otherwise {
        val remTmp = tmp ## dividend(len-2, 0)
        dividend := remTmp(2*len-2, 0) ## 0.U(1.W) // 左移一位
        quotient  := quotient(len-2, 0) ## 1.U(1.W)
      }
    }.elsewhen(state === recover) {
      // 修正
      when(neg) {
        quotient := Mux(quotient(len-1), quotient, getN(quotient))
      }.otherwise {
        quotient := Mux(quotient(len-1), getN(quotient), quotient)
      }
    }.otherwise {
      dividend := dividend
      quotient := quotient
    }

    // 余数
    when(state === idle) {
      remainder := 0.U(len.W)
    }.elsewhen(state === recover) {
      remainder := Mux(dividend(2*len-1) ^ io.in.bits.dividend(len-1), getN(dividend(2*len-1, len)), dividend(2*len-1, len))
    }.otherwise {
      remainder := remainder
    }

    io.out.bits.quotient := quotient
    io.out.bits.remainder := remainder

    when(state === ok) {
      val ref = io.in.bits.dividend / io.in.bits.divisor
      val rem = io.in.bits.dividend % io.in.bits.divisor
      //val ref = ~1.U(64.W)
      //val rem = ~0.U(64.W)
      printf("quotient: dut=%x, ref=%x\n", quotient, ref)
      printf("remainder: dut=%x, ref=%x\n", remainder, rem)
      chisel3.assert(io.out.bits.quotient === ref)
      chisel3.assert(io.out.bits.remainder === rem)
    }
  }

  printf("quotient=%x, dividend=%x\n", quotient, dividend)
}

// 一致性写回的多路选择器（仲裁）
class CohArbiter(len: Int) extends Module {
  val io = IO(new Bundle {
    val cenIn   = Input(Vec(len, Bool())) // valid & dirty
    val noIn    = Input(Vec(len, UInt(2.W)))
    val indexIn = Input(Vec(len, UInt(6.W)))
    val tagIn   = Input(Vec(len, UInt(22.W)))

    val noOut   = Output(UInt(2.W))
    val indexOut= Output(UInt(6.W))
    val tagOut  = Output(UInt(22.W))
  })

  var flag: Boolean = false

  for (i <- 0 until len) {
    if(!flag) {
      when(io.cenIn(i) === true.B) {
        io.noOut := io.noIn(i)
        io.indexOut := io.indexIn(i)
        io.tagOut := io.tagIn(i)
        flag = true
      }
    }
  }
}

object JzCoreSpec extends ChiselUtestTester {
  val tests = Tests {
    test("mul") {
      testCircuit(new Div(64)) {
        dut =>
          /*
          dut.io.a.poke("h0".U(64.W))
          dut.io.b.poke("hffffffffffffffff".U(64.W))
          var flag = true
          dut.io.valid.poke(true.B)
          while(flag) {
            dut.clock.step()
            if(dut.io.ready.peek.litToBoolean) {
              flag = false
            }
          }
          dut.clock.step()
          */
          dut.io.in.valid.poke(true.B)
          dut.io.in.bits.dividend.poke(123456123.U(64.W))
          //dut.io.in.bits.dividend.poke("h0000_0000_0000_0100".U(64.W))
          dut.io.in.bits.divisor.poke(1231234.U(64.W))
          dut.io.in.bits.divSigned.poke(true.B)
          dut.io.in.bits.divw.poke(false.B)
          dut.io.out.ready.poke(false.B)
          while(true) {
            dut.clock.step()
          }
          //dut.io.result.expect(72.U)
      }
    }
  }
}