package jzcore

import chisel3._
import chisel3.util._
import utils._

class Sram extends Module {
  val io = IO(new Bundle {
    val raddrIO   = Flipped(Decoupled(new AddrIO))
    val rdataIO   = Decoupled(new RdataIO)
  })

  // read state
  val ar_wait :: fetch :: Nil = Enum(2)
  val rState = RegInit(ar_wait)
  rState := MuxLookup(rState, ar_wait, List(
    ar_wait       -> Mux(io.raddrIO.valid && io.raddrIO.ready, fetch, ar_wait), // 等待地址信息
    fetch         -> Mux(io.rdataIO.valid && io.rdataIO.ready, ar_wait, fetch), // 取指完成，当取指阻塞时保持状态
  ))

  val pmem               = Module(new Pmem)

  io.raddrIO.ready      := true.B // todo

  val raddrFire          = io.raddrIO.valid && io.raddrIO.ready

  io.rdataIO.valid      := rState === fetch

  pmem.io.raddr         := io.raddrIO.bits.addr
  pmem.io.rvalid        := rState === fetch
  io.rdataIO.bits.data  := pmem.io.rdata
}