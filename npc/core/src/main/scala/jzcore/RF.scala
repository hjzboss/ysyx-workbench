package jzcore

import chisel3._
import chisel3.util._

class RF extends Module {
  val io = IO(new Bundle {
    val read    = Flipped(new RFReadIO)
    val write   = Flipped(new RFWriteIO)
    val src1    = Output(UInt(64.W))
    val src2    = Output(UInt(64.W))
  })

  val registerFile = RegInit(VecInit(Seq.fill(32)(0.U(64.W))))

  // read
  val src1_pre = registerFile(io.read.rs1)
  val src2_pre = registerFile(io.read.rs2)
  io.src1 := Mux(io.read.ren1, src1_pre, 0.U(64.W))
  io.src2 := Mux(io.read.ren2, src2_pre, 0.U(64.W))
  
  //write
  val current = registerFile(io.write.rd)
  registerFile(io.write.rd) := Mux(io.write.wen && io.write.rd =/= 0.U(5.W), io.write.value, current)
}