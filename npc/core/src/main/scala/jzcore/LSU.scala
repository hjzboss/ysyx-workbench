package jzcore

import chisel3._
import chisel3.util._
import utils._


class LSU extends Module {
  val io = IO(new Bundle {
    // exu传入
    val in        = Flipped(new MemCtrl)

    // 传给wbu
    val out       = new LsuOut

    // 送给ctrl模块，用于停顿
    val lsuReady  = Output(Bool())
    val lsuTrans  = Output(Bool())

    val stall     = Input(Bool())

    // axi总线访存接口
    val raddrIO   = Decoupled(new RaddrIO)
    val rdataIO   = Flipped(Decoupled(new RdataIO))

    val waddrIO   = Decoupled(new WaddrIO)
    val wdataIO   = Decoupled(new WdataIO)
    val brespIO   = Flipped(Decoupled(new BrespIO))

    val align     = Output(UInt(6.W))
  })

  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.raddrIO.valid && io.raddrIO.ready
  val rdataFire          = io.rdataIO.valid && io.rdataIO.ready
  val waddrFire          = io.waddrIO.valid && io.waddrIO.ready
  val wdataFire          = io.wdataIO.valid && io.wdataIO.ready
  val brespFire          = io.brespIO.valid && io.brespIO.ready

  val addr               = io.in.addr

  // load状态机
  val idle :: wait_data :: wait_resp ::Nil = Enum(3)
  val rState = RegInit(idle)
  rState := MuxLookup(rState, idle, List(
    idle        -> Mux(raddrFire, wait_data, idle),
    wait_data   -> Mux(rdataFire, idle, wait_data)
  ))

  val readTrans          = rState === idle && io.in.ren
  val rresp              = io.rdataIO.bits.rresp // todo
  val bresp              = io.brespIO.bits.bresp

  io.raddrIO.valid      := rState === idle && !io.stall && io.in.ren
  io.raddrIO.bits.addr  := addr
  io.rdataIO.ready      := rState === wait_data

  // store状态机
  val wState = RegInit(idle)
  wState := MuxLookup(wState, idle, List(
    idle        -> Mux(waddrFire && wdataFire, wait_resp, idle),
    wait_resp   -> Mux(brespFire, idle, wait_resp)
  ))

  io.waddrIO.valid      := wState === idle && !io.stall && io.in.wen
  io.waddrIO.bits.addr  := addr
  io.wdataIO.valid      := wState === idle && !io.stall && io.in.wen
  io.wdataIO.bits.wdata := io.in.wdata
  io.wdataIO.bits.wstrb := io.in.wmask << addr(2, 0) // todo
  io.brespIO.ready      := wState === wait_resp
  
  val lsTypeReg          = RegInit(LsType.nop)
  lsTypeReg             := Mux(readTrans, io.in.lsType, lsTypeReg)

  // 数据对齐,todo,此处有问题
  //val align              = Cat(addr(2, 0), 0.U(3.W)) // 此处变成了0，原因未知
  val align              = addr(2, 0) // 此处变成了0，原因未知
  val alignReg           = RegInit(0.U(5.W))
  alignReg              := Mux(readTrans, align, alignReg)
  io.align              := alignReg

  val rdata              = io.rdataIO.bits.rdata >> alignReg
  val lsuOut             = LookupTree(lsTypeReg, Seq(
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

  val writeTrans         = wState === idle && io.in.wen
  val hasTrans           = readTrans || writeTrans

  // 当lsu访存未结束时锁存控制信号
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

  io.out.lsuOut         := lsuOut
  io.out.loadMem        := Mux(rState === idle && wState === idle, io.in.loadMem, loadMemreg)
  io.out.exuOut         := Mux(rState === idle && wState === idle, io.in.exuOut, exuOutreg)
  io.out.rd             := Mux(rState === idle && wState === idle, io.in.rd, rdreg)
  //io.out.regWen         := Mux(rState === idle && !io.in.ren && wState === idle && !io.in.wen, io.in.regWen, Mux(io.lsuReady && !io.stall, regWenreg, false.B))
  io.out.regWen         := Mux(!io.lsuReady || io.stall, false.B, Mux(rState === wait_data || wState === wait_resp, regWenreg, io.in.regWen))
  io.out.pc             := io.in.pc
  io.out.no             := Mux(rState === idle && wState === idle, io.in.no, noreg)
  //io.out.exception      := Mux(rState === idle && wState === idle && !io.in.wen && !io.in.ren, io.in.exception, Mux(io.lsuReady && !io.stall, exceptionreg, false.B))
  io.out.exception      := Mux(!io.lsuReady || io.stall, false.B, Mux(rState === wait_data || wState === wait_resp, exceptionreg, io.in.exception))
  io.out.csrWaddr       := Mux(rState === idle && wState === idle, io.in.csrWaddr, csrWaddrreg)
  //io.out.csrWen         := Mux(io.lsuReady && !io.stall, Mux(!hasTrans, io.in.csrWen, csrWenreg), false.B)
  io.out.csrWen         := Mux(!io.lsuReady || io.stall, false.B, Mux(rState === wait_data || wState === wait_resp, csrWenreg, io.in.csrWen))

  io.lsuTrans           := hasTrans || rState === wait_data || wState === wait_resp
  io.lsuReady           := !hasTrans || ((rState === wait_data && rdataFire) || (wState === wait_resp && brespFire)) && (rresp === okay || bresp === okay)
  //io.lsuReady           := true.B
  //val lsuReady           = RegInit(true.B)
  //lsuReady              := ((rState === wait_data && rdataFire) || (wState === wait_resp && brespFire)) && (rresp === okay || bresp === okay)
  //io.lsuReady           := Mux(!io.in.ren && !io.in.wen, true.B, lsuReady)
}