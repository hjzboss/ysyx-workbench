package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

// 问题原因：取指出错，取出了cacheline中错误位置的指令
sealed class IcArbiter extends Module {
  val io = IO(new Bundle {
    val stage3Addr = Input(UInt(6.W))
    val stage1Addr = Input(UInt(6.W))
    val stage3Cen  = Input(Bool())
    val stage1Cen  = Input(Bool())
    val stage3Wen  = Input(Bool())
    val stage1Wen  = Input(Bool())

    val arbAddr    = Output(UInt(6.W))
    val arbCen     = Output(Bool())
    val arbWen     = Output(Bool())
  })

  io.arbAddr     := Mux(io.stage3Cen, io.stage1Addr, io.stage3Addr)
  io.arbCen      := Mux(io.stage3Cen, io.stage1Cen, io.stage3Cen)
  io.arbWen      := Mux(io.stage3Wen, io.stage1Wen, io.stage3Wen)
}

sealed class CacheStage1 extends Module {
  val io = IO(new Bundle {
    val toStage1        = Flipped(new Stage1IO)
    val toStage2        = new Stage2IO

    val stall           = Input(Bool())

    // data array
    val sram0_cen       = Output(Bool())
    val sram0_wen       = Output(Bool())
    val sram0_addr      = Output(UInt(6.W))

    val sram1_cen       = Output(Bool())
    val sram1_wen       = Output(Bool())
    val sram1_addr      = Output(UInt(6.W))

    val sram2_cen       = Output(Bool())
    val sram2_wen       = Output(Bool())
    val sram2_addr      = Output(UInt(6.W))

    val sram3_cen       = Output(Bool())
    val sram3_wen       = Output(Bool())
    val sram3_addr      = Output(UInt(6.W))
  })

  // decode
  val tag        = io.toStage1.addr(31, 10)
  val index      = io.toStage1.addr(9, 4)
  val align      = io.toStage1.addr(3, 2)

  val valid      = !io.stall

  io.sram0_cen  := !valid
  io.sram1_cen  := !valid
  io.sram2_cen  := !valid
  io.sram3_cen  := !valid
  io.sram0_wen  := true.B
  io.sram1_wen  := true.B
  io.sram2_wen  := true.B
  io.sram3_wen  := true.B
  io.sram0_addr := index
  io.sram1_addr := index
  io.sram2_addr := index
  io.sram3_addr := index

  io.toStage2.index     := index
  io.toStage2.tag       := tag
  io.toStage2.align     := align
  io.toStage2.pc        := io.toStage1.addr
  io.toStage2.cacheable := io.toStage1.cacheable
}

sealed class CacheStage2 extends Module with HasResetVector {
  val io = IO(new Bundle {
    val debugIn         = if(Settings.get("sim")) Some(Flipped(new DebugIO)) else None
    val debugOut        = if(Settings.get("sim")) Some(new DebugIO) else None

    val validIn         = Input(Bool())
    val validOut        = Output(Bool())

    // cache
    val toStage2        = Flipped(new Stage2IO)
    val toStage3        = new Stage3IO

    val flushIn         = Input(Bool())
    val stallIn         = Input(Bool()) // lsu的stall，优先级最高
    val stage3Stall     = Input(Bool()) // cache stage3的stall，优先级最低

    val sram0_rdata     = Input(UInt(128.W))
    val sram1_rdata     = Input(UInt(128.W))
    val sram2_rdata     = Input(UInt(128.W))
    val sram3_rdata     = Input(UInt(128.W))

    val metaAlloc       = Flipped(new MetaAllocIO)
  })


  val validReg           = RegInit(false.B)
  validReg              := Mux(io.stallIn, validReg, Mux(io.flushIn, false.B, Mux(io.stage3Stall, validReg, io.validIn)))
  io.validOut           := validReg

  // pipline reg
  val regInit            = Wire(new Stage2IO)
  regInit.index         := 0.U(6.W)
  regInit.tag           := 0.U(22.W)
  regInit.align         := false.B
  regInit.cacheable     := true.B
  regInit.pc            := 0.U(32.W)
  val stage2Reg          = RegInit(regInit)
  stage2Reg             := Mux(io.stallIn, stage2Reg, Mux(io.flushIn, regInit, Mux(io.stage3Stall, stage2Reg, io.toStage2)))

