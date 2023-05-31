package jzcore

import chisel3._
import chisel3.util._
import utils._


class ICache extends Module {
  val io = IO(new Bundle {
    // todo: 是否需要valid信号来提示是一条有效指令?
    val validOut        = Output(Bool())
    val debugIn         = Flipped(new DebugIO)
    val debugOut        = new DebugIO

    // cpu
    val cpu2cache       = Flipped(new Stage1IO)
    val cache2cpu       = new InstrFetch

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
    val axiRaddrIO      = Decoupled(new RaddrIO)
    val axiRdataIO      = Flipped(Decoupled(new RdataIO))
    // useless
    val axiWaddrIO      = Decoupled(new WaddrIO)
    val axiWdataIO      = Decoupled(new WdataIO)
    val axiBrespIO      = Flipped(Decoupled(new BrespIO))    

    // arbiter
    val axiReq          = Output(Bool())
    val axiGrant        = Input(Bool())
    val axiReady        = Output(Bool())    
    
    // ctrl
    val stallOut        = Output(Bool())
    val stallIn         = Input(Bool())
    val flush           = Input(Bool())
  })

  val stage1 = Module(new CacheStage1)
  val stage2 = Module(new CacheStage2)
  val stage3 = Module(new CacheStage3)

  val dataArb0 = Module(new IcArbiter)
  val dataArb1 = Module(new IcArbiter)
  val dataArb2 = Module(new IcArbiter)
  val dataArb3 = Module(new IcArbiter)
  
  // debug
  io.validOut           <> stage3.io.validOut
  stage2.io.debugIn     <> io.debugIn
  stage2.io.debugOut    <> stage3.io.debugIn
  stage3.io.debugOut    <> io.debugOut


  io.cpu2cache          <> stage1.io.toStage1
  stage1.io.toStage2    <> stage2.io.toStage2
  stage2.io.toStage3    <> stage3.io.toStage3
  io.cache2cpu          <> stage3.io.out

  stage1.io.stall       := io.stallIn | stage3.io.stallOut | stage2.io.stallOut | io.flush
  stage2.io.flushIn     := io.flush
  stage2.io.stallIn     := io.stallIn | stage3.io.stallOut
  stage2.io.sram0_rdata <> io.sram0_rdata
  stage2.io.sram1_rdata <> io.sram1_rdata
  stage2.io.sram2_rdata <> io.sram2_rdata
  stage2.io.sram3_rdata <> io.sram3_rdata
  stage2.io.metaAlloc   <> stage3.io.metaAlloc
  stage3.io.stallIn     := io.stallIn
  stage3.io.flushIn     := io.flush
  io.stallOut           := stage3.io.stallOut | stage2.io.stallOut


  dataArb0.io.stage3Addr := stage3.io.sram0_addr
  dataArb0.io.stage3Cen  := stage3.io.sram0_cen
  dataArb0.io.stage3Wen  := stage3.io.sram0_wen
  dataArb0.io.stage1Addr := stage1.io.sram0_addr
  dataArb0.io.stage1Cen  := stage1.io.sram0_cen
  dataArb0.io.stage1Wen  := stage1.io.sram0_wen

  dataArb1.io.stage3Addr := stage3.io.sram1_addr
  dataArb1.io.stage3Cen  := stage3.io.sram1_cen
  dataArb1.io.stage3Wen  := stage3.io.sram1_wen
  dataArb1.io.stage1Addr := stage1.io.sram1_addr
  dataArb1.io.stage1Cen  := stage1.io.sram1_cen
  dataArb1.io.stage1Wen  := stage1.io.sram1_wen

  dataArb2.io.stage3Addr := stage3.io.sram2_addr
  dataArb2.io.stage3Cen  := stage3.io.sram2_cen
  dataArb2.io.stage3Wen  := stage3.io.sram2_wen
  dataArb2.io.stage1Addr := stage1.io.sram2_addr
  dataArb2.io.stage1Cen  := stage1.io.sram2_cen
  dataArb2.io.stage1Wen  := stage1.io.sram2_wen

