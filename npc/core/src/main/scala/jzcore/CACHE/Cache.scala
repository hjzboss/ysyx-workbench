/*
package jzcore

import chisel3._
import chisel3.util._
import utils._


// dataArray = 4KB, 4路组相连, 64个组，一个块16B
class Cache extends Module {
  val io = IO(new Bundle {
    // cpu
    val cpu2cache = Flipped(Decoupled(new CacheIO))

    // ram, dataArray
    val sram0_rdata     = Input(UInt(128.W))
    val sram0_cen       = Output(Bool())
    val sram0_wen       = Output(Bool())
    val sram0_wmask     = Output(UInt(128.W))
    val sram0_addr      = Output(UInt(6.W))
    val sram0_wdata     = Output(UInt(128.W)) 

    val sram1_rdata     = Input(UInt(128.W))
    val sram1_cen       = Output(Bool())
    val sram1_wen       = Output(Bool())
    val sram1_wmask     = Output(UInt(128.W))
    val sram1_addr      = Output(UInt(6.W))
    val sram1_wdata     = Output(UInt(128.W)) 

    val sram2_rdata     = Input(UInt(128.W))
    val sram2_cen       = Output(Bool())
    val sram2_wen       = Output(Bool())
    val sram2_wmask     = Output(UInt(128.W))
    val sram2_addr      = Output(UInt(6.W))
    val sram2_wdata     = Output(UInt(128.W)) 

    val sram3_rdata     = Input(UInt(128.W))
    val sram3_cen       = Output(Bool())
    val sram3_wen       = Output(Bool())
    val sram3_wmask     = Output(UInt(128.W))
    val sram3_addr      = Output(UInt(6.W))
    val sram3_wdata     = Output(UInt(128.W)) 

    // axi
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    
    // todo: burst


    // arbiter
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())
  })

  // 随机替换计数器
  val randCount          = RegInit(0.U(2.W))
  randCount             := randCount + 1.U(2.W)

  val hit                = WireDefault(false.B)
  val dirty              = WireDefault(false.B)

  // axi fire
  val raddrFire          = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire          = io.axiRdataIO.valid && io.axiRdataIO.ready
  val waddrFire          = io.axiWaddrIO.valid && io.axiWaddrIO.ready
  val wdataFire          = io.axiWdataIO.valid && io.axiWdataIO.ready
  val brespFire          = io.axiBrespIO.valid && io.axiBrespIO.ready

  val cacheFire          = io.cpu2cache.valid && io.cpu2cache.ready

  // cache状态机
  val idle :: tagCompare :: data :: writeback1 :: writeback2 :: allocate1 :: allocate2 :: Nil = Enum(7)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle        -> Mux(io.cpu2cache.valid, tagCompare, idle),
    tagCompare  -> Mux(hit, data, Mux(dirty, writeback1, allocate1)),
    data        -> Mux(cacheFire, idle, data),
    writeback1  -> Mux(waddrFire && io.axiGrant, writeback2, writeback1), // addr
    writeback2  -> Mux(brespFire, allocate1, writeback2), // data and resp
    allocate1   -> Mux(raddrFire && io.axiGrant, allocate2, allocate1), // addr 
    allocate2   -> Mux(rdataFire && io.axiRdataIO.bits.last, data, allocate2) // data
  ))

  // address decoder
  val addr    = io.cpu2cache.bits.addr
  val tag     = addr(63, 10)
  val index   = addr(9, 4)
  val align   = addr(3)

  // meta data
  val metaInit        = Wire(new MetaData)
  metaInit.valid     := false.B
  metaInit.dirty     := false.B
  //metaInit.cacheable := false.B
  metaInit.tag       := 0.U(54.W)
  val metaArray = List.fill(4)(RegInit(VecInit(Seq.fill(32)(metaInit))))

  // hit and dirty
  val hitList = Wire(Vec(4, Bool()))
  (0 to 3).map(i => (hitList(i) := metaArray(i)(index).valid && (metaArray(i)(index).tag === tag)))
  dirty := LookupTreeDefault(hitList.asUInt, false.B, List(
    "b0001".U   -> metaArray(0)(index).dirty,
    "b0010".U   -> metaArray(1)(index).dirty,
    "b0100".U   -> metaArray(2)(index).dirty,
    "b1000".U   -> metaArray(3)(index).dirty,
  ))
  hit := (hitList.asUInt).orR

  val wburstOne = RegInit(Bool())
  wburstOne := Mux(state === writeback1, false.B, Mux(state === writeback2 && wdataFire, true.B, wburstOne))
  val rburstOne = RegInit(Bool())
  rburstOne := Mux(state === allocate1, false.B, Mux(state === allocate2 && rdataFire, true.B, rburstOne))

  // axi
  io.axiReq := state === writeback1 || state === allocate1
  io.axiReady := (state === writeback2 && brespFire) || (state === allocate2 && rdataFire && io.rdataIO.rlast)

  val burstAddr            = addr & "hfffffff8".U

  // todo: allocate axi, burst read
  io.axiRaddrIO.valid     := state === allocate1
  io.axiRaddrIO.bit.addr  := burstAddr
  io.axiRaddrIO.bits.len  := 1.U(8.W) // 2
  io.axiRaddrIO.bits.size := 3.U(3.W) // 8B
  io.axiRaddrIO.bits.burst:= 2.U(2.W) // wrap
  io.axiRdataIO.ready     := state === allocate2

  val rblockBuffer         = RegInit(VecInit(Seq.fill(2)(0.U(64.W))))
  rblockBuffer(0)         := MuxLookup(state, 0.U(64.W), List(
                              writeback1 -> 0.U(64.W),
                              writeback2 -> Mux(rdataFire && !rburstOne, io.axiRdataio.bits.rdata, rblockBuffer(0))
                            ))
  rblockBuffer(1)         := MuxLookup(state, 0.U(64.W), List(
                              writeback1 -> 0.U(64.W),
                              writeback2 -> Mux(rdataFire && io.axiRdataIO.bits.rlast, io.axiRdataio.bits.rdata, rblockBuffer(1))
                            ))

  val rblockData           = Cat(rblockBuffer(1), rblockBuffer(0))
  val rblockDataRev        = Cat(rblockBuffer(0), rblockBuffer(1))

  // todo: writeback axi, burst write
  io.axiWaddrIO.valid     := state === writeback1
  io.axiWaddrIO.bits.addr := burstAddr
  io.axiWaddrIO.bits.len  := 1.U(8.W) // 2
  io.axiWaddrIO.bits.size := 3.U(3.W) // 8B
  io.axiWaddrIO.bits.burst:= 2.U(2.W) // wrap

  io.axiWdataIO.valid     := state === writeback2
  io.axiWdataIO.wlast     := state === writeback2 && wburstEnd
  io.axiWdataIO.wstrb     := "b11111111".U
  // todo: wdata



  // ram
  io.sram0_addr   := index
  io.sram0_wen    := !io.cpu2cache.bits.wen
  io.sram1_addr   := index
  io.sram1_wen    := !io.cpu2cache.bits.wen
  io.sram2_addr   := index
  io.sram2_wen    := !io.cpu2cache.bits.wen
  io.sram3_addr   := index
  io.sram3_wen    := !io.cpu2cache.bits.wen

  io.sram0_cen    := true.B
  io.sram1_cen    := true.B
  io.sram2_cen    := true.B
  io.sram3_cen    := true.B
  when((state === tagCompare && hit && !io.cpu2cache.bits.wen) || (state === allocate2 && rdataFire && !io.cpu2cache.bits.wen)) {
    // read data
    io.sram0_cen  := false.B
    io.sram1_cen  := false.B
    io.sram2_cen  := false.B
    io.sram3_cen  := false.B
  }.elsewhen(state === allocate2 && rdataFire) {
    // allocate
    switch(randCount) {
      is(0.U) {
        io.sram0_cen    := false.B
        io.sram0_wdata  := Mux(align, rblockDataRev, rblockData)
        io.sram0_wen    := false.B
        io.sram0_wmask  := 0.U(128.W)
      }
      is(1.U) {
        io.sram1_cen    := false.B
        io.sram1_wdata  := Mux(align, rblockDataRev, rblockData)
        io.sram1_wen    := false.B
        io.sram1_wmask  := 0.U(128.W)
      }
      is(2.U) {
        io.sram2_cen    := false.B
        io.sram2_wdata  := Mux(align, rblockDataRev, rblockData)
        io.sram2_wen    := false.B
        io.sram2_wmask  := 0.U(128.W)
      }
      is(3.U) {
        io.sram3_cen    := false.B
        io.sram3_wdata  := Mux(align, rblockDataRev, rblockData)
        io.sram3_wen    := false.B
        io.sram3_wmask  := 0.U(128.W)
      }
    }
  }.elsewhen(state === data && io.cpu2cache.bits.wen) {
    // write data
    switch(hitList) {
      is("b0001".U) {
        io.sram0_cen    := false.B
        //io.sram0_wmask  := Mux(align, "h0000_0000_0000_0000_ffff_ffff_ffff_ffff".U, "hffff_ffff_ffff_ffff_0000_0000_0000_0000".U)
        // mask, todo
        
      }
    }
  }

  // return value
  val blockData = LookupTreeDefault(hitList.asUInt, 0.U(128.W), List(
    "b0001".U   -> io.sram0_rdata,
    "b0010".U   -> io.sram1_rdata,
    "b0100".U   -> io.sram2_rdata,
    "b1000".U   -> io.sram3_rdata,
  ))
  val alignData = Mux(align, blockData(127, 64), blockData(63, 0))
  io.cpu2cache.bits.rdata := Mux(state === data, alignData, 0.U(64.W))
  io.cpu2cache.ready      := state === data
}
*/