  // just for verilator
  if(Settings.get("sim")) {
    val debugReset         = Wire(new DebugIO)
    debugReset.pc         := 0.U(32.W)
    debugReset.nextPc     := 0.U(32.W)
    debugReset.inst       := Instruction.NOP
    debugReset.valid      := false.B

    val debugReg           = RegInit(debugReset)
    debugReg              := Mux(io.stallIn, debugReg, Mux(io.flushIn, debugReset, io.debugIn.get))
    io.debugOut.get       := debugReg
  }

  // random replace count
  val randCount          = RegInit(0.U(2.W))
  randCount             := randCount + 1.U(2.W)

  // meta array
  val metaInit           = Wire(new MetaData)
  metaInit.valid        := false.B
  metaInit.dirty        := false.B
  metaInit.tag          := 0.U(22.W)
  val metaArray          = List.fill(4)(RegInit(VecInit(Seq.fill(64)(metaInit))))

  // metaArray lookup
  val hitList    = dontTouch(VecInit(List.fill(4)(false.B)))
  (0 to 3).map(i => (hitList(i) := metaArray(i)(stage2Reg.index).valid && (metaArray(i)(stage2Reg.index).tag === stage2Reg.tag)))
  //val hit = (hitList.asUInt).orR
  val hit = dontTouch(WireDefault(true.B))
  hit    := (hitList.asUInt).orR
  // metaArray alloc
  when(io.metaAlloc.valid) {
    // allocate metaArray
    val meta = Wire(new MetaData)
    meta.tag := io.metaAlloc.tag
    meta.valid := true.B
    meta.dirty := false.B
    switch(io.metaAlloc.victim) {
      is(0.U) { metaArray(0)(io.metaAlloc.index) := meta }
      is(1.U) { metaArray(1)(io.metaAlloc.index) := meta }
      is(2.U) { metaArray(2)(io.metaAlloc.index) := meta }
      is(3.U) { metaArray(3)(io.metaAlloc.index) := meta }
    }
  }

  val stallReg           = RegInit(false.B)
  stallReg              := Mux(io.stallIn || io.stage3Stall, true.B, false.B)

  // todo: 未锁存
  val cacheline          = RegInit(0.U(128.W))
  val tmp                = MuxLookup(hitList.asUInt, 0.U(128.W), List(
                              1.U -> io.sram0_rdata,
                              2.U -> io.sram1_rdata,
                              4.U -> io.sram2_rdata,
                              8.U -> io.sram3_rdata,
                          ))

  // todo 
  cacheline             := Mux(io.stallIn && !stallReg, tmp, Mux(io.flushIn, 0.U(128.W), Mux(io.stage3Stall && !stallReg, tmp, cacheline)))
  io.toStage3.cacheline := Mux(stallReg, cacheline, tmp)

  // !stage2Reg.pc.or是为了复位后也要保证hit为true
  io.toStage3.hit       := Mux(io.flushIn || !stage2Reg.pc.orR, true.B, hit)
  //io.toStage3.allocAddr := stage2Reg.tag ## stage2Reg.index ## stage2Reg.align(1) ## 0.U(3.W)
  io.toStage3.allocAddr := Mux(io.toStage2.cacheable, stage2Reg.tag ## stage2Reg.index ## stage2Reg.align(1) ## 0.U(3.W), stage2Reg.pc)
  io.toStage3.victim    := randCount
  io.toStage3.cacheable := stage2Reg.cacheable
  io.toStage3.align     := stage2Reg.align
  io.toStage3.pc        := stage2Reg.pc
  io.toStage3.index     := stage2Reg.index
  io.toStage3.tag       := stage2Reg.tag
}

sealed class CacheStage3 extends Module with HasResetVector {
  val io = IO(new Bundle {
    // debug
    val validIn         = Input(Bool())
    val validOut        = Output(Bool())
  
    val debugIn         = if(Settings.get("sim")) Some(Flipped(new DebugIO)) else None
    val debugOut        = if(Settings.get("sim")) Some(new DebugIO) else None

    val toStage3        = Flipped(new Stage3IO)

    val stallOut        = Output(Bool())
    val stallIn         = Input(Bool())

    val flushIn         = Input(Bool())
    val flushOut        = Output(Bool())
    val redirect        = Output(Bool()) // 转发stage2的地址给data ram

    // to idu
    val out             = new InstrFetch

    // data array, allocate
    val sram0_cen       = Output(Bool())
    val sram0_wen       = Output(Bool())
    val sram0_wmask     = Output(UInt(128.W))
    val sram0_addr      = Output(UInt(6.W))
    val sram0_wdata     = Output(UInt(128.W)) 

    val sram1_cen       = Output(Bool())
    val sram1_wen       = Output(Bool())
    val sram1_wmask     = Output(UInt(128.W))
    val sram1_addr      = Output(UInt(6.W))
    val sram1_wdata     = Output(UInt(128.W)) 

    val sram2_cen       = Output(Bool())
    val sram2_wen       = Output(Bool())
    val sram2_wmask     = Output(UInt(128.W))
    val sram2_addr      = Output(UInt(6.W))
    val sram2_wdata     = Output(UInt(128.W)) 

    val sram3_cen       = Output(Bool())
    val sram3_wen       = Output(Bool())
    val sram3_wmask     = Output(UInt(128.W))
    val sram3_addr      = Output(UInt(6.W))
    val sram3_wdata     = Output(UInt(128.W)) 

    val metaAlloc       = new MetaAllocIO

    // axi master
    val master         = new AxiMaster

    // arbiter
    val axiReq          = Output(Bool())
    val axiGrant        = Input(Bool())
    val axiReady        = Output(Bool())
  })

