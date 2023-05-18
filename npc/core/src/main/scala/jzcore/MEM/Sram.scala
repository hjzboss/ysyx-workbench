package jzcore

import chisel3._
import chisel3.util._
import utils._

// just transfer with cache
class Sram extends Module {
  val io = IO(new Bundle {
    // read channel
    val raddrIO   = Flipped(Decoupled(new RaddrIO))
    val rdataIO   = Decoupled(new RdataIO)

    // write channel
    val waddrIO   = Flipped(Decoupled(new WaddrIO))
    val wdataIO   = Flipped(Decoupled(new WdataIO))

    // write response channel
    val brespIO   = Decoupled(new BrespIO)
  })

  val pmem               = Module(new Pmem)

  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.raddrIO.valid && io.raddrIO.ready
  val rdataFire          = io.rdataIO.valid && io.rdataIO.ready

  val raddrReg           = RegInit(0.U(32.W))
  val waddrReg           = RegInit(0.U(32.W))
  val wmaskReg           = RegInit(0.U(8.W))

  // read state
  val rcnt               = RegInit(0.U(2.W))
  val rsize              = RegInit(1.U(2.W))

  val ar_wait :: fetch :: Nil = Enum(2)
  val rState  = RegInit(ar_wait)
  rState := MuxLookup(rState, ar_wait, List(
    ar_wait       -> Mux(raddrFire, fetch, ar_wait), // 等待地址信息
    fetch         -> Mux(rdataFire && rcnt === rsize, ar_wait, fetch), // 取指完成，当取指阻塞时保持状态
  ))
  
  rcnt                  := Mux(rState === ar_wait, 0.U(2.W), Mux(rState === fetch && rdataFire, rcnt + 1.U(2.W), rcnt))
  rsize                 := Mux(rState === ar_wait && raddrFire, io.raddrIO.bits.len - 1.U(2.W), rsize)
  raddrReg              := Mux(rState === ar_wait, io.raddrIO.bits.addr, raddrReg)

  // 读事务
  io.raddrIO.ready      := rState === ar_wait
  io.rdataIO.valid      := rState === fetch
  io.rdataIO.bits.rresp := okay // ok
  io.rdataIO.bits.rlast := rState === fetch && (rsize === rcnt)
  //io.rdataIO.bits.rlast := rState === fetch && (rcnt === 1.U(2.W) && rdataFire)
  // wrap burst
  when(rState === fetch) {
    pmem.io.raddr       := Mux(rcnt === 0.U(2.W), raddrReg, Cat(raddrReg(31, 4),  Cat(~raddrReg(3), raddrReg(2, 0))))
  }
  pmem.io.rvalid        := rState === fetch
  io.rdataIO.bits.rdata := pmem.io.rdata

  val waddrFire          = io.waddrIO.valid && io.waddrIO.ready
  val wdataFire          = io.wdataIO.valid && io.wdataIO.ready
  val brespFire          = io.brespIO.valid && io.brespIO.ready

  // write state
  val w_wait :: w_data :: w_resp :: Nil = Enum(3)
  val wState = RegInit(w_wait)
  wState := MuxLookup(wState, w_wait, List(
    w_wait       -> Mux(waddrFire && wdataFire && io.wdataIO.bits.wlast, w_resp, Mux(waddrFire, w_data, w_wait)),
    //w_wait       -> Mux(waddrFire, w_data, w_wait), // 等待地址信息
    w_data       -> Mux(wdataFire && io.wdataIO.bits.wlast, w_resp, w_data),
    w_resp       -> Mux(brespFire, w_wait, w_resp)
  ))

  //val wsize              = RegInit(1.U(2.W))
  //wsize                 := Mux(wState === w_wait && waddrFire, io.waddrIO.bits.len - 1.U(2.W), wsize)

  val wcnt               = RegInit(0.U(2.W))
  when(wState === w_wait) {
    wcnt  := Mux(waddrFire && wdataFire, 1.U(2.W), 0.U(2.W))
  }.elsewhen(wState === w_data) {
    wcnt  := Mux(wdataFire, wcnt + 1.U(2.W), wcnt)
  }.otherwise {
    wcnt  := 0.U(2.W)
  }

  waddrReg              := Mux(wState === w_wait, io.waddrIO.bits.addr, waddrReg)
  wmaskReg              := Mux(wState === w_wait, io.wdataIO.bits.wstrb, wmaskReg)

  // 写事务
  io.waddrIO.ready      := wState === w_wait
  io.wdataIO.ready      := wState === w_wait || wState === w_data
  when(wState === w_wait && wdataFire && waddrFire) {
    pmem.io.waddr := io.waddrIO.bits.addr
    pmem.io.mask  := io.wdataIO.bits.wstrb
  }.elsewhen(wState === w_data) {
    pmem.io.waddr := Mux(wcnt === 0.U(2.W), waddrReg, Cat(waddrReg(31, 4), Cat(~waddrReg(3), waddrReg(2, 0))))
    pmem.io.mask  := Mux(wdataFire, wmaskReg, 0.U(8.W))
  }.otherwise {
    pmem.io.mask  := 0.U(8.W)
  }
  //pmem.io.waddr         := io.waddrIO.bits.addr
  //pmem.io.mask          := Mux(wState === w_wait && waddrFire && wdataFire, io.wdataIO.bits.wstrb, 0.U(8.W))
  pmem.io.wdata         := io.wdataIO.bits.wdata
  io.brespIO.valid      := wState === w_resp
  io.brespIO.bits.bresp := okay // ok
}
