package jzcore

import chisel3._
import top.Settings
import chisel3.util._
import chisel3.util.experimental.BoringUtils

// 性能分析模块
class Perf extends Module with HasResetVector {
  val io = IO(new Bundle {
    val perfIO = new PerfIO
  })

  // icache perf
  val icacheHit = Wire(Bool())
  val icacheReq = Wire(Bool())
  val icacheHitCnt = RegInit(0.U(64.W))
  val icacheReqCnt = RegInit(0.U(64.W))
  icacheHitCnt := Mux(icacheHit, icacheHitCnt + 1.U, icacheHitCnt)
  icacheReqCnt := Mux(icacheReq, icacheReqCnt + 1.U, icacheReqCnt)

  // dcache perf
  val dcacheHit = Wire(Bool())
  val dcacheReq = Wire(Bool())
  val dcacheHitCnt = RegInit(0.U(64.W))
  val dcacheReqCnt = RegInit(0.U(64.W))
  dcacheHitCnt := Mux(dcacheHit, dcacheHitCnt + 1.U, dcacheHitCnt)
  dcacheReqCnt := Mux(dcacheReq, dcacheReqCnt + 1.U, dcacheReqCnt)

  BoringUtils.addSink(icacheHit, "icacheHit")
  BoringUtils.addSink(icacheReq, "icacheReq")
  BoringUtils.addSink(dcacheHit, "dcacheHit")
  BoringUtils.addSink(dcacheReq, "dcacheReq")

  // bpu
  val bpuMiss = Wire(Bool())
  val bpuReq = Wire(Bool())
  val bpuMissCnt = RegInit(0.U(64.W))
  val bpuReqCnt = RegInit(0.U(64.W))
  bpuMissCnt := Mux(bpuMiss, bpuMissCnt + 1.U, bpuMissCnt)
  bpuReqCnt := Mux(bpuReq, bpuReqCnt + 1.U, bpuReqCnt)

  BoringUtils.addSink(bpuMiss, "bpuMiss")
  BoringUtils.addSink(bpuReq, "bpuReq")

  io.perfIO.icacheHitCnt  := icacheHitCnt
  io.perfIO.icacheReqCnt  := icacheReqCnt
  io.perfIO.dcacheHitCnt  := dcacheHitCnt
  io.perfIO.dcacheReqCnt  := dcacheReqCnt
  io.perfIO.bpuMissCnt    := bpuMissCnt
  io.perfIO.bpuReqCnt     := bpuReqCnt
}