package jzcore

import chisel3._
import chisel3.util._
import utils._

class WBU extends Module {
  val io = IO(new Bundle {
    // 来自exu
    val in        = Flipped(new LsuOut)

    // 写回regfile 和 csrfile
    val regWrite  = new RFWriteIO
    val csrWrite  = new CSRWriteIO

    val ready     = Output(Bool()) // 用于仿真环境，提示执行完一条指令
  })

  val stop              = Module(new Stop)

  // 寄存器文件写回
  io.regWrite.rd       := io.in.rd
  io.regWrite.wen      := io.in.regWen
  io.regWrite.value    := Mux(io.in.loadMem, io.in.lsuOut, io.in.exuOut)

  // csr文件写回
  io.csrWrite.waddr    := io.in.csrWaddr
  io.csrWrite.wdata    := io.in.exuOut
  io.csrWrite.wen      := io.in.csrWen
  // exception
  io.csrWrite.exception:= io.in.exception
  io.csrWrite.epc      := io.in.pc
  io.csrWrite.no       := io.in.excepNo

  // ebreak
  stop.io.valid        := io.in.ebreak
  stop.io.haltRet      := io.in.haltRet
}