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

    // clint接口
    val clintIO     = Flipped(new ClintIO)

    val lsFlag        = if(Settings.get("sim")) Some(Output(Bool())) else None
  })

  val addr        = io.in.lsuAddr
  val readTrans   = io.in.lsuRen
  val writeTrans  = io.in.lsuWen
  val hasTrans    = readTrans || writeTrans

  val pmem        = Module(new Pmem) // debug
  val clintSel                   = dontTouch(WireDefault(false.B))
  clintSel                      := (addr <= 0x0200ffff.U) && (addr >= 0x02000000.U) // clint select


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