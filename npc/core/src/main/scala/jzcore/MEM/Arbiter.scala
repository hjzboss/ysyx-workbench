package jzcore

import chisel3._
import chisel3.util._
import utils._

/**
  * axi请求仲裁器，仲裁策略是lsu优先
  */

class AxiArbiter extends Module {
  val io = IO(new Bundle {
    // ifu仲裁信号
    val ifuReq    = Input(Bool())
    val ifuReady  = Input(Bool())
    val grantIfu  = Output(Bool())

    // lsu仲裁信号
    val lsuReq    = Input(Bool())
    val lsuReady  = Input(Bool())
    val grantLsu  = Output(Bool())
  })

  val req = io.ifuReq || io.lsuReq
  val ready = io.ifuReady || io.lsuReady

  val idle :: grant :: Nil = Enum(2)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle    -> Mux(req, grant, idle),
    grant   -> Mux(!ready, grant, idle)
  ))

  // 保存请求状态，用于state为grant状态时的判断
  // lsu的请求优先
  val ifuReq = RegInit(false.B)
  val lsuReq = RegInit(false.B)
  ifuReq := Mux(state === idle && && !io.lsuReq, io.ifuReq, true.B, false.B)
  lsuReq := Mux(state === idle && io.lsuReq, true.B, false.B)

  // 仲裁
  val grantIfu = (state === idle && && !io.lsuReq && io.ifuReq) || (state === grant && ifuReq)
  val grantLsu = (state === idle && io.lsuReq) || (state === grant && lsuReq)
  io.grantIfu := grantIfu
  io.grantLsu := grantLsu
}