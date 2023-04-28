package jzcore

import chisel3._
import top.Settings
import chisel3.util._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

class IFU extends Module with HasResetVector {
  val io = IO(new Bundle {
    // 用于仿真环境
    val debug       = new DebugIO
    
    val valid       = Output(Bool()) // 是否是一条有效指令，用于提示仿真环境

    // 来自exu
    val redirect    = Flipped(new RedirectIO)

    // 送给idu
    val out         = new InstrFetch

/*
    // axi
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    
    // 仲裁信号
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())
*/

    // icache读端口，todo
    val icacheIO    = Decoupled(new CacheIO)

    // 控制模块
    val ready       = Output(Bool()) // 取指完成，主要用于唤醒流水线寄存器
    val stall       = Input(Bool()) // 停顿信号，停止pc的变化，并将取指的ready设置为false，保持取出的指令不变
  })

  // axi握手信号
  val dataFire = io.axiRdataIO.valid && io.axiRdataIO.ready
  val addrFire = io.axiRaddrIO.ready && io.axiRaddrIO.valid

  // 取指状态机
  val addr :: data :: Nil = Enum(2)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(addr)
  state := MuxLookup(state, addr, List(
    addr    -> Mux(io.redirect.valid, addr, Mux(addrFire, data, addr)),
    data    -> Mux(dataFire, addr, data)
  ))

  // pc
  val pc  = RegInit(resetVector.U(64.W))
  val snpc = pc + 4.U
  val dnpc = io.redirect.brAddr

/*
  // cache
  io.icacheIO.valid          := state === addr && !io.stall && !io.redirect.valid
  io.icacheIO.bits.addr      := pc
*/
  // axi取指接口
  io.axiRaddrIO.valid       := state === addr && !io.stall && !io.redirect.valid
  io.axiRaddrIO.bits.addr   := pc
  // 使用cache时的准备：当lsu访存未结束时应该阻塞ifu阶段，要保证取出的指令的值保持到lsu访存完成
  io.axiRdataIO.ready       := state === data

  // 始终没有写请求
  io.axiWaddrIO.valid       := false.B
  io.axiWaddrIO.bits.addr   := "h80000000".U
  io.axiWdataIO.valid       := false.B
  io.axiWdataIO.bits.wdata  := 0.U
  io.axiWdataIO.bits.wstrb  := 0.U
  io.axiBrespIO.ready       := false.B

  // 数据选择, todo: 从cache中选择
  val instPre                = io.axiRdataIO.bits.rdata
  //val instPre                = io.icacheIO.bits.data
  val inst                   = Mux(pc(2) === 0.U(1.W), instPre(31, 0), instPre(63, 32))

  val stallPc                = dontTouch(Wire(UInt(64.W)))
  stallPc                   := Mux(io.stall, pc, Mux(io.redirect.valid, dnpc, snpc))

  pc                        := MuxLookup(state, pc, List(
                                  addr  -> Mux(io.redirect.valid && !io.stall, dnpc, pc),  
                                  // 当停顿信号生效时保持原pc
                                  data  -> stallPc
                                ))

  // 仿真环境
  io.debug.inst             := inst
  io.debug.nextPc           := Mux(io.redirect.valid, dnpc, snpc)
  io.debug.pc               := pc

  io.out.pc                 := pc
  io.out.inst               := inst

  io.axiReq                 := state === addr
  io.axiReady               := state === data && dataFire

  // 取指完毕信号，用于提醒流水线寄存器传递数据
  io.ready                  := (state === data && dataFire) || io.stall // todo
  io.valid                  := state === data && dataFire
}