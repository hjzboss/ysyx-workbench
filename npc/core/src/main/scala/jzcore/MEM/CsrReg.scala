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
    // exception
    val exception = Input(Bool())
    val epc       = Input(UInt(64.W))
    val no        = Input(UInt(4.W))

    val raddr     = Input(UInt(3.W))
    val rdata     = Output(UInt(64.W))

    val waddr     = Input(UInt(3.W))
    val wen       = Input(Bool())
    val wdata     = Input(UInt(64.W))

    val timerInt  = Input(Bool()) // clint interrupt
  })

  // csr0: mstatus, csr1: mtvec, csr2: mepc, csr3: mcause
  //val csr = RegInit(VecInit(List.fill(4)(0.U(64.W))))

  val mstatus = RegInit(0.U(64.W))
  val mtvec   = RegInit(0.U(64.W))
  val mepc    = RegInit(0.U(64.W))
  val mip     = RegInit(0.U(16.W))
  val mie     = RegInit(0.U(16.W))
  val mcause  = RegInit(0.U(64.W))

  when(io.wen && io.waddr === io.raddr) {
    // forward
    io.rdata := io.wdata
  }.otherwise {
    io.rdata := LookupTreeDefault(io.raddr, 0.U, List(
      "h300".U -> mstatus,
      "h305".U -> mtvec,
      "h341".U -> mepc,
      "h342".U -> mcause,
      "h304".U -> mie,
      "h344".U -> mip
    ))
  }

  when(io.wen) {
    switch(io.waddr) {
      is("h300".U) { mstatus := io.wdata }
      is("h305".U) { mtvec   := io.wdata }
      is("h341".U) { mepc    := io.wdata }
      is("h342".U) { mcause  := io.wdata }
      is("h304".U) { mie     := io.wdata }
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
  mipVec(7) := io.timerInt
  mip       := mipVec.asUInt
}