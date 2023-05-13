package jzcore

import chisel3._
import chisel3.util._
import utils._


class CacheBank extends Module {
  val io = IO(new Bundle {
    val readIO      = Decoupled(new CacheReadIO)
    val writeIO     = Decoupled(new CacheWriteIO)

    val addr        = Input(UInt(64.W))
    
    

    val replace     = Input(Vec(8, 0.U(64.W)))
    val repValid    = Output(Bool())
  })

  val metaInit   = Wire(new MetaData)
  metaInit.valid = false.B
  metaInit.dirty = false.B
  metaInit.tag   = 0.U(20.W)

  // 64 entry
  val metaArray = RegInit(VecInit(Seq.fill(64)(metaInit)))
  val dataArray = RegInit(VecInit(Seq.fill(64)(VecInit(Seq.fill(8)(0.U(64.W))))))

  // ---------------------cache read---------------------
  // decode
  val raddr   = io.readIO.bits.addr
  val rtag    = raddr(63, 12)
  val rindex  = raddr(11, 6)
  val ralign  = raddr(5, 3)

  val rmiss   = WireDefault(false.B)

  when (io.readIO.valid) {
    val meta = metaArray(rindex)
    val data = dataArray(rindex)

    io.readIO.bits.data := Mux(meta.valid && (rtag === meta.tag), data(ralign), 0.U(64.W))
    io.readIO.ready     := meta.valid && (rtag === meta.tag)
    rmiss               := !(meta.valid && (rtag === meta.tag))
  }.otherwise {
    io.readIO.ready     := true.B
    io.readIO.bits.data := 0.U(64.W)
  }

  // ---------------------cache write---------------------
  val waddr   = io.writeIO.bits.addr
  val wtag    = waddr(63, 12)
  val windex  = waddr(11, 6)
  val walign  = waddr(5, 3)

  val wmiss   = WireDefault(false.B)

  when (io.writeIO.valid) {
    val meta = metaArray(windex)
    val data = dataArray(windex)

    when (meta.valid && (rtag === meta.tag)) {

    }
  }.otherwise {
    io.readIO.ready     := true.B
    io.readIO.bits.data := 0.U(64.W)
  }
}