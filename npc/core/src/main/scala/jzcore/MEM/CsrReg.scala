package jzcore

import chisel3._
import chisel3.util._
import utils._

/*
class CsrReg extends BlackBox {
  val io = IO(new Bundle {
    val clock     = Input(Clock())
    val reset     = Input(Bool())

    // exception
    val exception = Input(Bool())
    val epc       = Input(UInt(64.W))
    val no        = Input(UInt(4.W))

    val raddr     = Input(UInt(3.W))
    val rdata     = Output(UInt(64.W))

    val waddr     = Input(UInt(3.W))
    val wen       = Input(Bool())
    val wdata     = Input(UInt(64.W))
  })
}
*/

class CsrReg extends Module {
  val io = IO(new Bundle {
    val stall     = Input(Bool())

    // exception
    val exception = Input(Bool())
    val epc       = Input(UInt(64.W))
    val no        = Input(UInt(4.W))

    val raddr     = Input(UInt(12.W))
    val rdata     = Output(UInt(64.W))

    val waddr     = Input(UInt(12.W))
    val wen       = Input(Bool())
    val wdata     = Input(UInt(64.W))

    // clint interrupt
    val timerInt  = Input(Bool())
    val int       = Output(Bool())
  })

  // csr0: mstatus, csr1: mtvec, csr2: mepc, csr3: mcause
  //val csr = RegInit(VecInit(List.fill(4)(0.U(64.W))))

  val mstatus = RegInit(0.U(64.W))
  val mtvec   = RegInit(0.U(64.W))
  val mepc    = RegInit(0.U(64.W))
  val mip     = RegInit(0.U(16.W))
  val mie     = RegInit(0.U(16.W))
  val mcause  = RegInit(true.B ## 7.U(63.W))
  val mhartid = RegInit(0.U(64.W)) // todo

  val MSTATUS_MIE     = 3
  val MIP_CLINT       = 7

  when(io.wen && io.waddr === io.raddr) {
    // forward
    io.rdata := io.wdata
  }.otherwise {
    io.rdata := LookupTreeDefault(io.raddr, 0.U, List(
      CsrId.mstatus -> mstatus,
      CsrId.mtvec   -> mtvec,
      CsrId.mepc    -> mepc,
      CsrId.mcause  -> mcause,
      CsrId.mie     -> mie,
      CsrId.mip     -> mip
    ))
  }

  when(io.wen) {
    switch(io.waddr) {
      is(CsrId.mstatus) { mstatus := io.wdata }
      is(CsrId.mtvec)   { mtvec   := io.wdata }
      is(CsrId.mepc)    { mepc    := io.wdata }
      is(CsrId.mcause)  { mcause  := io.wdata }
      is(CsrId.mie)     { mie     := io.wdata }
    }
  }

  /*
  io.rdata := Mux(io.wen && io.waddr === io.raddr, io.wdata, csr(io.raddr(2, 0)))

  when(io.wen && (io.waddr(2, 0) =/= 5.U)) {
    csr(io.waddr(2, 0)) := io.wdata
  }

  when(io.exception) {
    csr(2) := io.epc
    csr(3) := ZeroExt(io.no, 64)
  }*/

  // timer int
  val mipVec = VecInit(mip.asBools)
  mipVec(MIP_CLINT)   := io.timerInt
  mip                 := mipVec.asUInt

  // interrupt, just for timer int now
  val int    = RegInit(false.B)
  int       := Mux(io.stall, int, io.timerInt & mie(MIP_CLINT) & mstatus(MSTATUS_MIE))

  //io.int    := io.timerInt & mie(MIP_CLINT) & mstatus(MSTATUS_MIE)
  io.int    := int

  // clear other interrupt，可能有问题，mie不会恢复
  when(int) {
    mie := 0.U
  }
}

/*
package jzcore

import chisel3._
import chisel3.util._
import utils._

class CsrReg extends Module {
  val io = IO(new Bundle {
    // exception
    val exception = Input(Bool())
    val epc       = Input(UInt(64.W))
    val no        = Input(UInt(4.W))

    val raddr     = Input(UInt(3.W))
    val rdata     = Output(UInt(64.W))

    val waddr     = Input(UInt(3.W))
    val wen       = Input(Bool())
    val wdata     = Input(UInt(64.W))
  })

  val csr = RegInit(VecInit(List.fill(4)(0.U(64.W))))

  io.rdata := Mux(io.wen && io.waddr === io.raddr, io.wdata, csr(io.raddr(1, 0)))

  when(io.wen) {
    csr(io.waddr(1, 0)) := io.wdata
  }

  when(io.exception) {
    csr(2) := io.epc
    csr(3) := ZeroExt(io.no, 64)
  }
}*/