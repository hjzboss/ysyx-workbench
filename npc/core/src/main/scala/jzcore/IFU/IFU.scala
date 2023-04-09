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

    // axi接口
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    

    // 仲裁信号
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())

    // 控制模块
    val ready       = Output(Bool()) // 取指完成
    //val stall       = Input(Bool()) // 停顿信号

    // 来自wbu，当前指令已执行完毕
    val pcEnable    = Input(Bool())
  })

  val dataFire = io.axiRdataIO.valid && io.axiRdataIO.ready
  val addrFire = io.axiRaddrIO.ready && io.axiRaddrIO.valid

  // 取指状态机
  val addr :: data :: Nil = Enum(2)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(addr)
  state := MuxLookup(state, addr, List(
    addr    -> Mux(addrFire && io.axiGrant, data, addr),
    data    -> Mux(dataFire, addr, data)
  ))

  // pc
  val pc  = RegInit(resetVector.U(64.W))
  val snpc = pc + 4.U
  val dnpc = io.redirect.brAddr

  // 取指接口，todo：停顿信号也要发挥作用
  //io.axiRaddrIO.valid       := state === addr && !io.stall
  io.axiRaddrIO.valid       := state === addr
  io.axiRaddrIO.bits.addr   := pc
  io.axiRdataIO.ready       := state === data

  // 始终没有写请求
  io.axiWaddrIO.valid       := false.B
  io.axiWaddrIO.bits.addr   := "h80000000".U
  io.axiWdataIO.valid       := false.B
  io.axiWdataIO.bits.wdata  := 0.U
  io.axiWdataIO.bits.wstrb  := 0.U
  io.axiBrespIO.ready       := false.B

  // 数据选择
  val instPre                = io.axiRdataIO.bits.rdata
  val inst                   = Mux(pc(2) === 0.U(1.W), instPre(31, 0), instPre(63, 32))

  val instReg                = RegInit(Instruction.NOP)
  instReg                   := Mux(state === data, inst, instReg)

  // 更新pc值
  pc                        := MuxLookup(state, pc, List(
                                  addr  -> pc,
                                  // 如果rresp不是okay，则pc保持原值重新取指，todo，当lsu取指成功后再更新pc
                                  //data  -> Mux(!((dataFire && !io.stall) || (io.stall && io.lsuReady)), pc, Mux(io.redirect.valid, dnpc, snpc))
                                  // 单周期时，wbu执行完毕才更新pc
                                  data  -> Mux(io.pcEnable, Mux(io.redirect.valid, dnpc, snpc), pc)
                                ))

  // 仿真环境
  io.debug.inst             := Mux(state === data || (state === addr && io.axiGrant), inst, instReg)
  io.debug.nextPc           := Mux(io.redirect.valid, dnpc, snpc)
  io.debug.pc               := pc
  //io.debug.execonce         := state === data && ((dataFire && !io.stall) || (io.stall && io.lsuReady))

  io.out.pc                 := pc
  io.out.inst               := Mux(state === data || (state === addr && io.axiGrant), inst, instReg)
  //io.out.inst               := inst

  io.axiReq                 := state === addr
  io.axiReady               := dataFire

  io.ready                  := state === data && dataFire
}