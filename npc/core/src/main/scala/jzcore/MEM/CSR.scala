package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

// dpi-c csrreg
class CsrReg extends BlackBox {
  val io = IO(new Bundle {
    val mstatus = Input(UInt(64.W))
    val mtvec   = Input(UInt(64.W))
    val mepc    = Input(UInt(64.W))
    val mcause  = Input(UInt(64.W))
  })
}


class CSR extends Module {
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
  })

  val MSTATUS_INIT = if(Settings.get("sim")) "ha00001800".U(64.W) else 0.U(64.W)

  val mstatus = RegInit(MSTATUS_INIT)
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

  // timer int
  val mipVec = VecInit(mip.asBools)
  mipVec(7) := io.timerInt
  mip       := mipVec.asUInt

  // interrupt, just for timer int now
  io.int    := io.timerInt & mie(MIP_CLINT) & mstatus(MSTATUS_MIE)

  // interrupt
  when(io.mret) {
    // mret指令会导致mie更新为mpie,mpie更新为1
    val mpie = mstatus(MSTATUS_MPIE)
    mstatus := mstatus(63, MSTATUS_MPIE+1) ## true.B ## mstatus(MSTATUS_MPIE-1, MSTATUS_MIE+1) ## mpie ## mstatus(MSTATUS_MIE-1, 0)
  }.elsewhen(io.exception) {
    // 将mstatus的mie字段保存到mpie，mie字段设置为0
    val mstatusMie = mstatus(MSTATUS_MIE)
    mstatus := mstatus(63, MSTATUS_MPIE+1) ## mstatusMie ## mstatus(MSTATUS_MPIE-1, MSTATUS_MIE+1) ## false.B ## mstatus(MSTATUS_MIE-1, 0)
  }

  if(Settings.get("sim")) {
    val csrReg = Module(new CsrReg)
    // csr0: mstatus, csr1: mtvec, csr2: mepc, csr3: mcause
    csrReg.io.mstatus := mstatus
    csrReg.io.mtvec   := ZeroExt(mtvec, 64)
    csrReg.io.mepc    := ZeroExt(mepc, 64)
    csrReg.io.mcause  := mcause
  }
}