  val flushReg          = WireDefault(false.B)
  io.redirect          := flushReg
  val regInit           = Wire(new Stage3IO)
  regInit.pc           := 0.U(32.W)
  regInit.cacheline    := 0.U(128.W)
  regInit.hit          := true.B
  regInit.allocAddr    := 0.U(32.W)
  regInit.cacheable    := true.B
  regInit.victim       := 0.U(2.W)
  regInit.align        := 0.U(2.W)
  regInit.tag          := 0.U(22.W)
  regInit.index        := 0.U(6.W)
  val stage3Reg         = RegInit(regInit)
  stage3Reg            := Mux(io.stallIn, stage3Reg, Mux(io.flushIn || flushReg, regInit, Mux(io.stallOut, stage3Reg, io.toStage3)))

  if(Settings.get("sim")) {
    val debugReset            = Wire(new DebugIO)
    debugReset.pc            := 0.U(32.W)
    debugReset.nextPc        := 0.U(32.W)
    debugReset.inst          := Instruction.NOP
    debugReset.valid         := false.B

    val debugReg              = RegInit(debugReset)
    debugReg                 := Mux(io.stallIn, debugReg, Mux(io.flushIn || flushReg, debugReset, Mux(io.stallOut, debugReg, io.debugIn.get)))
    io.debugOut.get.pc       := io.out.pc
    io.debugOut.get.nextPc   := debugReg.nextPc
    io.debugOut.get.valid    := io.validOut
    io.debugOut.get.inst     := io.out.inst
  }

  val align             = stage3Reg.align

  // axi fire
  val raddrFire         = io.master.arvalid && io.master.arready
  val rdataFire         = io.master.rvalid && io.master.rready

