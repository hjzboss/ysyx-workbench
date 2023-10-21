package jzcore

import chisel3._
import chisel3.util._
import utils._

// just transfer with cache, for verilator simulation, dpi-c sram
class Sram extends Module {
  val io = IO(new Bundle {
    /*
    // read channel
    val raddrIO   = Flipped(Decoupled(new RaddrIO))
    val rdataIO   = Decoupled(new RdataIO)
    // write channel
    val waddrIO   = Flipped(Decoupled(new WaddrIO))
    val wdataIO   = Flipped(Decoupled(new WdataIO))
    // write response channel
    val brespIO   = Decoupled(new BrespIO)*/

    val slave     = Flipped(new AxiMaster)
  })

  val pmem               = Module(new Pmem)

  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.slave.arvalid && io.slave.arready
  val rdataFire          = io.slave.rvalid && io.slave.rready

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
  rsize                 := Mux(rState === ar_wait && raddrFire, io.slave.arlen, rsize)
  raddrReg              := Mux(rState === ar_wait, io.slave.araddr, raddrReg)

  // 读事务
  io.slave.arready      := rState === ar_wait
  io.slave.rvalid       := rState === fetch
  io.slave.rresp        := okay // ok
  io.slave.rlast        := rState === fetch && (rsize === rcnt)

  // wrap burst
  when(rState === fetch) {
    pmem.io.raddr       := Mux(rcnt === 0.U(2.W), raddrReg, Cat(raddrReg(31, 4),  Cat(~raddrReg(3), raddrReg(2, 0))))
  }
  pmem.io.rvalid        := rState === fetch
  io.slave.rdata        := pmem.io.rdata

  val waddrFire          = io.slave.awvalid && io.slave.awready
  val wdataFire          = io.slave.wvalid && io.slave.wready
  val brespFire          = io.slave.bvalid && io.slave.bready

  // write state
  val w_wait :: w_data :: w_resp :: Nil = Enum(3)
  val wState = RegInit(w_wait)
  wState := MuxLookup(wState, w_wait, List(
    w_wait       -> Mux(waddrFire && wdataFire && io.slave.wlast, w_resp, Mux(waddrFire, w_data, w_wait)),
    w_data       -> Mux(wdataFire && io.slave.wlast, w_resp, w_data),
    w_resp       -> Mux(brespFire, w_wait, w_resp)
  ))

  val wcnt               = RegInit(0.U(2.W))
  when(wState === w_wait) {
    wcnt  := Mux(waddrFire && wdataFire, 1.U(2.W), 0.U(2.W))
  }.elsewhen(wState === w_data) {
    wcnt  := Mux(wdataFire, wcnt + 1.U(2.W), wcnt)
  }.otherwise {
    wcnt  := 0.U(2.W)
  }

  waddrReg              := Mux(wState === w_wait, io.slave.awaddr, waddrReg)
  wmaskReg              := Mux(wState === w_wait, io.slave.wstrb, wmaskReg)

  // 写事务
  io.slave.awready      := wState === w_wait
  io.slave.wready       := wState === w_wait || wState === w_data
  when(wState === w_wait && wdataFire && waddrFire) {
    pmem.io.waddr := io.slave.awaddr
    pmem.io.mask  := io.slave.wstrb
  }.elsewhen(wState === w_data) {
    pmem.io.waddr := Mux(wcnt === 0.U(2.W), waddrReg, Cat(waddrReg(31, 4), Cat(~waddrReg(3), waddrReg(2, 0))))
    pmem.io.mask  := Mux(wdataFire, wmaskReg, 0.U(8.W))
  }.otherwise {
    pmem.io.mask  := 0.U(8.W)
  }
  pmem.io.wdata         := io.slave.wdata
  io.slave.bvalid       := wState === w_resp
  io.slave.bresp        := okay
  io.slave.bid          := 0.U
  io.slave.rid          := 0.U
}