  dataArb3.io.stage3Addr := stage3.io.sram3_addr
  dataArb3.io.stage3Cen  := stage3.io.sram3_cen
  dataArb3.io.stage3Wen  := stage3.io.sram3_wen
  dataArb3.io.stage1Addr := stage1.io.sram3_addr
  dataArb3.io.stage1Cen  := stage1.io.sram3_cen
  dataArb3.io.stage1Wen  := stage1.io.sram3_wen 

  io.sram0_addr          := dataArb0.io.arbAddr
  io.sram0_cen           := dataArb0.io.arbCen
  io.sram0_wen           := dataArb0.io.arbWen
  io.sram0_wmask         := stage3.io.sram0_wmask
  io.sram0_wdata         := stage3.io.sram0_wdata
  io.sram1_addr          := dataArb1.io.arbAddr
  io.sram1_cen           := dataArb1.io.arbCen
  io.sram1_wen           := dataArb1.io.arbWen
  io.sram1_wmask         := stage3.io.sram1_wmask
  io.sram1_wdata         := stage3.io.sram1_wdata
  io.sram2_addr          := dataArb2.io.arbAddr
  io.sram2_cen           := dataArb2.io.arbCen
  io.sram2_wen           := dataArb2.io.arbWen
  io.sram2_wmask         := stage3.io.sram2_wmask
  io.sram2_wdata         := stage3.io.sram2_wdata
  io.sram3_addr          := dataArb3.io.arbAddr
  io.sram3_cen           := dataArb3.io.arbCen
  io.sram3_wen           := dataArb3.io.arbWen
  io.sram3_wmask         := stage3.io.sram3_wmask
  io.sram3_wdata         := stage3.io.sram3_wdata

  io.axiRaddrIO         <> stage3.io.axiRaddrIO
  io.axiRdataIO         <> stage3.io.axiRdataIO
  io.axiWaddrIO         <> stage3.io.axiWaddrIO
  io.axiWdataIO         <> stage3.io.axiWdataIO
  io.axiBrespIO         <> stage3.io.axiBrespIO

  io.axiReq             <> stage3.io.axiReq
  io.axiGrant           <> stage3.io.axiGrant
  io.axiReady           <> stage3.io.axiReady
}