  // 分配和flash取指状态机
  val idle :: addr :: data :: flush :: stall :: Nil = Enum(5)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle  -> Mux(io.flushIn || io.stallIn, idle, Mux(!stage3Reg.hit || !stage3Reg.cacheable, addr, idle)),
    addr  -> Mux(io.flushIn, Mux(io.axiGrant && raddrFire, flush, idle), Mux(raddrFire && io.axiGrant, data, addr)),
    data  -> Mux(rdataFire && io.master.rlast && (io.master.rresp === okay || io.master.rresp === exokay), Mux(io.stallIn, stall, idle), Mux(io.flushIn, flush, data)),
    stall -> Mux(io.stallIn, stall, idle),
    flush -> Mux(rdataFire && io.master.rlast, Mux(io.stallIn, stall, idle), flush)
  ))

  // todo
  flushReg                := (state === data && rdataFire && io.master.rlast && !io.stallIn) || (state === stall && !io.stallIn) 
  io.flushOut             := flushReg

  val stallOut             = (state === idle && (!stage3Reg.hit || !stage3Reg.cacheable)) || state === addr || state === data || state === stall || state === flush
  io.stallOut             := stallOut && !flushReg 

  io.axiReq               := state === addr
  io.axiReady             := (state === data || state === flush) && rdataFire && io.master.rlast

  // allocate axi, burst read
  io.master.arid          := 0.U
  io.master.arvalid      := state === addr
  io.master.araddr       := stage3Reg.allocAddr
  io.master.arlen        := Mux(stage3Reg.cacheable, 1.U(8.W), 0.U(8.W))
  io.master.arsize       := Mux(stage3Reg.cacheable, 3.U(3.W), 2.U(3.W))
  io.master.arburst      := Mux(stage3Reg.cacheable, 2.U(2.W), 0.U(2.W))
  io.master.rready       := state === data || state === flush

  val rblockBuffer         = RegInit(0.U(64.W))
  rblockBuffer            := MuxLookup(state, rblockBuffer, List(
                              addr -> 0.U(64.W),
                              data -> Mux(io.flushIn, 0.U(64.W), Mux(rdataFire && !io.master.rlast, io.master.rdata, rblockBuffer)),
                            ))

  // axi write
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

  val alignMask0   = Mux(align(1), "hffffffffffffffff".U(128.W), ~"hffffffffffffffff".U(128.W))
  val alignMask1   = Mux(align(1), ~"hffffffffffffffff".U(128.W), "hffffffffffffffff".U(128.W))
  val rdata0       = Mux(align(1), Cat(io.master.rdata, 0.U(64.W)), Cat(0.U(64.W), io.master.rdata))
  val rdata1       = Mux(align(1), Cat(0.U(64.W), io.master.rdata), Cat(io.master.rdata, 0.U(64.W)))

  // dataArray control, todo: 此时发生冲刷仍会写入cache
  io.sram0_addr   := stage3Reg.index
  io.sram0_wen    := true.B
  io.sram1_addr   := stage3Reg.index
  io.sram1_wen    := true.B
  io.sram2_addr   := stage3Reg.index
  io.sram2_wen    := true.B
  io.sram3_addr   := stage3Reg.index
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
  when(state === data && rdataFire && stage3Reg.cacheable) {
    // allocate dataArray
    switch(stage3Reg.victim) {
      is(0.U) {
        io.sram0_wdata  := Mux(io.master.rlast, rdata1, rdata0)
        io.sram0_wmask  := Mux(io.master.rlast, alignMask1, alignMask0)
        io.sram0_cen    := false.B
        io.sram0_wen    := false.B
      }
      is(1.U) {
        io.sram1_wdata  := Mux(io.master.rlast, rdata1, rdata0)
        io.sram1_wmask  := Mux(io.master.rlast, alignMask1, alignMask0)
        io.sram1_cen    := false.B
        io.sram1_wen    := false.B
      }
      is(2.U) {
        io.sram2_wdata  := Mux(io.master.rlast, rdata1, rdata0)
        io.sram2_wmask  := Mux(io.master.rlast, alignMask1, alignMask0)
        io.sram2_cen    := false.B
        io.sram2_wen    := false.B
      }
      is(3.U) {
        io.sram3_wdata  := Mux(io.master.rlast, rdata1, rdata0)
        io.sram3_wmask  := Mux(io.master.rlast, alignMask1, alignMask0)
        io.sram3_cen    := false.B
        io.sram3_wen    := false.B
      }
    }
  }

  // meta array alloc
  io.metaAlloc.tag    := stage3Reg.tag
  io.metaAlloc.index  := stage3Reg.index
  io.metaAlloc.victim := stage3Reg.victim
  io.metaAlloc.valid  := state === data && rdataFire && io.master.rlast && stage3Reg.cacheable

  val inst             = WireDefault(Instruction.NOP)
  // -----------------------data select-------------------------------
  when(state === idle && stage3Reg.hit && stage3Reg.cacheable) {
    when(align(1)) {
      inst := Mux(stage3Reg.align(0), stage3Reg.cacheline(127, 96), stage3Reg.cacheline(95, 64))
    }.otherwise {
      inst := Mux(stage3Reg.align(0), stage3Reg.cacheline(63, 32), stage3Reg.cacheline(31, 0))
    }
  }.elsewhen((state === data && rdataFire && io.master.rlast && !io.flushIn) || state === stall) {
    when(stage3Reg.cacheable) {
      inst := Mux(stage3Reg.align(0), rblockBuffer(63, 32), rblockBuffer(31, 0))
    }.otherwise {
      inst := io.master.rdata(31, 0)
    }
  }.otherwise {
    inst := Instruction.NOP
  }
  io.out.inst := inst
  io.out.pc   := stage3Reg.pc

  val validReg      = RegInit(false.B)
  validReg         := Mux(io.stallIn, validReg, Mux(io.flushIn || flushReg, false.B, Mux(io.stallOut, validReg, io.validIn)))    
  io.validOut      := validReg && ((state === idle && stage3Reg.hit && stage3Reg.cacheable) || (state === data && rdataFire && io.master.rlast) || state === stall)
}

