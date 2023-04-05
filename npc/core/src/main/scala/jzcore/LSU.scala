package jzcore

import chisel3._
import chisel3.util._
import utils._


class LSU extends Module {
  val io = IO(new Bundle {
    // exu传入
    val in        = Flipped(new MemCtrl)

    // 传给wbu
    val out       = new LsuOut

    // 送给ctrl模块，用于停顿
    val lsuReady  = Output(Bool())
    
    val stall     = Input(Bool())

    // axi总线访存接口
    val raddrIO   = Decoupled(new AddrIO)
    val rdataIO   = Flipped(Decoupled(new RdataIO))

    val waddrIO   = Decoupled(new AddrIO)
    val wdataIO   = Decoupled(new WdataIO)
    val brespIO   = Flipped(Decoupled(new BrespIO))
  })

  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.raddrIO.valid && io.raddrIO.ready
  val rdataFire          = io.rdataIO.valid && io.rdataIO.ready
  val waddrFire          = io.waddrIO.valid && io.waddrIO.ready
  val wdataFire          = io.wdataIO.valid && io.wdataIO.ready
  val brespFire          = io.brespIO.valid && io.brespIO.ready

  val addr               = io.in.addr

  // load状态机
  val idle :: wait_data :: wait_resp ::Nil = Enum(3)
  val rState = RegInit(idle)
  rState := MuxLookup(rState, idle, List(
    idle        -> Mux(raddrFire, wait_data, idle),
    wait_data   -> Mux(rdataFire, idle, wait_data)
  ))

  val rresp              = io.rdataIO.bits.rresp // todo
  val bresp              = io.brespIO.bits.bresp

  io.raddrIO.valid      := rState === idle && !io.stall && io.in.ren
  io.raddrIO.bits.addr  := addr
  io.rdataIO.ready      := rState === wait_data && !io.stall

  // store状态机
  val wState = RegInit(addr)
  wState := MuxLookup(wState, idle, List(
    idle        -> Mux(waddrFire && wdataFire, wait_resp, idle),
    wait_resp   -> Mux(brespFire, idle, wait_resp)
  ))

  io.waddrIO.valid      := wState === idle && !io.stall && io.in.wen
  io.waddrIO.bits.addr  := addr
  io.wdataIO.valid      := wState === idle && !io.stall && io.in.wen
  io.wdataIO.bits.wdata := io.in.wdata
  io.wdataIO.bits.wstrb := io.in.wmask << addr(2, 0) // todo
  
  // 数据对齐
  val rdata              = io.rdataIO.bits.rdata >> (Cat(0.U(3.W), addr(2, 0)) << 3.U)
  val lsuOut  = LookupTree(io.in.lsType, Seq(
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

  io.out.lsuOut         := lsuOut
  io.out.loadMem        := io.in.loadMem
  io.out.exuOut         := io.in.exuOut
  io.out.rd             := io.in.rd
  io.out.regWen         := Mux(io.lsuReady, io.in.regWen, false.B)
  io.out.pc             := io.in.pc
  io.out.no             := io.in.no
  io.out.exception      := Mux(io.lsuReady, io.in.exception, false.B)
  io.out.csrWaddr       := io.in.csrWaddr
  io.out.csrWen         := Mux(io.lsuReady, io.in.csrWen, false.B)

  io.lsuReady           := (!io.in.ren && !io.in.wen) || (((rState === wait_data && rdataFire) || (wState === wait_resp && brespFire)) && (rresp === okay || bresp === okay))
}