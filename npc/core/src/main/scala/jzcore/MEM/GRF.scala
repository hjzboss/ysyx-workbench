package jzcore

import chisel3._
import chisel3.util._

class GRF extends Module {
  val io = IO(new Bundle {
    val rs1   = Input(UInt(5.W))
    val rs2   = Input(UInt(5.W))
    val src1  = Output(UInt(64.W))
    val src2  = Output(UInt(64.W))

    val waddr = Input(UInt(5.W))
    val wen   = Input(Bool())
    val wdata = Input(UInt(64.W))
  })

  val rf = RegInit(VecInit(List.fill(32)(0.U(64.W))))

  io.src1 := Mux(io.wen && io.waddr === io.rs1, io.wdata, rf(io.rs1))
  io.src2 := Mux(io.wen && io.waddr === io.rs2, io.wdata, rf(io.rs2))

  when(io.wen && io.waddr =/= 0.U(5.W)) {
    rf(io.waddr) := io.wdata
  }
}

class SimGRF extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())

    val rs1   = Input(UInt(5.W))
    val rs2   = Input(UInt(5.W))
    val src1  = Output(UInt(64.W))
    val src2  = Output(UInt(64.W))

    val waddr = Input(UInt(5.W))
    val wen   = Input(Bool())
    val wdata = Input(UInt(64.W))
  })
}