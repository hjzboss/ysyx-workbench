package jzcore

import chisel3._
import chisel3.util._
import utils._


// dataArray = 2KB, 4路组相连, 32个组，一个块16B
class Cache extends Module {
  val io = IO(new Bundle {
    // cpu
    val cpu2cache = Flipped(Decoupled(new CacheIO))

    // axi
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

  // 随机替换计数器
  val randCount       = RegInit(0.U(2.W))
  randCount          := randCount + 1.U(2.W)

  val hit   = WireDefault(false.B)
  val dirty = WireDefault(false.B)

  // axi fire
  val raddrFire          = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire          = io.axiRdataIO.valid && io.axiRdataIO.ready
  val waddrFire          = io.axiWaddrIO.valid && io.axiWaddrIO.ready
  val wdataFire          = io.axiWdataIO.valid && io.axiWdataIO.ready
  val brespFire          = io.axiBrespIO.valid && io.axiBrespIO.ready

  val cacheFire          = io.cpu2cache.valid && io.cpu2cache.ready

  // cache状态机
  val idle :: tagCompare :: data :: writeback1 :: writeback2 :: allocate1 :: allocate2 :: Nil = Enum(7)
  // val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle        -> Mux(io.cpu2cache.valid, tagCompare, idle),
    tagCompare  -> Mux(hit, data, Mux(dirty, writeback1, allocate1)),
    data        -> Mux(cacheFire, idle, data),
    writeback1  -> Mux(waddrFire, writeback2, writeback1),
    writeback2  -> Mux(wdataFire, allocate1, writeback2),
    allocate1   -> Mux(raddrFire, allocate2, allocate1),
    allocate2   -> Mux(rdataFire, data, allocate2)
  ))

  val addr    = io.cpu2cache.bits.addr
  val tag     = addr(63, 9)
  val index   = addr(8, 4)
  val align   = addr(3)

  val metaInit        = Wire(new MetaData)
  metaInit.valid     := false.B
  metaInit.dirty     := false.B
  metaInit.cacheable := false.B
  metaInit.tag       := 0.U(55.W)

  // cache bank: 4
  val metaArray = Vec(4, VecInit(Seq.fill(32)(metaInit)))
  val dataArray = List.fill(4)(Module(new Ram))

  val hitList = Vec(4, Bool())

  (0 to 3).map(i => (hitList(i) := metaArray(i)(index).valid && (metaArray(i)(index).tag === tag)))

  hit := (hitList.asUInt).orR.toBool

  io.cpu2cache.ready := state === data
}