/*
// dataArray = 4KB, 4路组相连, 64个组，一个块16B
class ICache extends Module {
  val io = IO(new Bundle {
    val debugIn         = Flipped(new DebugIO)
    val debugOut        = new DebugIO

    // cpu
    val ctrlIO          = Flipped(Decoupled(new CacheCtrlIO))
    val wdataIO         = Flipped(Decoupled(new CacheWriteIO))
    val rdataIO         = Decoupled(new CacheReadIO) 

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

    // arbiter
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())
  })

  // random replace count
  val randCount          = RegInit(0.U(2.W))
  randCount             := randCount + 1.U(2.W)

  // the way chosen to victim
  val victimWay          = RegInit(0.U(2.W))

  val hit                = WireDefault(false.B)
  val dirty              = WireDefault(false.B)
  val wtag               = RegInit(0.U(22.W)) // dirty的tag

  // axi fire
  val raddrFire          = io.axiRaddrIO.valid && io.axiRaddrIO.ready
  val rdataFire          = io.axiRdataIO.valid && io.axiRdataIO.ready
  val waddrFire          = io.axiWaddrIO.valid && io.axiWaddrIO.ready
  val wdataFire          = io.axiWdataIO.valid && io.axiWdataIO.ready
  val brespFire          = io.axiBrespIO.valid && io.axiBrespIO.ready

  val ctrlFire           = io.ctrlIO.valid && io.ctrlIO.ready
  val cwdataFire         = io.wdataIO.valid && io.wdataIO.ready
  val crdataFire         = io.rdataIO.valid && io.rdataIO.ready

  // cache state machine
  val idle :: tagCompare :: data :: writeback1 :: writeback2 :: allocate1 :: allocate2 :: Nil = Enum(7)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle        -> Mux(ctrlFire, tagCompare, idle),
    tagCompare  -> Mux(hit, data, Mux(dirty, writeback1, allocate1)),
    data        -> Mux(crdataFire || cwdataFire, idle, data),
    writeback1  -> Mux(waddrFire && io.axiGrant, writeback2, writeback1), // addr
    writeback2  -> Mux(brespFire, allocate1, writeback2), // data and resp
    allocate1   -> Mux(raddrFire && io.axiGrant, allocate2, allocate1), // addr 
    allocate2   -> Mux(rdataFire && io.axiRdataIO.bits.rlast, data, allocate2) // data
  ))

  victimWay          := Mux(state === tagCompare, randCount, victimWay)

  // meta data
  val metaInit        = Wire(new MetaData)
  metaInit.valid     := false.B
  metaInit.dirty     := false.B
  metaInit.tag       := 0.U(22.W)
  val metaArray       = List.fill(4)(RegInit(VecInit(Seq.fill(64)(metaInit))))

  // ---------------------------address decode-----------------------------------------
  val addr    = RegInit(0.U(32.W))
  val wen     = RegInit(false.B)
  val tag     = Wire(UInt(22.W))
  val index   = Wire(UInt(6.W))
  val align   = Wire(Bool())
  addr       := Mux(state === idle && ctrlFire, io.ctrlIO.bits.addr, addr)
  wen        := Mux(state === idle && ctrlFire, io.ctrlIO.bits.wen, wen)
  tag        := addr(31, 10)
  index      := addr(9, 4)
  align      := addr(3)

  // ---------------------lookup metaArray and dataArray-------------------------------
  // metaArray lookup
  val hitList    = dontTouch(VecInit(List.fill(4)(false.B)))
  val hitListReg = RegInit(VecInit(List.fill(4)(false.B)))
  (0 to 3).map(i => (hitList(i) := Mux(state === tagCompare, metaArray(i)(index).valid && (metaArray(i)(index).tag === tag), false.B)))
  hit := (hitList.asUInt).orR
  hitListReg := Mux(state === tagCompare, hitList, hitListReg)

  when(state === tagCompare) {
    when(hit) {
      dirty := LookupTreeDefault(hitList.asUInt, false.B, List(
                "b0001".U   -> metaArray(0)(index).dirty,
                "b0010".U   -> metaArray(1)(index).dirty,
                "b0100".U   -> metaArray(2)(index).dirty,
                "b1000".U   -> metaArray(3)(index).dirty,
              ))
    }.otherwise {
      dirty := LookupTree(randCount, List(
                0.U   -> metaArray(0)(index).dirty,
                1.U   -> metaArray(1)(index).dirty,
                2.U   -> metaArray(2)(index).dirty,
                3.U   -> metaArray(3)(index).dirty,
              ))
    }
  }

  when(state === tagCompare && !hit) {
    wtag := LookupTree(randCount, List(
              0.U   -> metaArray(0)(index).tag,
              1.U   -> metaArray(1)(index).tag,
              2.U   -> metaArray(2)(index).tag,
              3.U   -> metaArray(3)(index).tag,
            ))
  }.otherwise {
    wtag := wtag
  }

  // dataArray lookup
  val dataBlock = RegInit(0.U(128.W))
  when(state === tagCompare) {
    when(hit) {
      dataBlock := LookupTree(hitList.asUInt, List(
                    "b0001".U   -> io.sram0_rdata,
                    "b0010".U   -> io.sram1_rdata,
                    "b0100".U   -> io.sram2_rdata,
                    "b1000".U   -> io.sram3_rdata,
                  ))
    }.otherwise {
      // random choose
      dataBlock := LookupTree(randCount, List(
                    0.U   -> io.sram0_rdata,
                    1.U   -> io.sram1_rdata,
                    2.U   -> io.sram2_rdata,
                    3.U   -> io.sram3_rdata,
                  ))
    }
  }.otherwise {
    dataBlock := dataBlock
  }

    // ----------------------------write back and allocate--------------------------------
  val allocTag = RegInit(false.B)
  allocTag := Mux(state === allocate1, true.B, Mux(state === idle, false.B, allocTag))

  // axi
  io.axiReq := state === writeback1 || state === allocate1
  io.axiReady := state === allocate2 && rdataFire && io.axiRdataIO.bits.rlast

  val burstAddr            = addr & "hfffffff8".U

  // allocate axi, burst read
  io.axiRaddrIO.valid     := state === allocate1
  io.axiRaddrIO.bits.addr  := burstAddr
  io.axiRaddrIO.bits.len  := 1.U(8.W) // 2
  io.axiRaddrIO.bits.size := 3.U(3.W) // 8B
  io.axiRaddrIO.bits.burst:= 2.U(2.W) // wrap
  io.axiRdataIO.ready     := state === allocate2

  val rblockBuffer         = RegInit(0.U(64.W))
  rblockBuffer            := MuxLookup(state, rblockBuffer, List(
                              allocate1 -> 0.U(64.W),
                              allocate2 -> Mux(rdataFire && !io.axiRdataIO.bits.rlast, io.axiRdataIO.bits.rdata, rblockBuffer)
                            ))
  val wburst = RegInit(0.U(2.W))
  when(state === tagCompare) {
    wburst := 0.U(2.W)
  }.elsewhen(state === writeback1 && wdataFire && waddrFire && io.axiGrant) {
    wburst := wburst + 1.U(2.W)
  }.elsewhen(state === writeback2 && (wburst === 1.U(2.W) || wburst === 0.U(2.W)) && wdataFire) {
    wburst := wburst + 1.U(2.W)
  }.otherwise {
    wburst := wburst
  }

  io.axiWaddrIO.valid     := state === writeback1 && io.axiGrant
  io.axiWaddrIO.bits.addr := Mux(state === writeback1 || state === writeback2, Cat(wtag, burstAddr(9, 0)), burstAddr)
  io.axiWaddrIO.bits.len  := 1.U(8.W) // 2
  io.axiWaddrIO.bits.size := 3.U(3.W) // 8B
  io.axiWaddrIO.bits.burst:= 2.U(2.W) // wrap

  io.axiWdataIO.valid     := state === writeback1 || state === writeback2
  io.axiWdataIO.bits.wlast:= state === writeback2 && wburst === 1.U(2.W)
  io.axiWdataIO.bits.wstrb:= "b11111111".U

  io.axiBrespIO.ready     := state === writeback2 && wburst === 2.U(2.W)
  // burst write
  when(state === writeback1 || (state === writeback2 && wburst === 0.U(2.W))) {
    io.axiWdataIO.bits.wdata := Mux(align, dataBlock(127, 64), dataBlock(63, 0))
  }.elsewhen(state === writeback2 && wburst === 1.U(2.W)) {
    io.axiWdataIO.bits.wdata := Mux(align, dataBlock(63, 0), dataBlock(127, 64))
  }.otherwise {
    io.axiWdataIO.bits.wdata := 0.U(64.W)
  }

  val alignMask0   = Mux(align, "hffffffffffffffff".U(128.W), ~"hffffffffffffffff".U(128.W))
  val alignMask1   = Mux(align, ~"hffffffffffffffff".U(128.W), "hffffffffffffffff".U(128.W))
  val rdata0       = Mux(align, Cat(io.axiRdataIO.bits.rdata, 0.U(64.W)), Cat(0.U(64.W), io.axiRdataIO.bits.rdata))
  val rdata1       = Mux(align, Cat(0.U(64.W), io.axiRdataIO.bits.rdata), Cat(io.axiRdataIO.bits.rdata, 0.U(64.W)))

  // dataArray control
  io.sram0_addr   := index
  io.sram0_wen    := true.B
  io.sram1_addr   := index
  io.sram1_wen    := true.B
  io.sram2_addr   := index
  io.sram2_wen    := true.B
  io.sram3_addr   := index
  io.sram3_wen    := true.B
  io.sram0_cen    := true.B
  io.sram1_cen    := true.B
  io.sram2_cen    := true.B
  io.sram3_cen    := true.B
  io.sram0_wdata  := 0.U
  io.sram0_wmask  := ~0.U(128.W)
  io.sram1_wdata  := 0.U
  io.sram1_wmask  := ~0.U(128.W)
  io.sram2_wdata  := 0.U
  io.sram2_wmask  := ~0.U(128.W)
  io.sram3_wdata  := 0.U
  io.sram3_wmask  := ~0.U(128.W)
  when(state === idle && ctrlFire) {
    // read data
    io.sram0_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram0_cen  := false.B
    io.sram1_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram1_cen  := false.B
    io.sram2_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram2_cen  := false.B
    io.sram3_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram3_cen  := false.B
  }.elsewhen(state === allocate2 && rdataFire) {
    // allocate metaArray
    val metaAlloc = Wire(new MetaData)
    metaAlloc.tag := tag
    metaAlloc.valid := true.B
    metaAlloc.dirty := false.B
    // allocate dataArray
    switch(victimWay) {
      is(0.U) {
        metaArray(0)(index) := metaAlloc
        io.sram0_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram0_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram0_cen    := false.B
        io.sram0_wen    := false.B
      }
      is(1.U) {
        metaArray(1)(index) := metaAlloc
        io.sram1_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram1_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram1_cen    := false.B
        io.sram1_wen    := false.B
      }
      is(2.U) {
        metaArray(2)(index) := metaAlloc
        io.sram2_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram2_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram2_cen    := false.B
        io.sram2_wen    := false.B
      }
      is(3.U) {
        metaArray(3)(index) := metaAlloc
        io.sram3_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram3_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram3_cen    := false.B
        io.sram3_wen    := false.B
      }
    }
  }.elsewhen(state === data && cwdataFire && wen) {
    // write dataArray
    // wmask8 to wmask64
    val wmask64 = Wire(Vec(8, UInt(8.W)))
    (0 to 7).map(i => (wmask64(i) := Mux(io.wdataIO.bits.wmask(i), 0.U(8.W), "hff".U)))
    // write enable
    when(allocTag) {
      //metaArray(victimWay)(index).dirty := true.B
      switch(victimWay) {
        is(0.U) {
          metaArray(0)(index).dirty := true.B
          io.sram0_wen    := false.B
          io.sram0_cen    := false.B
          io.sram0_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram0_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is(1.U) {
          metaArray(1)(index).dirty := true.B
          io.sram1_wen    := false.B
          io.sram1_cen    := false.B
          io.sram1_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram1_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is(2.U) {
          metaArray(2)(index).dirty := true.B
          io.sram2_wen    := false.B
          io.sram2_cen    := false.B
          io.sram2_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram2_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is(3.U) {
          metaArray(3)(index).dirty := true.B
          io.sram3_wen    := false.B
          io.sram3_cen    := false.B
          io.sram3_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram3_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
      }
    }.otherwise {
      switch(hitListReg.asUInt) {
        is("b0001".U) {
          metaArray(0)(index).dirty := true.B
          io.sram0_cen    := false.B
          io.sram0_wen    := false.B
          io.sram0_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram0_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is("b0010".U) {
          metaArray(1)(index).dirty := true.B
          io.sram1_cen    := false.B
          io.sram1_wen    := false.B
          io.sram1_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram1_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is("b0100".U) {
          metaArray(2)(index).dirty := true.B
          io.sram2_cen    := false.B
          io.sram2_wen    := false.B
          io.sram2_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram2_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is("b1000".U) {
          metaArray(3)(index).dirty := true.B
          io.sram3_cen    := false.B
          io.sram3_wen    := false.B
          io.sram3_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U), Cat("hffffffffffffffff".U, wmask64.asUInt))
          io.sram3_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
      }
    }
  }

  // -----------------------data aligner-------------------------------
  val alignData = WireDefault(0.U(64.W))
  when(state === data) {
    when(allocTag) {
      alignData := rblockBuffer
    }.otherwise {
      alignData := Mux(align, dataBlock(127, 64), dataBlock(63, 0))
    }
  }
  io.wdataIO.ready         := state === data

  io.rdataIO.bits.rdata    := Mux(state === data, alignData, 0.U(64.W))
  io.rdataIO.valid         := state === data

  io.ctrlIO.ready          := state === idle
}
*/