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
  val ifuReq = io.ifuReq && !io.lsuReq
  val lsuReq = io.lsuReq

  val ifuReqReg = RegInit(false.B)
  val lsuReqReg = RegInit(false.B)
  ifuReqReg := Mux(state === idle, ifuReq, ifuReqReg)
  lsuReqReg := Mux(state === idle, lsuReq, lsuReqReg)

  // 仲裁
  io.grantIfu := (state === grant && ifuReqReg) || (state === idle && ifuReq)
  io.grantLsu := (state === grant && lsuReqReg) || (state === idle && lsuReq)
}