class ICache extends Module {
  val io = IO(new Bundle {
    // todo: 是否需要valid信号来提示是一条有效指令?
    val validIn         = Input(Bool())
    val validOut        = Output(Bool())

    val debugIn         = if(Settings.get("sim")) Some(Flipped(new DebugIO)) else None
    val debugOut        = if(Settings.get("sim")) Some(new DebugIO) else None

    // cpu
    val cpu2cache       = Flipped(new Stage1IO)
    val cache2cpu       = new InstrFetch
    val redirect        = new RedirectIO

    // ram, dataArray
    val sram0           = new RamIO
    val sram1           = new RamIO
    val sram2           = new RamIO
    val sram3           = new RamIO

    // axi master
    val master         = new AxiMaster

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
  stage2.io.validIn     <> io.validIn
  stage3.io.validIn     <> stage2.io.validOut
  io.validOut           <> stage3.io.validOut

  if(Settings.get("sim")) {
    stage2.io.debugIn.get   <> io.debugIn.get
    stage3.io.debugIn.get   <> stage2.io.debugOut.get
    stage3.io.debugOut.get  <> io.debugOut.get
  }

  io.cpu2cache          <> stage1.io.toStage1
  stage1.io.toStage2    <> stage2.io.toStage2
  stage2.io.toStage3    <> stage3.io.toStage3
  io.cache2cpu          <> stage3.io.out
  io.redirect.brAddr    := stage2.io.toStage3.pc
  io.redirect.valid     := stage3.io.flushOut

  stage1.io.stall       := io.stallIn | stage3.io.stallOut | io.flush
  stage2.io.flushIn     := io.flush | stage3.io.flushOut
  stage2.io.stallIn     := io.stallIn
  stage2.io.stage3Stall := stage3.io.stallOut
  stage2.io.sram0_rdata <> io.sram0.rdata
  stage2.io.sram1_rdata <> io.sram1.rdata
  stage2.io.sram2_rdata <> io.sram2.rdata
  stage2.io.sram3_rdata <> io.sram3.rdata
  stage2.io.metaAlloc   <> stage3.io.metaAlloc
  stage3.io.stallIn     := io.stallIn
  stage3.io.flushIn     := io.flush
  io.stallOut           := stage3.io.stallOut

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

  io.sram0.addr          := dataArb0.io.arbAddr
  io.sram0.cen           := dataArb0.io.arbCen
  io.sram0.wen           := dataArb0.io.arbWen
  io.sram0.wmask         := stage3.io.sram0_wmask
  io.sram0.wdata         := stage3.io.sram0_wdata
  io.sram1.addr          := dataArb1.io.arbAddr
  io.sram1.cen           := dataArb1.io.arbCen
  io.sram1.wen           := dataArb1.io.arbWen
  io.sram1.wmask         := stage3.io.sram1_wmask
  io.sram1.wdata         := stage3.io.sram1_wdata
  io.sram2.addr          := dataArb2.io.arbAddr
  io.sram2.cen           := dataArb2.io.arbCen
  io.sram2.wen           := dataArb2.io.arbWen
  io.sram2.wmask         := stage3.io.sram2_wmask
  io.sram2.wdata         := stage3.io.sram2_wdata
  io.sram3.addr          := dataArb3.io.arbAddr
  io.sram3.cen           := dataArb3.io.arbCen
  io.sram3.wen           := dataArb3.io.arbWen
  io.sram3.wmask         := stage3.io.sram3_wmask
  io.sram3.wdata         := stage3.io.sram3_wdata

  io.master             <> stage3.io.master
  io.axiReq             <> stage3.io.axiReq
  io.axiGrant           <> stage3.io.axiGrant
  io.axiReady           <> stage3.io.axiReady
}
