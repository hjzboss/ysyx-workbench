package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

class FastLSU extends Module {
  val io = IO(new Bundle {
    // exu传入
    val in          = Flipped(new ExuOut)
    val stall       = Input(Bool())

    // 传给wbu
    val out         = new LsuOut

    // 送给ctrl模块，用于停顿
    val ready       = Output(Bool())
    
    // dcache访问接口
    //val dcacheCtrl  = Decoupled(new CacheCtrlIO)
    //val dcacheRead  = Flipped(Decoupled(new CacheReadIO))
    //val dcacheWrite = Decoupled(new CacheWriteIO)
    //val dcacheCoh   = new CoherenceIO

    // clint接口
    val clintIO     = Flipped(new ClintIO)

    val lsFlag        = if(Settings.get("sim")) Some(Output(Bool())) else None
  })

  val addr        = io.in.lsuAddr
  val readTrans   = io.in.lsuRen
  val writeTrans  = io.in.lsuWen
  val hasTrans    = readTrans || writeTrans

  val pmem        = Module(new Pmem) // debug
  /*
  val ctrlFire    = io.dcacheCtrl.valid && io.dcacheCtrl.ready
  val readFire    = io.dcacheRead.valid && io.dcacheRead.ready
  val writeFire   = io.dcacheWrite.valid && io.dcacheWrite.ready
  val coherenceFire = io.dcacheCoh.valid && io.dcacheCoh.ready
*/
  val clintSel                   = dontTouch(WireDefault(false.B))
  clintSel                      := (addr <= 0x0200ffff.U) && (addr >= 0x02000000.U) // clint select

  /*
  val idle :: ctrl :: data :: coherence :: Nil = Enum(4)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle -> Mux(hasTrans && !clintSel, ctrl, Mux(io.in.coherence, coherence, idle)),
    ctrl -> Mux(ctrlFire, data, ctrl),
    data -> Mux(readFire || writeFire, idle, data), // todo
    coherence -> Mux(coherenceFire, idle, coherence)
  ))

  val cacheable = if(Settings.get("sim")) { addr =/= 0xa0000048L.U && addr =/= 0xa0000050L.U && addr =/= 0xa0000100L.U && addr =/= 0xa0000080L.U && addr =/= 0xa00003f8L.U && addr =/= 0xa0000108L.U && !(addr >= 0xa1000000L.U && addr <= 0xa2000000L.U) } else { addr <= "hffff_ffff".U && addr >= "h8000_0000".U }
  var flash = addr <= "h3fff_ffff".U && addr >= "h3000_0000".U

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

  if(Settings.get("sim")) {
    io.dcacheWrite.bits.wdata     := io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U)
    io.dcacheWrite.bits.wmask     := io.in.wmask << addr(2, 0)
  } else {
    io.dcacheWrite.bits.wdata     := Mux(cacheable, io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U), io.in.lsuWdata << (ZeroExt(addr(1, 0), 4) << 3.U))
    io.dcacheWrite.bits.wmask     := Mux(cacheable, io.in.wmask << addr(2, 0), io.in.wmask << addr(1, 0))
  }

  // coherence
  io.dcacheCoh.valid            := io.in.coherence
  */

  // clint访问
  io.clintIO.addr       := addr
  io.clintIO.wen        := writeTrans && clintSel
  io.clintIO.wdata      := io.in.lsuWdata
  io.clintIO.wmask      := io.in.wmask

  // debug
  pmem.io.raddr         := addr
  pmem.io.rvalid        := readTrans & !io.stall
  pmem.io.waddr         := addr
  pmem.io.wdata         := io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U)
  pmem.io.mask          := io.in.wmask << addr(2, 0)
  //

  // 数据对齐
  val align64            = Cat(addr(2, 0), 0.U(3.W)) // debug
  //val align32            = Mux(flash, Cat(addr(1, 0), 0.U(3.W)), 0.U) // todo: sdram的访问可能需要配置
  //val rdata              = if(Settings.get("sim")) { io.dcacheRead.bits.rdata >> align64 } else { Mux(cacheable, io.dcacheRead.bits.rdata >> align64, io.dcacheRead.bits.rdata >> align32) }
  val rdata              = pmem.io.rdata >> align64
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
  io.out.loadMem        := io.in.loadMem
  io.out.exuOut         := io.in.exuOut
  io.out.rd             := io.in.rd
  io.out.regWen         := io.in.regWen
  io.out.pc             := pc
  io.out.excepNo        := io.in.excepNo
  io.out.exception      := io.in.exception
  io.out.csrWaddr       := io.in.csrWaddr
  io.out.csrWen         := io.in.csrWen
  io.out.csrValue       := io.in.csrValue
  io.out.mret           := io.in.mret
  io.out.csrChange      := io.in.csrChange

  io.ready              := true.B // debug

  if(Settings.get("sim")) {
    // 传给仿真环境，用于外设访问的判定
    io.lsFlag.get           := io.in.lsuRen || io.in.lsuWen
    io.out.ebreak.get       := io.in.ebreak.get
    io.out.haltRet.get      := io.in.haltRet.get
  }
}