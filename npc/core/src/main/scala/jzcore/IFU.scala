package jzcore

import chisel3._
import top.Settings
import chisel3.util._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class IFU extends Module with HasResetVector{
  val io = IO(new Bundle {
    // 用于仿真环境
    val debug       = new DebugIO

    // 来自exu
    val redirect    = Flipped(new RedirectIO)

    // 送给idu
    val out         = new InstrFetch

    // axi取指接口
    val axiAddrIO   = Decoupled(new RaddrIO)
    val axiDataIO   = Flipped(Decoupled(new RdataIO))

    // 控制模块
    val fetchReady  = Output(Bool()) // 取指完成
    val stall       = Input(Bool()) // 停顿信号

    // 来自lsu，临时的信号
    val lsuReady    = Input(Bool())
  })

  val dataFire = io.axiDataIO.valid && io.axiDataIO.ready
  val addrFire = io.axiAddrIO.ready && io.axiAddrIO.valid

  // 取指状态机
  val addr :: data :: Nil = Enum(2)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(addr)
  state := MuxLookup(state, addr, List(
    addr    -> Mux(addrFire, data, addr),
    data    -> Mux((dataFire && !io.stall) || (io.stall && io.lsuReady), addr, data) // 当lsu阶段执行完后才回到addr状态，开始取指,todo
  ))

  // pc
  val pc  = RegInit(resetVector.U(64.W))
  val snpc = pc + 4.U
  val dnpc = io.redirect.brAddr

  // 取指接口，todo：停顿信号也要发挥作用
  io.axiAddrIO.valid      := state === addr && !io.stall
  io.axiAddrIO.bits.addr  := pc
  io.axiDataIO.ready      := state === data
  // 数据选择
  val instPre              = io.axiDataIO.bits.rdata
  val inst                 = Mux(pc(2) === 0.U(1.W), instPre(31, 0), instPre(63, 32))

  // 取指成功后锁存指令
  val instReg              = RegInit(Instruction.NOP)
  instReg                 := Mux(state === data && dataFire, inst, instReg)

  // 更新pc值
  pc                      := MuxLookup(state, pc, List(
                              addr  -> pc,
                              // 如果rresp不是okay，则pc保持原值重新取指，todo，当lsu取指成功后再更新pc
                              data  -> Mux(!((dataFire && !io.stall) || (io.stall && io.lsuReady)), pc, Mux(io.redirect.valid, dnpc, snpc))
                            ))

  // 仿真环境
  io.debug.inst           := inst
  io.debug.nextPc         := Mux(io.redirect.valid, dnpc, snpc)
  io.debug.pc             := pc
  io.debug.execonce       := state === data && dataFire && (!io.stall || io.stall && io.lsuReady)

  io.out.pc               := pc
  io.out.inst             := inst

  io.fetchReady           := (state === data && dataFire) || (state === data && io.stall) 
}