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
    
    // dcache访问接口
    val dcacheCtrl  = Decoupled(new CacheCtrlIO)
    val dcacheRead  = Flipped(Decoupled(new CacheReadIO))
    val dcacheWrite = Decoupled(new CacheWriteIO)

    /*
    // axi总线访存接口，用于外设的访问
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))

    // 仲裁信号
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())
    */

    val lsFlag      = Output(Bool())
  })

  /*
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // resp

  val raddrFire          = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire          = io.axiRdataIO.valid && io.axiRdataIO.ready
  val waddrFire          = io.axiWaddrIO.valid && io.axiWaddrIO.ready
  val wdataFire          = io.axiWdataIO.valid && io.axiWdataIO.ready
  val brespFire          = io.axiBrespIO.valid && io.axiBrespIO.ready

  // todo
  io.axiRaddrIO.bits.len := 0.U
  io.axiRaddrIO.bits.size:= 3.U
  io.axiRaddrIO.bits.burst := 2.U(2.W)
  io.axiWaddrIO.bits.len := 0.U
  io.axiWaddrIO.bits.size:= 3.U
  io.axiWaddrIO.bits.burst := 2.U(2.W)
  io.axiWdataIO.bits.wlast := true.B
  */

  val addr        = io.in.lsuAddr
  val readTrans   = io.in.lsuRen
  val writeTrans  = io.in.lsuWen
  val hasTrans    = readTrans || writeTrans

  val ctrlFire    = io.dcacheCtrl.valid && io.dcacheCtrl.ready
  val readFire    = io.dcacheRead.valid && io.dcacheWrite.ready
  val writeFire   = io.dcacheWrite.valid && io.dcacheWrite.ready

  val idle :: ctrl :: data :: Nil = Enum(2)
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle -> Mux(hasTrans, ctrl, idle),
    ctrl -> Mux(ctrlFire, data, ctrl),
    data -> Mux(readFire && writeFire, Mux(hasTrans, ctrl, idle), data) // todo
  ))

  val cacheable                  = addr =/= "ha0000048".U && addr =/= "ha0000050".U && addr =/= "ha0000100".U && addr =/= "ha0000080".U && addr =/= "ha00003f8".U && addr =/= "a0000108".U

  io.dcacheCtrl.valid           := state === ctrl
  io.dcacheCtrl.bits.wen        := writeTrans
  io.dcacheCtrl.bits.addr       := addr
  io.dcacheCtrl.bits.cacheable  := cacheable

  io.dcacheRead.ready           := state === data
  io.dcacheWrite.valid          := state === data
  io.dcacheWrite.bits.wdata     := io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U)
  io.dcacheWrite.bits.wmask     := io.in.wmask << addr(2, 0)

  /*
  // load状态机
  val idle :: wait_data :: wait_resp ::Nil = Enum(3)
  val rState = RegInit(idle)
  rState := MuxLookup(rState, idle, List(
    idle        -> Mux(raddrFire && io.axiGrant, wait_data, idle),
    wait_data   -> Mux(rdataFire, idle, wait_data)
  ))

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
  io.axiWdataIO.bits.wdata := io.in.lsuWdata << (ZeroExt(addr(2, 0), 6) << 3.U)
  io.axiWdataIO.bits.wstrb := io.in.wmask << addr(2, 0) // todo
  io.axiBrespIO.ready      := wState === wait_resp
  */

  // 数据对齐
  val align              = Cat(addr(2, 0), 0.U(3.W))
  val rdata              = io.dcacheRead.bits.rdata >> align
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

  io.out.lsuOut         := lsuOut
  io.out.loadMem        := io.in.loadMem
  io.out.exuOut         := io.in.exuOut
  io.out.rd             := io.in.rd
  io.out.regWen         := io.in.regWen
  io.out.pc             := io.in.pc
  io.out.excepNo        := io.in.excepNo
  io.out.exception      := io.in.exception
  io.out.csrWaddr       := io.in.csrWaddr
  io.out.csrWen         := io.in.csrWen
  io.out.ebreak         := io.in.ebreak
  io.out.haltRet        := io.in.haltRet
  io.out.csrValue       := io.in.csrValue


  //io.ready              := !(readTrans || writeTrans) || ((rState === wait_data && rdataFire) || (wState === wait_resp && brespFire)) && (rresp === okay || bresp === okay)
  io.ready              := (state === idle && !(readTrans || writeTrans)) || (state === data && readFire && writeFire)
  // 仲裁信号
  //io.axiReq             := (rState === idle && io.in.lsuRen) || (wState === idle && io.in.lsuWen)
  //io.axiReady           := (rState === wait_data && rdataFire) || (brespFire && wState === wait_resp)

  // 传给仿真环境，用于外设访问的判定
  io.lsFlag             := io.in.lsuRen || io.in.lsuWen
}