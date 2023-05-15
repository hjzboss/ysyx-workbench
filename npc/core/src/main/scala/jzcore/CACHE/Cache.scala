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

  val raddrFire          = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire          = io.axiRdataIO.valid && io.axiRdataIO.ready
  val waddrFire          = io.axiWaddrIO.valid && io.axiWaddrIO.ready
  val wdataFire          = io.axiWdataIO.valid && io.axiWdataIO.ready
  val brespFire          = io.axiBrespIO.valid && io.axiBrespIO.ready

  // cache状态机
  val IDLE :: TAG :: DATA :: WRITEBACK1 :: WRITEBACK2 :: ALLOCATE1 :: ALLOCATE2 = Enum(7)
  // val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(IDLE)
  state := MuxLookup(state, IDLE, List(
    IDLE        -> Mux(io.cpu2cache.valid, TAG, IDLE),
    TAG         -> Mux(hit, DATA, Mux(dirty, WRITEBACK1, ALLOCATE1)),
    DATA        -> IDLE,
    WRITEBACK1  -> Mux(waddrFire, WRITEBACK2, WRITEBACK1),
    WRITEBACK2  -> Mux(wdataFire, ALLOCATE1, WRITEBACK2),
    ALLOCATE1   -> Mux(raddrFire, ALLOCATE2, ALLOCATE1),
    ALLOCATE2   -> Mux(rdataFire, DATA, ALLOCATE2)
  ))

  val addr    = io.cpu2cache.bits.addr
  val tag     = addr(63, 12)
  val index   = addr(11, 6)
  val align   = addr(5, 3)

  val metaInit        = Wire(new MetaData)
  metaInit.valid      = false.B
  metaInit.dirty      = false.B
  metaInit.cacheable  = false.B
  metaInit.tag        = 0.U(57.W)

  // cache bank: 4
  val metaArray = Vec(4, VecInit(Seq.fill(32)(metaInit)))
  val dataArray = Vec(2, new Ram)

  
  io.cpu2cache.ready := state === DATA
}