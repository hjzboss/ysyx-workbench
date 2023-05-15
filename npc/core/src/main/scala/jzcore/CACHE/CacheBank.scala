/*
package jzcore

import chisel3._
import chisel3.util._
import utils._


class CacheBank extends Module {
  val io = IO(new Bundle {
    val decode      = new CacheDecode

    val wvalid      = Input(Bool())
    val wdata       = Input(UInt(64.W))

    val rvalid      = Input(Bool())
    val rblock      = Output(Vec(8, UInt(64.W)))

    val hit         = Output(Bool())
    val dirty       = Output(Bool())
    
    // cache替换
    val repValid    = Input(Bool())
    val repBlock    = Input(Vec(8, UInt(64.W)))
  })

  val tag             = io.decode.tag
  val index           = io.decode.index

  val metaInit        = Wire(new MetaData)
  metaInit.valid      = false.B
  metaInit.dirty      = false.B
  metaInit.cacheable  = false.B
  metaInit.tag        = 0.U(52.W)

  val meteRep         = Wire(new MetaData)
  metaRep.valid      := true.B
  metaRep.dirty      := false.B
  metaRep.cacheable  := true.B
  metaRep.tag        := tag

  // 64 entry
  val metaArray = RegInit(VecInit(Seq.fill(64)(metaInit)))
  val dataArray = RegInit(VecInit(Seq.fill(64)(VecInit(Seq.fill(8)(0.U(64.W))))))

  // replace
  when(io.repValid) {
    metaArray(index)  := metaRep
    dataArray(index)  := io.repBlock
  }

  // ---------------------cache read---------------------
  val metaBlock    = metaArray(index)
  val dataBlock    = dataArray(index)

  val rhit   = WireDefault(false.B)
  when(io.rvalid) {
    io.rblock           := dataBlock
    rhit                := metaBlock.tag === tag && metaBlock.valid
  }.otherwise {

  }

  // ---------------------cache write---------------------
  val wmiss   = WireDefault(false.B)

  when(io.writeIO.valid) {
    when (meta.valid && (rtag === meta.tag)) {

    }
  }.otherwise {
    io.readIO.ready     := true.B
    io.readIO.bits.data := 0.U(64.W)
  }
}
*/