package jzcore

import chisel3._
import chisel3.util._
import utils._

// todo: flush信号的处理，现在的想法：在idu阶段检测中断，在wbu阶段进行异常号等等的写回和异常地址的跳转
// todo：wbu阶段跳转的优先级高于exu阶段
class LSU extends Module {
  val io = IO(new Bundle {
    // exu传入
    val in          = Flipped(new ExuOut)

    val flushCsr    = Input(Bool())

    // 传给wbu
    val out         = new LsuOut

    // 送给ctrl模块，用于停顿
    val ready       = Output(Bool())
    
    // dcache访问接口
    val dcacheCtrl  = Decoupled(new CacheCtrlIO)
    val dcacheRead  = Flipped(Decoupled(new CacheReadIO))
    val dcacheWrite = Decoupled(new CacheWriteIO)
    val dcacheCoh   = new CoherenceIO

    // clint接口
    val clintIO     = Flipped(new ClintIO)

    /*
    // axi总线访存接口，用于外设的访问
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

    //val lsFlag      = Output(Bool())
  })

  /*
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire          = io.axiRdataIO.valid && io.axiRdataIO.ready
  val waddrFire          = io.axiWaddrIO.valid && io.axiWaddrIO.ready
  val wdataFire          = io.axiWdataIO.valid && io.axiWdataIO.ready
  val brespFire          = io.axiBrespIO.valid && io.axiBrespIO.ready

  // todo
  io.axiRaddrIO.bits.len := 0.U
  io.axiRaddrIO.bits.size:= 3.U
  io.axiRaddrIO.bits.burst := 2.U(2.W)
  io.axiWaddrIO.bits.len := 0.U
  io.axiWaddrIO.bits.size:= 3.U
  io.axiWaddrIO.bits.burst := 2.U(2.W)
  io.axiWdataIO.bits.wlast := true.B
  */

  val addr        = io.in.lsuAddr
  val readTrans   = io.in.lsuRen
  val writeTrans  = io.in.lsuWen
  val hasTrans    = readTrans || writeTrans

  val ctrlFire    = io.dcacheCtrl.valid && io.dcacheCtrl.ready
  val readFire    = io.dcacheRead.valid && io.dcacheRead.ready
  val writeFire   = io.dcacheWrite.valid && io.dcacheWrite.ready
  val coherenceFire = io.dcacheCoh.valid && io.dcacheCoh.ready

  val clintSel                   = dontTouch(WireDefault(false.B))
  clintSel                      := (addr <= 0x0200ffff.U) && (addr >= 0x02000000.U) // clint select

  val idle :: ctrl :: data :: coherence :: Nil = Enum(4)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle -> Mux(hasTrans && !clintSel, ctrl, Mux(io.in.coherence, coherence, idle)),
    ctrl -> Mux(ctrlFire, data, ctrl),
    data -> Mux(readFire || writeFire, idle, data), // todo
    coherence -> Mux(coherenceFire, idle, coherence)
  ))

  //val cacheable                  = addr =/= 0xa0000048L.U && addr =/= 0xa0000050L.U && addr =/= 0xa0000100L.U && addr =/= 0xa0000080L.U && addr =/= 0xa00003f8L.U && addr =/= 0xa0000108L.U && !(addr >= 0xa1000000L.U && addr <= 0xa2000000L.U)
  val cacheable                  = addr <= "hffff_ffff".U && addr >= "h8000_0000".U
  val flash                      = addr <= "h3fff_ffff".U && addr >= "h3000_0000".U
  io.dcacheCtrl.valid           := state === ctrl
  io.dcacheCtrl.bits.wen        := writeTrans
  io.dcacheCtrl.bits.addr       := addr
  io.dcacheCtrl.bits.cacheable  := cacheable
  // 指定cache访问axi的size
  val size                      = LookupTree(io.in.lsType, Seq(
                                      LsType.ld   -> AxiWidth.double,
                                      LsType.lw   -> AxiWidth.word,
                                      LsType.lh   -> AxiWidth.half,
                                      LsType.lb   -> AxiWidth.byte,
                                      LsType.lbu  -> AxiWidth.byte,
                                      LsType.lhu  -> AxiWidth.half,
                                      LsType.lwu  -> AxiWidth.word,
                                      LsType.sd   -> AxiWidth.double,
                                      LsType.sw   -> AxiWidth.word,
                                      LsType.sh   -> AxiWidth.half,
                                      LsType.sb   -> AxiWidth.byte,
                                      LsType.nop  -> AxiWidth.double
                                    ))
  io.dcacheCtrl.bits.size       := Mux(cacheable, AxiWidth.double, size)

