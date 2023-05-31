package jzcore

import chisel3._
import chisel3.util._
import utils._


class CacheStage2 extends Module with HasResetVector {
  val io = IO(new Bundle {
    // debug
    val debugIn         = Flipped(new DebugIO)
    val debugOut        = new DebugIO

    // cache
    val toStage2        = Flipped(new Stage2IO)
    val toStage3        = new Stage3IO

    val flushIn         = Input(Bool())
    val stallOut        = Output(Bool())
    val stallIn         = Input(Bool())

    val sram0_rdata     = Input(UInt(128.W))
    val sram1_rdata     = Input(UInt(128.W))
    val sram2_rdata     = Input(UInt(128.W))
    val sram3_rdata     = Input(UInt(128.W))

    val metaAlloc       = Flipped(new MetaAllocIO)
  })

  val flush              = WireDefault(false.B)

  // pipline reg
  val regInit            = Wire(new CacheStage2)
  regInit.index         := 0.U(6.W)
  regInit.tag           := 0.U(22.W)
  regInit.align         := false.B
  regInit.cacheable     := true.B
  regInit.pc            := resetVector.U(32.W)
  val stage2Reg          = RegInit(regInit)
  stage2Reg             := Mux(flush || io.flushIn, regInit, Mux(io.stallIn, stage2Reg, io.toStage2))

  val debugReset         = Wire(new DebugIO)
  debugReset.pc         := resetVector.U(32.W)
  debugReset.nextPc     := resetVector.U(32.W)
  debugReset.inst       := Instruction.NOP

  val debugReg           = RegInit(debugReset)
  val stallDebug         = dontTouch(Wire(new DebugIO))
  stallDebug            := Mux(io.stallIn, debugReg, io.debugIn)
  debugReg              := Mux(io.flushIn || flush, debugReset, stallDebug)
  io.debugOut           := debugReg

  // random replace count
  val randCount          = RegInit(0.U(2.W))
  randCount             := randCount + 1.U(2.W)

  // meta array
  val metaInit           = Wire(new MetaData)
  metaInit.valid        := false.B
  metaInit.tag          := 0.U(22.W)
  val metaArray          = List.fill(4)(RegInit(VecInit(Seq.fill(64)(metaInit))))

  // metaArray lookup
  val hitList    = dontTouch(VecInit(List.fill(4)(false.B)))
  (0 to 3).map(i => (hitList(i) := metaArray(i)(stage2Reg.index).valid && (metaArray(i)(stage2Reg.index).tag === stage2Reg.tag)))
  val hit = (hitList.asUInt).orR

  // metaArray alloc
  when(io.metaAlloc.valid) {
    // allocate metaArray
    val meta = Wire(new MetaData)
    meta.tag := io.metaAlloc.tag
    meta.valid := true.B
    meta.dirty := false.B
    switch(io.metaAlloc.victim) {
      is(0.U) { metaArray(0)(io.metaArray.index) := meta }
      is(1.U) { metaArray(1)(io.metaArray.index) := meta }
      is(2.U) { metaArray(2)(io.metaArray.index) := meta }
      is(3.U) { metaArray(3)(io.metaArray.index) := meta }
    }
  }

  io.toStage3.cacheline := MuxLookup(hit, 0.U(128.W), List(
                            0.U -> io.sram0_rdata,
                            2.U -> io.sram1_rdata,
                            4.U -> io.sram2_rdata,
                            8.U -> io.sram3_rdata,
                          ))
  
  flush                 := !hit // 当未命中时冲刷流水线寄存器, 停顿ifu
  io.stallOut           := !hit
  io.toStage3.hit       := Mux(io.flushIn || flush, true.B, hit)
  io.toStage3.allocAddr := stage2Reg.tag ## stage2Reg.index ## stage2Reg.align(1) ## 0.U(3.W)
  io.toStage3.victim    := randCount
  io.toStage3.cacheable := stage2Reg.cacheable
  io.toStage3.align     := stage2Reg.align
  io.toStage3.pc        := stage2Reg.pc
  io.toStage3.index     := stage2Reg.index
  io.toStage3.tag       := stage2Reg.tag
}