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
}