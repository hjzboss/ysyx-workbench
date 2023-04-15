package jzcore

import chisel3._
import chisel3.util._
import utils._


class LSU extends Module {
  val io = IO(new Bundle {
    // exu传入
    val in          = Flipped(new ExuOut)

    // 传给wbu
    val out         = new LsuOut

    // 送给ctrl模块，用于停顿
    val ready       = Output(Bool())
    //val lsuTrans    = Output(Bool())
    //val stall       = Input(Bool())

    // axi总线访存接口
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))
    
    // 仲裁信号
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())
  })

  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire          = io.axiRdataIO.valid && io.axiRdataIO.ready
  val waddrFire          = io.axiWaddrIO.valid && io.axiWaddrIO.ready
  val wdataFire          = io.axiWdataIO.valid && io.axiWdataIO.ready
  val brespFire          = io.axiBrespIO.valid && io.axiBrespIO.ready

  val addr               = io.in.lsuAddr

  // load状态机
  val idle :: wait_data :: wait_resp ::Nil = Enum(3)
  val rState = RegInit(idle)
  rState := MuxLookup(rState, idle, List(
    idle        -> Mux(raddrFire && io.axiGrant, wait_data, idle),
    wait_data   -> Mux(rdataFire, idle, wait_data)
  ))

  val readTrans          = io.in.lsuRen
  val rresp              = io.axiRdataIO.bits.rresp // todo
  val bresp              = io.axiBrespIO.bits.bresp

  io.axiRaddrIO.valid      := rState === idle && io.in.lsuRen
  io.axiRaddrIO.bits.addr  := addr
  io.axiRdataIO.ready      := rState === wait_data

  // store状态机
  val wState = RegInit(idle)
  wState := MuxLookup(wState, idle, List(
    idle        -> Mux(waddrFire && wdataFire && io.axiGrant, wait_resp, idle),
    wait_resp   -> Mux(brespFire, idle, wait_resp)
  ))

  io.axiWaddrIO.valid      := wState === idle && io.in.lsuWen
  io.axiWaddrIO.bits.addr  := addr
  io.axiWdataIO.valid      := wState === idle && io.in.lsuWen
  io.axiWdataIO.bits.wdata := io.in.lsuWdata
  io.axiWdataIO.bits.wstrb := io.in.wmask << addr(2, 0) // todo
  io.axiBrespIO.ready      := wState === wait_resp

/*  
  val lsTypeReg          = RegInit(LsType.nop)
  lsTypeReg             := Mux(readTrans && io.axiGrant, io.in.lsType, lsTypeReg)
*/

  // 数据对齐
  val align              = Cat(addr(2, 0), 0.U(3.W)) // 此处变成了0，原因未知
  //val alignReg           = RegInit(0.U(6.W))
  //alignReg              := Mux(readTrans && io.axiGrant, align, alignReg)
  val rdata              = io.axiRdataIO.bits.rdata >> align
  val lsuOut             = LookupTree(io.in.lsType, Seq(
                            LsType.ld   -> rdata,
                            LsType.lw   -> SignExt(rdata(31, 0), 64),
                            LsType.lh   -> SignExt(rdata(15, 0), 64),
                            LsType.lb   -> SignExt(rdata(7, 0), 64),
                            LsType.lbu  -> ZeroExt(rdata(7, 0), 64),
                            LsType.lhu  -> ZeroExt(rdata(15, 0), 64),
                            LsType.lwu  -> ZeroExt(rdata(31, 0), 64),
                            LsType.sd   -> rdata,
                            LsType.sw   -> rdata,
                            LsType.sh   -> rdata,
                            LsType.sb   -> rdata,
                            LsType.nop  -> rdata
                          ))

  val writeTrans         = io.in.lsuWen
  val hasTrans           = (readTrans || writeTrans) && io.axiGrant

/*
  // 当lsu访存未结束时锁存控制信号,todo
  val exuOutreg          = RegInit(0.U(64.W))
  val loadMemreg         = RegInit(false.B)
  val rdreg              = RegInit(0.U(5.W))
  val regWenreg          = RegInit(false.B)
  val exceptionreg       = RegInit(false.B)
  val csrWaddrreg        = RegInit(0.U(2.W))
  val csrWenreg          = RegInit(false.B)
  val noreg              = RegInit(0.U(4.W))
  exuOutreg             := Mux(hasTrans, io.in.exuOut, exuOutreg)
  loadMemreg            := Mux(hasTrans, io.in.loadMem, loadMemreg)
  rdreg                 := Mux(hasTrans, io.in.rd, rdreg)
  regWenreg             := Mux(hasTrans, io.in.regWen, regWenreg)
  exceptionreg          := Mux(hasTrans, io.in.exception, exceptionreg)
  csrWaddrreg           := Mux(hasTrans, io.in.csrWaddr, csrWaddrreg)
  csrWenreg             := Mux(hasTrans, io.in.csrWen, csrWenreg)
  noreg                 := Mux(hasTrans, io.in.no, noreg)
*/

  io.out.lsuOut         := lsuOut
  io.out.loadMem        := io.in.loadMem
  io.out.exuOut         := io.in.exuOut
  io.out.rd             := io.in.rd
  //io.out.regWen         := Mux(!io.ready, false.B, Mux(rState === wait_data || wState === wait_resp, regWenreg, io.in.regWen))
  io.out.regWen         := io.in.regWen
  io.out.pc             := io.in.pc
  io.out.excepNo        := io.in.excepNo
  io.out.exception      := io.in.exception
  //io.out.no             := Mux(rState === idle && wState === idle, io.in.no, noreg)
  //io.out.exception      := Mux(!io.ready, false.B, Mux(rState === wait_data || wState === wait_resp, exceptionreg, io.in.exception))
  //io.out.csrWaddr       := Mux(rState === idle && wState === idle, io.in.csrWaddr, csrWaddrreg)
  //io.out.csrWen         := Mux(!io.ready, false.B, Mux(rState === wait_data || wState === wait_resp, csrWenreg, io.in.csrWen))
  io.out.csrWaddr       := io.in.csrWaddr
  io.out.csrWen         := io.in.csrWen
  io.out.ebreak         := io.in.ebreak
  io.out.haltRet        := io.in.haltRet
  io.out.csrValue       := io.in.csrValue

  //io.lsuTrans           := hasTrans || rState === wait_data || wState === wait_resp
  io.ready              := !(readTrans || writeTrans) || ((rState === wait_data && rdataFire) || (wState === wait_resp && brespFire)) && (rresp === okay || bresp === okay)

  // 仲裁信号
  io.axiReq             := (rState === idle && io.in.lsuRen) || (wState === idle && io.in.lsuWen)
  io.axiReady           := (rState === wait_data && rdataFire) || (brespFire && wState === wait_resp)
}