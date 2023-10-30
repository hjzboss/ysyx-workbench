package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

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



  if(Settings.get("core") == "single") {
    io.src1 := rf(io.rs1)
    io.src2 := rf(io.rs2)
  } else {
    io.src1 := Mux(io.wen && io.waddr === io.rs1 && io.rs1 =/= 0.U, io.wdata, rf(io.rs1))
    io.src2 := Mux(io.wen && io.waddr === io.rs2 && io.rs2 =/= 0.U, io.wdata, rf(io.rs2))
  }

  when(io.wen && io.waddr =/= 0.U(5.W)) {
    rf(io.waddr) := io.wdata
  }

  if(Settings.get("sim")) {
    val dpigrf = Module(new DpiGrf)
    dpigrf.io.reg0  := rf(0 )
    dpigrf.io.reg1  := rf(1 )
    dpigrf.io.reg2  := rf(2 )
    dpigrf.io.reg3  := rf(3 )
    dpigrf.io.reg4  := rf(4 )
    dpigrf.io.reg5  := rf(5 )
    dpigrf.io.reg6  := rf(6 )
    dpigrf.io.reg7  := rf(7 )
    dpigrf.io.reg8  := rf(8 )
    dpigrf.io.reg9  := rf(9 )
    dpigrf.io.reg10 := rf(10)
    dpigrf.io.reg11 := rf(11)
    dpigrf.io.reg12 := rf(12)
    dpigrf.io.reg13 := rf(13)
    dpigrf.io.reg14 := rf(14)
    dpigrf.io.reg15 := rf(15)
    dpigrf.io.reg16 := rf(16)
    dpigrf.io.reg17 := rf(17)
    dpigrf.io.reg18 := rf(18)
    dpigrf.io.reg19 := rf(19)
    dpigrf.io.reg20 := rf(20)
    dpigrf.io.reg21 := rf(21)
    dpigrf.io.reg22 := rf(22)
    dpigrf.io.reg23 := rf(23)
    dpigrf.io.reg24 := rf(24)
    dpigrf.io.reg25 := rf(25)
    dpigrf.io.reg26 := rf(26)
    dpigrf.io.reg27 := rf(27)
    dpigrf.io.reg28 := rf(28)
    dpigrf.io.reg29 := rf(29)
    dpigrf.io.reg30 := rf(30)
    dpigrf.io.reg31 := rf(31)
  }
}

/*
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
}*/