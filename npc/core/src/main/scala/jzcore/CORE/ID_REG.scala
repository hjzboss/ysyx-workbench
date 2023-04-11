package jzcore

import chisel3._
import chisel3.util._
import utils._

trait HasResetVector {
  val resetVector = Settings.getLong("ResetVector")
}

// ifu_idu的流水线寄存器
class ID_REG extends Module HasResetVector {
  val io = IO(new Bundle {
    val stall = Input(Bool()) // 停顿信号，用于阻塞流水线
    val flush = Input(Bool()) // 清空流水线

    // 传递数据
    val in = Flipped(new InstrFetch)
    val out = new InstrFetch
  })

  // 复位值
  val regReset = Wire(new InstrFetch)
  regReset.pc := resetVector.U(64.W)
  regReset.inst := Instruction.NOP

  // 流水线寄存器
  val idReg = RegInit(regReset)
  idReg := Mux(io.flush, regReset, Mux(io.stall, idReg, io.in))

  io.out := idReg
}