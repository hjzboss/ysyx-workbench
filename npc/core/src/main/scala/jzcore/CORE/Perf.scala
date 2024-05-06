package jzcore

import chisel3._
import top.Settings
import chisel3.util._
import chisel3.util.experimental.BoringUtils

// 性能分析模块
class Perf extends Module with HasResetVector {
  val io = IO(new Bundle {
    val perf = new PerfIO
  })

  // icache perf
  val icacheHit = Wire(Bool())
  val icacheReq = Wire(Bool())
  val icacheHitCnt = RegEnable(icacheHitCnt + 1.U, 0.U(64.W), icacheHit)
  val icacheReqCnt = RegEnable(icacheReqCnt + 1.U, 0.U(64.W), icacheReq)

  // dcache perf
  val dcacheHit = Wire(Bool())
  val dcacheReq = Wire(Bool())
  val dcacheHitCnt = RegEnable(dcacheHitCnt + 1.U, 0.U(64.W), dcacheHit)
  val dcacheReqCnt = RegEnable(dcacheReqCnt + 1.U, 0.U(64.W), dcacheReq)

  BoringUtils.addSink(icacheHit, "icacheHit")
  BoringUtils.addSink(icacheReq, "icacheReq")
  BoringUtils.addSink(dcacheHit, "dcacheHit")
  BoringUtils.addSink(dcacheReq, "dcacheReq")

  // bpu
  val bpuMiss = Wire(Bool())
  val bpuReq = Wire(Bool())
  val bpuMissCnt = RegEnable(bpuMissCnt + 1.U, 0.U(64.W), bpuMiss)
  val bpuReqCnt = RegEnable(bpuReqCnt + 1.U, 0.U(64.W), bpuReq)

  BoringUtils.addSink(bpuMiss, "bpuMiss")
  BoringUtils.addSink(bpuReq, "bpuReq")

  io.perfIO.icacheHitCnt  := icacheHitCnt
  io.perfIO.icacheReqCnt  := icacheReqCnt
  io.perfIO.dcacheHitCnt  := dcacheHitCnt
  io.perfIO.dcacheReqCnt  := dcacheReqCnt
  io.perfIO.bpuMissCnt    := bpuMissCnt
  io.perfIO.bpuReqCnt     := bpuReqCnt
}