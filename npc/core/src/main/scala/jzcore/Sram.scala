package jzcore

import chisel3._
import chisel3.util._
import utils._

class Sram extends Module {
  val io = IO(new Bundle {
    // 读通道
    val raddrIO   = Flipped(Decoupled(new RaddrIO))
    val rdataIO   = Decoupled(new RdataIO)

    // 写通道
    val waddrIO   = Flipped(Decoupled(new WaddrIO))
    val wdataIO   = Flipped(Decoupled(new WdataIO))

    // 写回应通道
    val brespIO   = Decoupled(new BrespIO)
  })

  val pmem               = Module(new Pmem)

  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.raddrIO.valid && io.raddrIO.ready
  val rdataFire          = io.rdataIO.valid && io.rdataIO.ready

  val raddrReg            = RegInit(0.U(64.W))
  //val waddrReg            = RegInit(0.U(64.W))

  // read state
  val ar_wait :: fetch :: Nil = Enum(2)
  val rState = RegInit(ar_wait)
  rState := MuxLookup(rState, ar_wait, List(
    ar_wait       -> Mux(raddrFire, fetch, ar_wait), // 等待地址信息
    fetch         -> Mux(rdataFire, ar_wait, fetch), // 取指完成，当取指阻塞时保持状态
  ))

  raddrReg              := Mux(rState === ar_wait, io.raddrIO.bits.addr, raddrReg)

  // 读事务
  io.raddrIO.ready      := rState === ar_wait
  io.rdataIO.valid      := rState === fetch
  io.rdataIO.bits.rresp := okay // ok
  pmem.io.raddr         := raddrReg
  pmem.io.rvalid        := rState === fetch
  io.rdataIO.bits.rdata := pmem.io.rdata

  val waddrFire          = io.waddrIO.valid && io.waddrIO.ready
  val wdataFire          = io.wdataIO.valid && io.wdataIO.ready
  val brespFire          = io.brespIO.valid && io.brespIO.ready

  // write state
  val w_wait :: w_resp :: Nil = Enum(2)
  val wState = RegInit(w_wait)
  wState := MuxLookup(wState, w_wait, List(
    w_wait       -> Mux(waddrFire && wdataFire, w_resp, w_wait), // 等待地址信息
    w_resp       -> Mux(brespFire, w_wait, w_resp)
  ))

  // 写事务
  io.waddrIO.ready      := wState === w_wait
  io.wdataIO.ready      := wState === w_wait
  pmem.io.waddr         := io.waddrIO.bits.addr
  pmem.io.mask          := Mux(wState === w_wait && waddrFire && wdataFire, io.wdataIO.bits.wstrb, 0.U(8.W))
  pmem.io.wdata         := io.wdataIO.bits.wdata
  io.brespIO.valid      := wState === w_resp
  io.brespIO.bits.bresp := okay // ok
}
