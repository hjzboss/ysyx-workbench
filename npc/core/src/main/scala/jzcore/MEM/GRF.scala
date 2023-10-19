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

  io.src1 := Mux(io.wen && io.waddr === io.rs1, io.wdata, rf(io.rs1))
  io.src2 := Mux(io.wen && io.waddr === io.rs2, io.wdata, rf(io.rs2))

  when(io.wen && io.waddr =/= 0.U(5.W)) {
    rf(io.waddr) := io.wdata
  }

  if(Settings.get("sim")) {
    val dpigrf = Module(new DpiGrf)
    reg0  := rf(0 )
    reg1  := rf(1 )
    reg2  := rf(2 )
    reg3  := rf(3 )
    reg4  := rf(4 )
    reg5  := rf(5 )
    reg6  := rf(6 )
    reg7  := rf(7 )
    reg8  := rf(8 )
    reg9  := rf(9 )
    reg10 := rf(10)
    reg11 := rf(11)
    reg12 := rf(12)
    reg13 := rf(13)
    reg14 := rf(14)
    reg15 := rf(15)
    reg16 := rf(16)
    reg17 := rf(17)
    reg18 := rf(18)
    reg19 := rf(19)
    reg20 := rf(20)
    reg21 := rf(21)
    reg22 := rf(22)
    reg23 := rf(23)
    reg24 := rf(24)
    reg25 := rf(25)
    reg26 := rf(26)
    reg27 := rf(27)
    reg28 := rf(28)
    reg29 := rf(29)
    reg30 := rf(30)
    reg31 := rf(31)
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