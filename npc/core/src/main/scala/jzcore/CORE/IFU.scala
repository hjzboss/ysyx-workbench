package jzcore

import chisel3._
import top.Settings
import chisel3.util._


trait HasResetVector {
  val resetVector = if(Settings.get("sim")) Settings.getLong("ResetVector") else Settings.getLong("SocResetVector")
}

class IFU extends Module with HasResetVector {
  val io = IO(new Bundle {
    // 用于仿真环境
    val debug         = if(Settings.get("sim")) Some(new DebugIO) else None
    
    val valid         = Output(Bool()) // 是否是一条有效指令

    // from exu
    val exuRedirect   = Flipped(new RedirectIO)
    /*
    val icRedirect    = Flipped(new RedirectIO)
    //val wbuRedirect   = Flipped(new RedirectIO)

    // to idu
    val out           = new Stage1IO
    //val out           = new InstrFetch

    // icache
    //val icacheCtrl  = Decoupled(new CacheCtrlIO)
    //val icacheRead  = Flipped(Decoupled(new CacheReadIO))
    //val icacheWrite = Decoupled(new CacheWriteIO)*/
 
    // ctrl
    //val ready       = Output(Bool()) // 取指完成，主要用于唤醒流水线寄存器
    val stall       = Input(Bool()) // 停顿信号，停止pc的变化，并将取指的ready设置为false，保持取出的指令不变

    val out         = new InstrFetch
  })

  /*
  // 非流水icache begin
  val addrFire      = io.icacheCtrl.valid && io.icacheCtrl.ready
  val rdataFire     = io.icacheRead.valid && io.icacheRead.ready
  val wdataFire     = io.icacheWrite.valid && io.icacheWrite.ready

  // todo: 当state位于data状态时需要等待cache处理完才可进行分支跳转
  // 取指状态机
  val addr :: data :: Nil = Enum(2)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(addr)
  state := MuxLookup(state, addr, List(
    addr    -> Mux(io.exuRedirect.valid, addr, Mux(addrFire && !io.stall, data, addr)),
    data    -> Mux(rdataFire && !io.stall, addr, data)
  ))

  // 保存跳转分支
  val brAddr                 = RegInit(resetVector.U(32.W))
  brAddr                    := Mux(io.exuRedirect.valid, io.exuRedirect.brAddr, brAddr)
  // 分支跳转标志，用于返回icache一个正确的信号，防止阻塞
  val brFlag                 = RegInit(false.B)
  brFlag                    := Mux(state === addr || (state === data && rdataFire), false.B, Mux(io.exuRedirect.valid, true.B, brFlag))
  val dnpc         = io.exuRedirect.brAddr
  // 非流水icache end
  */
  val valid        = dontTouch(WireDefault(false.B))
  valid           := !io.stall && !io.exuRedirect.valid
  io.valid        := valid

  // pc
  val pc           = RegInit(resetVector.U(32.W))
  val snpc         = pc + 4.U(32.W)

  pc              := Mux(io.stall, pc, Mux(io.exuRedirect.valid, io.exuRedirect.brAddr, snpc))
  //pc              := Mux(io.stall, pc, Mux(io.exuRedirect.valid, io.exuRedirect.brAddr, Mux(io.icRedirect.valid, io.icRedirect.brAddr, snpc)))
  val imem         = Module(new IMEM)
  imem.io.pc      := pc
  io.out.pc       := pc
  io.out.inst     := Mux(valid, imem.io.inst, Instruction.NOP)

  /*
  io.out.addr     := pc 
  if(Settings.get("sim")) {
    io.out.cacheable := true.B
  } else {
    io.out.cacheable := pc <= "hffff_ffff".U && pc >= "h8000_0000".U
  }
  */

  if(Settings.get("sim")) {
    //io.debug.get.nextPc := Mux(io.stall, pc, Mux(io.exuRedirect.valid, io.exuRedirect.brAddr, Mux(io.icRedirect.valid, io.icRedirect.brAddr, snpc)))
    io.debug.get.nextPc := Mux(io.stall, pc, Mux(io.exuRedirect.valid, io.exuRedirect.brAddr, snpc))
    io.debug.get.pc     := pc
    //io.debug.get.inst   := Instruction.NOP
    io.debug.get.inst   := io.out.inst
    io.debug.get.valid  := valid
  }

  /*
  val valid        = dontTouch(WireDefault(false.B))
  valid           := !io.stall && !io.exuRedirect.valid && !io.icRedirect.valid
  io.valid        := valid
  */
  /*
  io.icacheCtrl.valid       := state === addr && !io.stall && !io.exuRedirect.valid
  io.icacheCtrl.bits.addr   := pc
  io.icacheCtrl.bits.wen    := false.B
  io.icacheCtrl.bits.cacheable := true.B // todo

  io.icacheRead.ready       := state === data && !io.stall
  
  // todo: 是否会写？
  io.icacheWrite.valid      := false.B
  io.icacheWrite.bits.wdata := 0.U(64.W)
  io.icacheWrite.bits.wmask := 0.U(8.W)
  
  // 当发生分支跳转时忽略icache返回的数据
  val instPre                = Mux(brFlag || io.exuRedirect.valid, 0.U(32.W), io.icacheRead.bits.rdata)
  val inst                   = Mux(pc(2) === 0.U(1.W), instPre(31, 0), instPre(63, 32))

  // 当停顿信号生效时保持原pc
  val stallPc                = dontTouch(Wire(UInt(32.W)))
  stallPc                   := Mux(rdataFire && !io.stall, Mux(brFlag, brAddr, snpc), pc)

  pc                        := MuxLookup(state, pc, List(
                                  addr  -> Mux(io.exuRedirect.valid && !io.stall, dnpc, pc),  
                                  data  -> stallPc
                                ))

  // 仿真环境
  io.debug.inst             := inst
  io.debug.nextPc           := stallPc
  io.debug.pc               := pc

  io.out.pc                 := pc
  io.out.inst               := inst

  // 取指完毕信号，用于提醒流水线寄存器传递数据
  io.ready                  := (state === data && rdataFire) || io.stall // todo
  io.valid                  := state === data && rdataFire && !brFlag // 分支发生跳转时的指令是无效的
  */
}