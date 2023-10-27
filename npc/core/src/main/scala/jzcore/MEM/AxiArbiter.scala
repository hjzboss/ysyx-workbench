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

    val master0   = Flipped(new AxiMaster)
    val master1   = Flipped(new AxiMaster)
    val master    = new AxiMaster
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

  when(io.grantLsu) {    
    io.master <> io.master1
    io.master0.rvalid := false.B
    io.master0.rdata := 0.U
    io.master0.rlast := true.B
    io.master0.rresp := 0.U
    io.master0.awready := false.B
    io.master0.wready := false.B
    io.master0.bvalid := false.B
    io.master0.bresp := 0.U
    io.master0.bid := 0.U
    io.master0.arready := false.B
    io.master0.rid := 0.U
  }.elsewhen(io.grantIfu) {
    io.master <> io.master0
    io.master1.rvalid := false.B
    io.master1.rdata := 0.U
    io.master1.rlast := true.B
    io.master1.rresp := 0.U
    io.master1.awready := false.B
    io.master1.wready := false.B
    io.master1.bvalid := false.B
    io.master1.bresp := 0.U
    io.master1.bid := 0.U
    io.master1.arready := false.B
    io.master1.rid := 0.U
  }.otherwise {
    io.master0.rvalid := false.B
    io.master0.rdata := 0.U
    io.master0.rlast := true.B
    io.master0.rresp := 0.U
    io.master0.awready := false.B
    io.master0.wready := false.B
    io.master0.bvalid := false.B
    io.master0.bresp := 0.U
    io.master0.bid := 0.U
    io.master0.arready := false.B
    io.master0.rid := 0.U
    io.master1.rvalid := false.B
    io.master1.rdata := 0.U
    io.master1.rlast := true.B
    io.master1.rresp := 0.U
    io.master1.awready := false.B
    io.master1.wready := false.B
    io.master1.bvalid := false.B
    io.master1.bresp := 0.U
    io.master1.bid := 0.U
    io.master1.arready := false.B
    io.master1.rid := 0.U
    io.master.awid := 0.U
    io.master.awvalid := false.B
    io.master.awaddr := 0.U
    io.master.awlen := 0.U
    io.master.awsize := 0.U
    io.master.awburst := 0.U
    io.master.wvalid := false.B
    io.master.wdata := 0.U
    io.master.wstrb := 0.U
    io.master.wlast := false.B
    io.master.bready := false.B
    io.master.arid := 0.U
    io.master.araddr := 0.U
    io.master.arvalid := false.B
    io.master.arlen := 0.U
    io.master.arsize := 0.U
    io.master.arburst := 0.U
    io.master.rready := false.B
  }
}