  io.dcacheRead.ready           := state === data
  io.dcacheWrite.valid          := state === data
  // todo：sdram需不需要对齐？
  io.dcacheWrite.bits.wdata     := Mux(cacheable, io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U), io.in.lsuWdata << (ZeroExt(addr(1, 0), 4) << 3.U))
  io.dcacheWrite.bits.wmask     := Mux(cacheable, io.in.wmask << addr(2, 0), io.in.wmask << addr(1, 0))
  //io.dcacheWrite.bits.wdata     := io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U)
  //io.dcacheWrite.bits.wmask     := io.in.wmask << addr(2, 0)

  // coherence
  io.dcacheCoh.valid            := io.in.coherence

  /*
  // load状态机
  val idle :: wait_data :: wait_resp ::Nil = Enum(3)
  val rState = RegInit(idle)
  rState := MuxLookup(rState, idle, List(
    idle        -> Mux(raddrFire && io.axiGrant, wait_data, idle),
    wait_data   -> Mux(rdataFire, idle, wait_data)
  ))

  val rresp              = io.axiRdataIO.bits.rresp // todo
  val bresp              = io.axiBrespIO.bits.bresp

  io.axiRaddrIO.valid      := rState === idle && io.in.lsuRen
  io.axiRaddrIO.bits.addr  := addr
  io.axiRdataIO.ready      := rState === wait_data

  // store状态机
  val wState = RegInit(idle)
  wState := MuxLookup(wState, idle, List(
    idle        -> Mux(waddrFire && wdataFire && io.axiGrant, wait_resp, idle),
    wait_resp   -> Mux(brespFire, idle, wait_resp)
  ))

  io.axiWaddrIO.valid      := wState === idle && io.in.lsuWen
  io.axiWaddrIO.bits.addr  := addr
  io.axiWdataIO.valid      := wState === idle && io.in.lsuWen
  io.axiWdataIO.bits.wdata := io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U)
  io.axiWdataIO.bits.wstrb := io.in.wmask << addr(2, 0) // todo
  io.axiBrespIO.ready      := wState === wait_resp
  */

  // clint访问
  io.clintIO.addr       := addr
  io.clintIO.wen        := writeTrans && clintSel
  io.clintIO.wdata      := io.in.lsuWdata
  io.clintIO.wmask      := io.in.wmask

  // 数据对齐
  val align64            = Cat(addr(2, 0), 0.U(3.W))
  val align32            = Mux(flash, Cat(addr(1, 0), 0.U(3.W)), 0.U) // todo: sdram的访问可能需要配置
  val rdata              = Mux(cacheable, io.dcacheRead.bits.rdata >> align64, io.dcacheRead.bits.rdata >> align32)
  val lsuOut             = LookupTree(io.in.lsType, Seq(
                            LsType.ld   -> rdata,
                            LsType.lw   -> SignExt(rdata(31, 0), 64),
                            LsType.lh   -> SignExt(rdata(15, 0), 64),
                            LsType.lb   -> SignExt(rdata(7, 0), 64),
                            LsType.lbu  -> ZeroExt(rdata(7, 0), 64),
                            LsType.lhu  -> ZeroExt(rdata(15, 0), 64),
                            LsType.lwu  -> ZeroExt(rdata(31, 0), 64),
                            LsType.sd   -> rdata,
                            LsType.sw   -> rdata,
                            LsType.sh   -> rdata,
                            LsType.sb   -> rdata,
                            LsType.nop  -> rdata
                          ))

  val pc                 = dontTouch(Wire(UInt(32.W)))
  pc                    := io.in.pc

  io.out.lsuOut         := Mux(clintSel, io.clintIO.rdata, lsuOut)
  //io.out.lsuOut         := rdata
  io.out.loadMem        := io.in.loadMem
  io.out.exuOut         := io.in.exuOut
  io.out.rd             := io.in.rd
  io.out.regWen         := io.in.regWen
  io.out.pc             := pc
  io.out.excepNo        := io.in.excepNo
  io.out.exception      := io.in.exception
  io.out.csrWaddr       := io.in.csrWaddr
  io.out.csrWen         := Mux(io.flushCsr, false.B, io.in.csrWen)
  //io.out.ebreak         := io.in.ebreak
  //io.out.haltRet        := io.in.haltRet
  io.out.csrValue       := io.in.csrValue
  io.out.int            := io.in.int

  //io.ready              := !(readTrans || writeTrans) || ((rState === wait_data && rdataFire) || (wState === wait_resp && brespFire)) && (rresp === okay || bresp === okay)
  io.ready              := (state === idle && !(readTrans || writeTrans) && !io.in.coherence) || (state === data && (readFire || writeFire)) || (state === coherence && coherenceFire) || clintSel
  // 仲裁信号
  //io.axiReq             := (rState === idle && io.in.lsuRen) || (wState === idle && io.in.lsuWen)
  //io.axiReady           := (rState === wait_data && rdataFire) || (brespFire && wState === wait_resp)

  // 传给仿真环境，用于外设访问的判定
  //io.lsFlag             := io.in.lsuRen || io.in.lsuWen
}