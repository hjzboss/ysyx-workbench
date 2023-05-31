/*
package jzcore

import chisel3._
import chisel3.util._
import utils._


class CacheStage3 extends Module with HasResetVector {
  val io = IO(new Bundle {
    val validOut        = Output(Bool())
    val debugIn         = Flipped(new DebugIO)
    val debugOut        = new DebugIO

    val toStage3        = Flipped(new Stage3IO)

    val stallOut        = Output(Bool())
    val stallIn         = Input(Bool())

    val flushIn         = Input(Bool())

    // to idu
    val out             = new InstrFetch

    // data array, allocate
    val sram0_cen       = Output(Bool())
    val sram0_wen       = Output(Bool())
    val sram0_wmask     = Output(UInt(128.W))
    val sram0_addr      = Output(UInt(6.W))
    val sram0_wdata     = Output(UInt(128.W)) 

    val sram1_cen       = Output(Bool())
    val sram1_wen       = Output(Bool())
    val sram1_wmask     = Output(UInt(128.W))
    val sram1_addr      = Output(UInt(6.W))
    val sram1_wdata     = Output(UInt(128.W)) 

    val sram2_cen       = Output(Bool())
    val sram2_wen       = Output(Bool())
    val sram2_wmask     = Output(UInt(128.W))
    val sram2_addr      = Output(UInt(6.W))
    val sram2_wdata     = Output(UInt(128.W)) 

    val sram3_cen       = Output(Bool())
    val sram3_wen       = Output(Bool())
    val sram3_wmask     = Output(UInt(128.W))
    val sram3_addr      = Output(UInt(6.W))
    val sram3_wdata     = Output(UInt(128.W)) 

    val metaAlloc       = new MetaAllocIO

    // axi
    val axiRaddrIO      = Decoupled(new RaddrIO)
    val axiRdataIO      = Flipped(Decoupled(new RdataIO))
    // useless
    val axiWaddrIO      = Decoupled(new WaddrIO)
    val axiWdataIO      = Decoupled(new WdataIO)
    val axiBrespIO      = Flipped(Decoupled(new BrespIO))    

    // arbiter
    val axiReq          = Output(Bool())
    val axiGrant        = Input(Bool())
    val axiReady        = Output(Bool())
  })

  val regInit           = Wire(new Stage3IO)
  regInit.pc           := resetVector.U(32.W)
  regInit.cacheline    := 0.U(128.W)
  regInit.hit          := true.B
  regInit.allocAddr    := 0.U(32.W)
  regInit.cacheable    := true.B
  regInit.victim       := 0.U(2.W)
  regInit.align        := 0.U(2.W)
  regInit.tag          := 0.U(22.W)
  regInit.index        := 0.U(6.W)
  val stage3Reg         = RegInit(regInit)
  stage3Reg            := Mux(io.flushIn, regInit, Mux(io.stallOut || io.stallIn, stage3Reg, io.toStage3))

  val debugReset        = Wire(new DebugIO)
  debugReset.pc        := resetVector.U(32.W)
  debugReset.nextPc    := resetVector.U(32.W)
  debugReset.inst      := Instruction.NOP

  val debugReg          = RegInit(debugReset)
  val stallDebug        = dontTouch(Wire(new DebugIO))
  stallDebug           := Mux(io.stallIn || io.stallOut, debugReg, io.debugIn)
  debugReg             := Mux(io.flushIn, debugReset, stallDebug)
  io.debugOut.pc       := debugReg.pc
  io.debugOut.nextPc   := debugReg.nextPc

  val align             = stage3Reg.align

  // axi fire
  val raddrFire         = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire         = io.axiRdataIO.valid && io.axiRdataIO.ready

  // 分配和flash取指状态机
  val idle :: addr :: data :: flush :: stall :: Nil = Enum(5)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle  -> Mux(io.flushIn || io.stallIn, idle, Mux(!stage3Reg.hit || !stage3Reg.cacheable, addr, idle)),
    addr  -> Mux(io.flushIn, flush, Mux(raddrFire && io.axiGrant, data, addr)),
    data  -> Mux(rdataFire && io.axiRdataIO.bits.rlast, Mux(io.stallIn, stall, idle), Mux(io.flushIn, flush, data)),
    stall -> Mux(io.stallIn, stall, idle),
    flush -> Mux(rdataFire && io.axiRdataIO.bits.rlast, Mux(io.stallIn, stall, idle), flush)
  ))

  io.stallOut             := (state === idle && (!stage3Reg.hit || !stage3Reg.cacheable)) || state === addr || (state === data && !io.axiRdataIO.bits.rlast)   

  io.axiReq               := state === addr
  //io.axiReq               := state === idle && (!stage3Reg.hit || !stage3Reg.cacheable) && !io.flushIn
  io.axiReady             := (state === data || state === flush) && rdataFire && io.axiRdataIO.bits.rlast

  // allocate axi, burst read
  io.axiRaddrIO.valid     := state === addr || state === flush
  io.axiRaddrIO.bits.addr := stage3Reg.allocAddr
  io.axiRaddrIO.bits.len  := Mux(stage3Reg.cacheable, 1.U(8.W), 0.U(8.W))
  io.axiRaddrIO.bits.size := Mux(stage3Reg.cacheable, 3.U(3.W), 2.U(3.W))
  io.axiRaddrIO.bits.burst:= Mux(stage3Reg.cacheable, 2.U(2.W), 0.U(2.W))
  io.axiRdataIO.ready     := state === data || state === flush

  val rblockBuffer         = RegInit(0.U(64.W))
  rblockBuffer            := MuxLookup(state, rblockBuffer, List(
                              addr -> 0.U(64.W),
                              data -> Mux(rdataFire && !io.axiRdataIO.bits.rlast, io.axiRdataIO.bits.rdata, rblockBuffer),
                              flush-> 0.U(64.W)
                            ))
  // todo
  io.axiWaddrIO.valid     := false.B
  io.axiWaddrIO.bits.addr := 0.U(32.W)
  io.axiWaddrIO.bits.len  := 0.U
  io.axiWaddrIO.bits.size := 0.U
  io.axiWaddrIO.bits.burst:= 0.U
  io.axiWdataIO.valid     := false.B
  io.axiWdataIO.bits.wlast:= true.B
  io.axiWdataIO.bits.wstrb:= 0.U(8.W)
  io.axiBrespIO.ready     := true.B

  val alignMask0   = Mux(align(1), "hffffffffffffffff".U(128.W), ~"hffffffffffffffff".U(128.W))
  val alignMask1   = Mux(align(1), ~"hffffffffffffffff".U(128.W), "hffffffffffffffff".U(128.W))
  val rdata0       = Mux(align(1), Cat(io.axiRdataIO.bits.rdata, 0.U(64.W)), Cat(0.U(64.W), io.axiRdataIO.bits.rdata))
  val rdata1       = Mux(align(1), Cat(0.U(64.W), io.axiRdataIO.bits.rdata), Cat(io.axiRdataIO.bits.rdata, 0.U(64.W)))

  // dataArray control, todo: 此时发生冲刷仍会写入cache
  io.sram0_addr   := stage3Reg.index
  io.sram0_wen    := true.B
  io.sram1_addr   := stage3Reg.index
  io.sram1_wen    := true.B
  io.sram2_addr   := stage3Reg.index
  io.sram2_wen    := true.B
  io.sram3_addr   := stage3Reg.index
  io.sram3_wen    := true.B
  io.sram0_cen    := true.B
  io.sram1_cen    := true.B
  io.sram2_cen    := true.B
  io.sram3_cen    := true.B
  io.sram0_wdata  := 0.U
  io.sram0_wmask  := ~0.U(128.W)
  io.sram1_wdata  := 0.U
  io.sram1_wmask  := ~0.U(128.W)
  io.sram2_wdata  := 0.U
  io.sram2_wmask  := ~0.U(128.W)
  io.sram3_wdata  := 0.U
  io.sram3_wmask  := ~0.U(128.W)
  when(state === data && rdataFire && stage3Reg.cacheable) {
    // allocate dataArray
    switch(stage3Reg.victim) {
      is(0.U) {
        io.sram0_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram0_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram0_cen    := false.B
        io.sram0_wen    := false.B
      }
      is(1.U) {
        io.sram1_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram1_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram1_cen    := false.B
        io.sram1_wen    := false.B
      }
      is(2.U) {
        io.sram2_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram2_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram2_cen    := false.B
        io.sram2_wen    := false.B
      }
      is(3.U) {
        io.sram3_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram3_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram3_cen    := false.B
        io.sram3_wen    := false.B
      }
    }
  }

  // meta array alloc
  io.metaAlloc.tag    := stage3Reg.tag
  io.metaAlloc.index  := stage3Reg.index
  io.metaAlloc.victim := stage3Reg.victim
  io.metaAlloc.valid  := state === data && rdataFire && io.axiRdataIO.bits.rlast && stage3Reg.cacheable

  val inst             = WireDefault(Instruction.NOP)
  // -----------------------data aligner-------------------------------
  when(state === idle && stage3Reg.hit && stage3Reg.cacheable) {
    when(align(1)) {
      inst := Mux(stage3Reg.align(0), stage3Reg.cacheline(127, 96), stage3Reg.cacheline(95, 64))
    }.otherwise {
      inst := Mux(stage3Reg.align(0), stage3Reg.cacheline(63, 32), stage3Reg.cacheline(31, 0))
    }
  }.elsewhen((state === data && rdataFire && io.axiRdataIO.bits.rlast) || state === stall) {
    when(stage3Reg.cacheable) {
      inst := Mux(stage3Reg.align(0), rblockBuffer(63, 32), rblockBuffer(31, 0))
    }.otherwise {
      inst := io.axiRdataIO.bits.rdata(31, 0)
    }
  }.otherwise {
    inst := Instruction.NOP
  }
  io.out.inst := inst
  io.out.pc   := stage3Reg.pc
  
  io.validOut := !io.flushIn && (state === idle && stage3Reg.hit && stage3Reg.cacheable) || ((state === data || state === flush) && rdataFire && io.axiRdataIO.bits.rlast) || state === stall
  io.debugOut.inst := inst
}
*/