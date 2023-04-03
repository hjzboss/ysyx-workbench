package jzcore

import chisel3._
import top.Settings
import chisel3.util._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class IFU extends Module with HasResetVector{
  val io = IO(new Bundle {
    // 传给仿真环境
    val pc        = Output(UInt(64.W))
    val nextPc    = Output(UInt(64.W))
    val inst      = Output(UInt(32.W))

    // 来自exu
    val redirect  = Flipped(Decoupled(new RedirectIO))

    // 传给idu
    val out       = Decoupled(new InstrFetch)

    // axi总线接口：取指
    val addrIO    = Decoupled(new AddrIO)
    val dataIO    = Flipped(Decoupled(new RdataIO))
  })

  val fire = io.dataIO.valid && io.dataIO.ready

  // 取指状态机
  val start :: fetch :: f_wait :: Nil = Enum(3)
  val state = RegInit(start)
  state := MuxLookup(state, start, List(
    start         -> Mux(io.addrIO.ready, fetch, start), // 开始取指
    fetch         -> Mux(fire && io.out.ready, start, Mux(!fire, fetch, f_wait)), // 取指完成，当取指阻塞时保持状态
    f_wait        -> Mux(io.out.ready, start, f_wait) // idu阻塞，但当前的设计中idu不会阻塞
  ))

  // pc
  val pc  = RegInit(resetVector.U(64.W))
  val snpc = pc + 4.U

  // redirect
  val dnpc = io.redirect.bits.brAddr
  io.redirect.ready   := state === fetch
  val npc = Mux(io.redirect.valid, dnpc, snpc)

  pc                  := Mux(state === fetch, npc, pc)

  // 取指
  io.addrIO.bits.addr := pc
  io.addrIO.valid     := state === start // todo，什么时候有效？
  // 指令对齐
  val instPre = io.dataIO.bits.data
  val inst = Mux(pc(2) === 1.U(1.W), instPre(63, 32), instPre(31, 0))
  val instReg = Reg(UInt(32.W))
  instReg             := Mux(state === fetch, inst, instReg) // 取出指令后锁存指令，当idu未准备好时有用
  io.dataIO.ready     := true.B // todo，忽略了rresp

  // ifu -> idu
  io.out.valid        := state =/= start
  io.out.bits.pc      := pc
  io.out.bits.inst    := Mux(state === f_wait, instReg, inst)

  // 传给仿真环境
  io.nextPc           := npc
  io.pc               := pc
  io.inst             := io.out.bits.inst
}