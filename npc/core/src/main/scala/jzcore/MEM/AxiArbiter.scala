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

    val master0   = new AxiArbiter
    val master1   = new AxiArbiter
    val master    = new AxiArbiter
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

  val defaultMaster = Wire(new AxiArbiter)
  defaultMaster.awid := 0.U
  defaultMaster.awvalid := false.B
  defaultMaster.awaddr := 0.U
  defaultMaster.awlen := 0.U
  defaultMaster.awsize := 0.U
  defaultMaster.awburst := 0.U
  defaultMaster.wvalid := false.B
  defaultMaster.wdata := 0.U
  defaultMaster.wstrb := 0.U
  defaultMaster.wlast := false.B
  defaultMaster.bready := false.B
  defaultMaster.arid := 0.U
  defaultMaster.araddr := 0.U
  defaultMaster.arvalid := false.B
  defaultMaster.arlen := 0.U
  defaultMaster.arsize := 0.U
  defaultMaster.arburst := 0.U
  defaultMaster.rready := false.B

  when(io.grantLsu) {
    io.master <> io.master1
  }.elsewhen(io.grantIfu) {
    io.master <> io.master0
  }.otherwise {
    io.master <> defaultMaster
  }
}