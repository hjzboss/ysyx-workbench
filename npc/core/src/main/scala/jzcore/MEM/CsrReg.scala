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
    val epc       = Input(UInt(32.W))
    val no        = Input(UInt(64.W))

    val raddr     = Input(UInt(12.W))
    val rdata     = Output(UInt(64.W))

    val waddr     = Input(UInt(12.W))
    val wen       = Input(Bool())
    val wdata     = Input(UInt(64.W))

    // mret
    val mret      = Input(Bool()) 

    // clint interrupt
    val timerInt  = Input(Bool())

    // interrupt
    val int       = Output(Bool())
    val intResp   = Input(Bool())
  })

  // csr0: mstatus, csr1: mtvec, csr2: mepc, csr3: mcause
  //val csr = RegInit(VecInit(List.fill(4)(0.U(64.W))))

  val mstatus = RegInit(0.U(64.W))
  val mtvec   = RegInit(0.U(32.W))
  val mepc    = RegInit(0.U(32.W))
  val mip     = RegInit(0.U(16.W))
  val mie     = RegInit(0.U(16.W))
  val mcause  = RegInit(0.U(64.W))
  //val mhartid = RegInit(0.U(64.W)) // todo

  val MSTATUS_MIE     = 3
  val MSTATUS_MPIE    = 7
  val MIP_CLINT       = 7

  when(io.wen && io.waddr === io.raddr) {
    // forward
    io.rdata := io.wdata
  }.otherwise {
    io.rdata := LookupTreeDefault(io.raddr, 0.U, List(
      CsrId.mstatus -> mstatus,
      CsrId.mtvec   -> ZeroExt(mtvec, 64),
      CsrId.mepc    -> ZeroExt(mepc, 64),
      CsrId.mcause  -> mcause,
      CsrId.mie     -> ZeroExt(mie, 64),
      CsrId.mip     -> ZeroExt(mip, 64)
    ))
  }

  when(io.wen) {
    switch(io.waddr) {
      is(CsrId.mstatus) { mstatus := io.wdata }
      is(CsrId.mtvec)   { mtvec   := io.wdata(31, 0) }
      is(CsrId.mepc)    { mepc    := io.wdata(31, 0) }
      is(CsrId.mcause)  { mcause  := io.wdata }
      is(CsrId.mie)     { mie     := io.wdata(15, 0) }
    }
  }

  when(io.exception) {
    mepc := io.epc
    mcause := io.no
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

  // interrupt, just for timer int now
  //io.int    := io.timerInt & mie(MIP_CLINT) & mstatus(MSTATUS_MIE)
  io.int := false.B

  // interrupt
  when(io.mret) {
    // mret指令会导致mie更新为mpie,mpie更新为1
    val mpie = mstatus(MSTATUS_MPIE)
    mstatus := mstatus(63, MSTATUS_MPIE+1) ## true.B ## mstatus(MSTATUS_MPIE-1, MSTATUS_MIE+1) ## mpie ## mstatus(MSTATUS_MIE-1, 0)
  }.elsewhen(io.int && io.intResp) {
    // 将mstatus的mie字段保存到mpie，mie字段设置为0
    val mstatusMie = mstatus(MSTATUS_MIE)
    mstatus := mstatus(63, MSTATUS_MPIE+1) ## mstatusMie ## mstatus(MSTATUS_MPIE-1, MSTATUS_MIE+1) ## false.B ## mstatus(MSTATUS_MIE-1, 0)
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