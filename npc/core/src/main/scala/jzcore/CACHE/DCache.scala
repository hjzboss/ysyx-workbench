package jzcore

import chisel3._
import chisel3.util._
import utils._

class ArbiterIO extends Bundle {
  //val cen   = Output(Vec(len, Bool())) // valid & dirty
  val no    = Output(UInt(2.W))
  val index = Output(UInt(6.W))
  val tag   = Output(UInt(22.W))
}

// 一致性写回的多路选择器（仲裁）
sealed class CohArbiter(len: Int) extends Module {
  val io = IO(new Bundle {
    val in = Vec(len, Flipped(Decoupled(new ArbiterIO)))
    val out = Decoupled(new ArbiterIO)
  })

  val arbiter = Module(new Arbiter(new ArbiterIO, len))
  arbiter.io.in <> io.in
  arbiter.io.out <> io.out
}

// todo: fencei指令的处理，cache流水化改造
// dataArray = 4KB, 4路组相连, 64个组，一个块16B
class DCache extends Module {
  val io = IO(new Bundle {
    // cpu
    val ctrlIO          = Flipped(Decoupled(new CacheCtrlIO))
    val wdataIO         = Flipped(Decoupled(new CacheWriteIO))
    val rdataIO         = Decoupled(new CacheReadIO)
    val coherence       = Flipped(new CoherenceIO)

    // ram, dataArray
    val sram4_rdata     = Input(UInt(128.W))
    val sram4_cen       = Output(Bool())
    val sram4_wen       = Output(Bool())
    val sram4_wmask     = Output(UInt(128.W))
    val sram4_addr      = Output(UInt(6.W))
    val sram4_wdata     = Output(UInt(128.W)) 

    val sram5_rdata     = Input(UInt(128.W))
    val sram5_cen       = Output(Bool())
    val sram5_wen       = Output(Bool())
    val sram5_wmask     = Output(UInt(128.W))
    val sram5_addr      = Output(UInt(6.W))
    val sram5_wdata     = Output(UInt(128.W)) 

    val sram6_rdata     = Input(UInt(128.W))
    val sram6_cen       = Output(Bool())
    val sram6_wen       = Output(Bool())
    val sram6_wmask     = Output(UInt(128.W))
    val sram6_addr      = Output(UInt(6.W))
    val sram6_wdata     = Output(UInt(128.W)) 

    val sram7_rdata     = Input(UInt(128.W))
    val sram7_cen       = Output(Bool())
    val sram7_wen       = Output(Bool())
    val sram7_wmask     = Output(UInt(128.W))
    val sram7_addr      = Output(UInt(6.W))
    val sram7_wdata     = Output(UInt(128.W)) 

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

  val hit                = dontTouch(WireDefault(false.B))
  val dirty              = dontTouch(WireDefault(false.B))
  val addr               = RegInit(0.U(32.W))
  val wen                = RegInit(false.B)
  val tag                = Wire(UInt(22.W))
  val index              = Wire(UInt(6.W))
  val align              = Wire(Bool())
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
  val coherenceFire      = io.coherence.valid && io.coherence.ready

  // cache state machine，cacheable access
  val idle :: tagCompare :: data :: writeback1 :: writeback2 :: allocate1 :: allocate2 :: addr_trans :: data_trans :: wait_resp :: ok :: coherence1 :: coherence2 :: Nil = Enum(13)
  val okay :: exokay :: slverr :: decerr :: Nil = Enum(4) // rresp
  val state = RegInit(idle)
  state := MuxLookup(state, idle, List(
    idle        -> Mux(io.coherence.valid, coherence1, Mux(ctrlFire && io.ctrlIO.bits.cacheable, tagCompare, idle)),
    tagCompare  -> Mux(hit, data, Mux(dirty, writeback1, allocate1)),
    data        -> Mux(crdataFire || cwdataFire, idle, data),
    writeback1  -> Mux(waddrFire && io.axiGrant, writeback2, writeback1), // addr
    writeback2  -> Mux(brespFire && io.axiBrespIO.bits.bresp === okay, Mux(io.coherence.valid, coherence1, allocate1), writeback2), // data and resp
    allocate1   -> Mux(raddrFire && io.axiGrant, allocate2, allocate1), // addr 
    allocate2   -> Mux(rdataFire && io.axiRdataIO.bits.rlast && io.axiRdataIO.bits.rresp === okay, data, allocate2), // data
    coherence1  -> Mux(coherenceFire, idle, coherence2),
    coherence2  -> writeback1, // data array read
  ))

  // not cacheable access
  val rState = RegInit(idle)
  rState := MuxLookup(rState, idle, List(
    idle        -> Mux(!io.ctrlIO.bits.cacheable && ctrlFire && !io.ctrlIO.bits.wen, addr_trans, idle),
    addr_trans  -> Mux(raddrFire && io.axiGrant, data_trans, addr_trans),
    data_trans  -> Mux(rdataFire && io.axiRdataIO.bits.rresp === okay, Mux(crdataFire, idle, ok), data_trans), // todo: 要等待cpu和cache握手完毕，而不是和axi总线
    ok          -> Mux(crdataFire, idle, ok)
  ))

  val wState = RegInit(idle)
  wState := MuxLookup(wState, idle, List(
    idle        -> Mux(!io.ctrlIO.bits.cacheable && ctrlFire && io.ctrlIO.bits.wen, addr_trans, idle),
    addr_trans  -> Mux(waddrFire && io.axiGrant, Mux(wdataFire, wait_resp, data_trans), addr_trans),
    data_trans  -> Mux(wdataFire, wait_resp, data_trans),
    wait_resp   -> Mux(brespFire && io.axiBrespIO.bits.bresp === okay, Mux(cwdataFire, idle, ok), wait_resp), // todo: 要等待cpu和cache握手完毕，而不是和axi总线
    ok          -> Mux(cwdataFire, idle, ok)
  ))

  victimWay          := Mux(state === tagCompare, randCount, victimWay)

  // meta data
  val metaInit        = Wire(new MetaData)
  metaInit.valid     := false.B
  metaInit.dirty     := false.B
  metaInit.tag       := 0.U(22.W)
  val metaArray       = List.fill(4)(RegInit(VecInit(Seq.fill(64)(metaInit))))
  
  // ---------------------------address decode-----------------------------------------
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

  // -----------------------------------coherence---------------------------------------
  val arbList64 = List.fill(4)(Module(new CohArbiter(64)))
  val dirtyArray = List.fill(4)(VecInit(List.fill(64)(false.B)))
  for(i <- 0 to 63; j <- 0 to 3) {
    dirtyArray(j)(i) := metaArray(j)(i).valid & metaArray(j)(i).dirty
  }

  val arbIOList64 = List.fill(4)(Wire(Vec(64, Decoupled(new ArbiterIO))))
  //val arb64Index = List.fill(4)(dontTouch(Wire(Vec(64, UInt(6.W)))))
  //val arb64Tag   = List.fill(4)(dontTouch(Wire(Vec(64, UInt(22.W)))))
  //val arb64No    = List.fill(4)(dontTouch(Wire(Vec(64, UInt(2.W)))))
  for(i <- 0 to 3) {
    for(j <- 0 to 63) {
      arbIOList64(i)(j).valid := dirtyArray(i)(j)
      arbIOList64(i)(j).bits.no := i.U(2.W)
      arbIOList64(i)(j).bits.tag := metaArray(i)(j).tag
      arbIOList64(i)(j).bits.index := j.U(6.W)
      //arb64Index(i)(j) := j.U(6.W)
      //arb64Tag(i)(j)   := metaArray(i)(j).tag
      //arb64No(i)(j)    := i.U(2.W)
    }
  }
  (0 to 3).map(i => (arbList64(i).io.in <> arbIOList64(i)))
  (0 to 3).map(i => (arbList64(i).io.out.ready := true.B))
  //(0 to 3).map(i => (arbList64(i).io.cenIn := dirtyArray(i)))
  //(0 to 3).map(i => (arbList64(i).io.noIn := arb64No(i)))
  //(0 to 3).map(i => (arbList64(i).io.indexIn := arb64Index(i)))
  //(0 to 3).map(i => (arbList64(i).io.tagIn := arb64Tag(i)))

  val arb4 = Module(new CohArbiter(4))
  //val arb4CenIn = VecInit(List.fill(4)(false.B))
  //val arb4TagIn = VecInit(List.fill(4)(0.U(22.W)))
  //val arb4NoIn  = VecInit(List.fill(4)(0.U(2.W)))
  //val arb4IndexIn = VecInit(List.fill(4)(0.U(6.W)))
  //(0 to 3).map(i => (arb4CenIn(i) := arbList64(i).io.cenOut))
  //(0 to 3).map(i => (arb4TagIn(i) := arbList64(i).io.tagOut))
  //(0 to 3).map(i => (arb4NoIn(i)  := arbList64(i).io.noOut))
  //(0 to 3).map(i => (arb4IndexIn(i) := arbList64(i).io.indexOut))
  //arb4.io.cenIn := arb4CenIn
  //arb4.io.indexIn := arb4IndexIn
  //arb4.io.noIn := arb4NoIn
  //arb4.io.tagIn := arb4TagIn
  val arb4IO = Wire(Vec(4, Decoupled(new ArbiterIO)))
  (0 to 3).map(i => (arb4IO(i) := arbList64(i).io.out))
  arb4.io.in <> arb4IO
  arb4.io.out.ready := true.B

  val ramCenPre = WireDefault(0.U(4.W))
  ramCenPre := LookupTree(arb4.io.out.bits.no, List(
    0.U -> 1.U,
    1.U -> 2.U,
    2.U -> 4.U,
    3.U -> 8.U
  ))
  val ramCen = VecInit(List.fill(4)(false.B))
  (0 to 3).map(i => (ramCen(i) := ramCenPre(i) & arb4.io.out.valid))

  val colTagReg = RegInit(0.U(22.W))
  val colIndexReg = RegInit(0.U(6.W))
  val colNoReg  = RegInit(0.U(2.W))
  colTagReg := Mux(state === coherence2, arb4.io.out.bits.tag, colTagReg)
  colIndexReg := Mux(state === coherence2, arb4.io.out.bits.index, colIndexReg)
  colNoReg  := Mux(state === coherence2, arb4.io.out.bits.no, colNoReg)

  val colOver = Wire(Bool()) // coherence over
  colOver := !(arbList64(0).io.out.valid | arbList64(1).io.out.valid | arbList64(2).io.out.valid | arbList64(3).io.out.valid)
  io.coherence.ready := colOver

  // dataArray lookup
  val dataBlock = RegInit(0.U(128.W))
  when(state === tagCompare) {
    when(hit) {
      dataBlock := LookupTree(hitList.asUInt, List(
                    "b0001".U   -> io.sram4_rdata,
                    "b0010".U   -> io.sram5_rdata,
                    "b0100".U   -> io.sram6_rdata,
                    "b1000".U   -> io.sram7_rdata,
                  ))
    }.otherwise {
      // random choose
      dataBlock := LookupTree(randCount, List(
                    0.U   -> io.sram4_rdata,
                    1.U   -> io.sram5_rdata,
                    2.U   -> io.sram6_rdata,
                    3.U   -> io.sram7_rdata,
                  ))
    }
  }.elsewhen(state === coherence2) {
    dataBlock := LookupTree(ramCen.asUInt, List(
                    0.U   -> dataBlock,
                    1.U   -> io.sram4_rdata,
                    2.U   -> io.sram5_rdata,
                    4.U   -> io.sram6_rdata,
                    8.U   -> io.sram7_rdata,
                  ))
  }.otherwise {
    dataBlock := dataBlock
  }

  // flush mate array
  when(state === writeback2 && brespFire && io.coherence.valid) {
    switch(colNoReg) {
      is(0.U) {
        metaArray(0)(colIndexReg).valid := false.B
        metaArray(0)(colIndexReg).dirty := false.B
      }
      is(1.U) {
        metaArray(1)(colIndexReg).valid := false.B
        metaArray(1)(colIndexReg).dirty := false.B
      }
      is(2.U) {
        metaArray(2)(colIndexReg).valid := false.B
        metaArray(2)(colIndexReg).dirty := false.B
      }
      is(3.U) {
        metaArray(3)(colIndexReg).valid := false.B
        metaArray(3)(colIndexReg).dirty := false.B
      }
    }
  }

  // ----------------------------write back and allocate--------------------------------
  val allocTag = RegInit(false.B)
  allocTag := Mux(state === allocate1, true.B, Mux(state === idle, false.B, allocTag))

  // axi
  io.axiReq := state === writeback1 || state === allocate1 || rState === addr_trans || wState === addr_trans // todo: 可以提前申请总线请求
  io.axiReady := ((state === allocate2 || rState === data_trans) && rdataFire && io.axiRdataIO.bits.rlast) || (wState === wait_resp && brespFire)

  val burstAddr            = addr & "hfffffff8".U

  // allocate axi, burst read
  io.axiRaddrIO.valid     := state === allocate1 || rState === addr_trans
  //io.axiRaddrIO.bits.addr := burstAddr
  io.axiRaddrIO.bits.addr := Mux(io.ctrlIO.bits.cacheable, burstAddr, addr)
  //io.axiRaddrIO.bits.len  := 1.U(8.W) // 2
  io.axiRaddrIO.bits.len  := Mux(rState === addr_trans, 0.U(8.W), 1.U(8.W))
  //io.axiRaddrIO.bits.size := Mux(io.ctrlIO.bits.cacheable, 3.U(3.W), 2.U(3.W)) // todo: 根据lbu这些类型来决定，当访问外设的时候
  io.axiRaddrIO.bits.size := Mux(io.ctrlIO.bits.cacheable, 3.U(3.W), io.ctrlIO.bits.size)
  //io.axiRaddrIO.bits.burst:= 2.U(2.W) // wrap
  io.axiRaddrIO.bits.burst:= Mux(io.ctrlIO.bits.cacheable, 2.U, 0.U)
  io.axiRdataIO.ready     := state === allocate2 || rState === data_trans

  // 锁存axi读取的值
  val axiDataReg           = RegInit(0.U(64.W))
  axiDataReg              := Mux(rState === data_trans && rdataFire, io.axiRdataIO.bits.rdata, axiDataReg)

  val rblockBuffer         = RegInit(0.U(64.W))
  rblockBuffer            := MuxLookup(state, rblockBuffer, List(
                              allocate1 -> 0.U(64.W),
                              allocate2 -> Mux(rdataFire && !io.axiRdataIO.bits.rlast, io.axiRdataIO.bits.rdata, rblockBuffer)
                            ))
  val wburst = RegInit(0.U(2.W))
  when(state === tagCompare || state === coherence2) {
    wburst := 0.U(2.W)
  }.elsewhen(state === writeback1 && wdataFire && waddrFire && io.axiGrant) {
    wburst := wburst + 1.U(2.W)
  }.elsewhen(state === writeback2 && (wburst === 1.U(2.W) || wburst === 0.U(2.W)) && wdataFire) {
    wburst := wburst + 1.U(2.W)
  }.otherwise {
    wburst := wburst
  }

  io.axiWaddrIO.valid     := state === writeback1 || wState === addr_trans
  //io.axiWaddrIO.bits.addr := burstAddr
  //io.axiWaddrIO.bits.addr := Mux(state === writeback1 || state === writeback2, Cat(wtag, burstAddr(9, 0)), burstAddr)
  io.axiWaddrIO.bits.addr := Mux(state === writeback1 || state === writeback2, Mux(io.coherence.valid, Cat(colTagReg, colIndexReg, 0.U(4.W)), Cat(wtag, burstAddr(9, 0))), Mux(io.ctrlIO.bits.cacheable, burstAddr, addr))
  //io.axiWaddrIO.bits.len  := 1.U(8.W) // 2
  io.axiWaddrIO.bits.len  := Mux(io.ctrlIO.bits.cacheable, 1.U(8.W), 0.U(8.W))
  //io.axiWaddrIO.bits.size := 3.U(3.W) // 8B, todo， 外设不能超过4字节的请求
  io.axiWaddrIO.bits.size := Mux(io.ctrlIO.bits.cacheable, 3.U, io.ctrlIO.bits.size)
  //io.axiWaddrIO.bits.burst:= 2.U(2.W) // wrap, todo, 不能向外设发送burst
  io.axiWaddrIO.bits.burst:= Mux(io.ctrlIO.bits.cacheable, 2.U, 0.U)
  io.axiWdataIO.valid     := state === writeback1 || state === writeback2 || wState === addr_trans || wState === data_trans
  io.axiWdataIO.bits.wlast:= (state === writeback2 && wburst === 1.U(2.W)) || wState === addr_trans || wState === data_trans 
  //io.axiWdataIO.bits.wstrb:= "b11111111".U
  io.axiWdataIO.bits.wstrb:= Mux(wState === addr_trans || wState === data_trans, io.wdataIO.bits.wmask, "b11111111".U)

  io.axiBrespIO.ready     := (state === writeback2 && wburst === 2.U(2.W)) || wState === wait_resp
  
  // burst write
  when(state === writeback1 || (state === writeback2 && wburst === 0.U(2.W))) {
    io.axiWdataIO.bits.wdata := Mux(align && !io.coherence.valid, dataBlock(127, 64), dataBlock(63, 0))
  }.elsewhen(state === writeback2 && wburst === 1.U(2.W)) {
    io.axiWdataIO.bits.wdata := Mux(align && !io.coherence.valid, dataBlock(63, 0), dataBlock(127, 64))
  }.elsewhen(wState === addr_trans || wState === data_trans) {
    io.axiWdataIO.bits.wdata := io.wdataIO.bits.wdata
  }.otherwise {
    io.axiWdataIO.bits.wdata := 0.U(64.W)
  }

  val alignMask0   = Mux(align, "hffffffffffffffff".U(128.W), ~"hffffffffffffffff".U(128.W))
  val alignMask1   = Mux(align, ~"hffffffffffffffff".U(128.W), "hffffffffffffffff".U(128.W))
  val rdata0       = Mux(align, Cat(io.axiRdataIO.bits.rdata, 0.U(64.W)), Cat(0.U(64.W), io.axiRdataIO.bits.rdata))
  val rdata1       = Mux(align, Cat(0.U(64.W), io.axiRdataIO.bits.rdata), Cat(io.axiRdataIO.bits.rdata, 0.U(64.W)))

  // dataArray control
  io.sram4_addr   := index
  io.sram4_wen    := true.B
  io.sram5_addr   := index
  io.sram5_wen    := true.B
  io.sram6_addr   := index
  io.sram6_wen    := true.B
  io.sram7_addr   := index
  io.sram7_wen    := true.B
  io.sram4_cen    := true.B
  io.sram5_cen    := true.B
  io.sram6_cen    := true.B
  io.sram7_cen    := true.B
  io.sram4_wdata  := 0.U
  io.sram4_wmask  := ~0.U(128.W)
  io.sram5_wdata  := 0.U
  io.sram5_wmask  := ~0.U(128.W)
  io.sram6_wdata  := 0.U
  io.sram6_wmask  := ~0.U(128.W)
  io.sram7_wdata  := 0.U
  io.sram7_wmask  := ~0.U(128.W)
  when(state === coherence1) {
    // coherence
    io.sram4_addr := arb4.io.out.bits.index
    io.sram4_cen  := !ramCen(0)
    io.sram5_addr := arb4.io.out.bits.index
    io.sram5_cen  := !ramCen(1)
    io.sram6_addr := arb4.io.out.bits.index
    io.sram6_cen  := !ramCen(2)
    io.sram7_addr := arb4.io.out.bits.index
    io.sram7_cen  := !ramCen(3)
  }.elsewhen(state === idle && ctrlFire) {
    // read data
    io.sram4_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram4_cen  := false.B
    io.sram5_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram5_cen  := false.B
    io.sram6_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram6_cen  := false.B
    io.sram7_addr := io.ctrlIO.bits.addr(9, 4)
    io.sram7_cen  := false.B
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
        io.sram4_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram4_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram4_cen    := false.B
        io.sram4_wen    := false.B
      }
      is(1.U) {
        metaArray(1)(index) := metaAlloc
        io.sram5_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram5_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram5_cen    := false.B
        io.sram5_wen    := false.B
      }
      is(2.U) {
        metaArray(2)(index) := metaAlloc
        io.sram6_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram6_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram6_cen    := false.B
        io.sram6_wen    := false.B
      }
      is(3.U) {
        metaArray(3)(index) := metaAlloc
        io.sram7_wdata  := Mux(io.axiRdataIO.bits.rlast, rdata1, rdata0)
        io.sram7_wmask  := Mux(io.axiRdataIO.bits.rlast, alignMask1, alignMask0)
        io.sram7_cen    := false.B
        io.sram7_wen    := false.B
      }
    }
  }.elsewhen(state === data && cwdataFire && wen) {
    // write dataArray
    // wmask8 to wmask64
    val wmask64 = Wire(Vec(8, UInt(8.W)))
    (0 to 7).map(i => (wmask64(i) := Mux(io.wdataIO.bits.wmask(i), 0.U(8.W), "hff".U)))
    // write enable
    when(allocTag) {
      switch(victimWay) {
        is(0.U) {
          metaArray(0)(index).dirty := true.B
          io.sram4_wen    := false.B
          io.sram4_cen    := false.B
          io.sram4_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram4_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is(1.U) {
          metaArray(1)(index).dirty := true.B
          io.sram5_wen    := false.B
          io.sram5_cen    := false.B
          io.sram5_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram5_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is(2.U) {
          metaArray(2)(index).dirty := true.B
          io.sram6_wen    := false.B
          io.sram6_cen    := false.B
          io.sram6_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram6_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is(3.U) {
          metaArray(3)(index).dirty := true.B
          io.sram7_wen    := false.B
          io.sram7_cen    := false.B
          io.sram7_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram7_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
      }
    }.otherwise {
      switch(hitListReg.asUInt) {
        is("b0001".U) {
          metaArray(0)(index).dirty := true.B
          io.sram4_cen    := false.B
          io.sram4_wen    := false.B
          io.sram4_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram4_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is("b0010".U) {
          metaArray(1)(index).dirty := true.B
          io.sram5_cen    := false.B
          io.sram5_wen    := false.B
          io.sram5_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram5_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is("b0100".U) {
          metaArray(2)(index).dirty := true.B
          io.sram6_cen    := false.B
          io.sram6_wen    := false.B
          io.sram6_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram6_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
        }
        is("b1000".U) {
          metaArray(3)(index).dirty := true.B
          io.sram7_cen    := false.B
          io.sram7_wen    := false.B
          io.sram7_wmask  := Mux(align, Cat(wmask64.asUInt, "hffffffffffffffff".U(64.W)), Cat("hffffffffffffffff".U(64.W), wmask64.asUInt))
          io.sram7_wdata  := Mux(align, Cat(io.wdataIO.bits.wdata, 0.U(64.W)), Cat(0.U(64.W), io.wdataIO.bits.wdata))
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
  // todo: 要等待cpu和cache握手完毕，而不是和axi总线
  io.wdataIO.ready         := state === data || (wState === wait_resp && brespFire) || wState === ok

  //io.rdataIO.bits.rdata    := Mux(state === data, alignData, 0.U(64.W))
  //io.rdataIO.bits.rdata    := Mux(state === data, alignData, Mux(rState === data_trans, io.axiRdataIO.bits,rdata, 0.U(64.W)))
  io.rdataIO.valid         := state === data || (rState === data_trans && rdataFire) || rState === ok
  io.rdataIO.bits.rdata    := Mux(state === data, alignData, Mux(rState === data_trans, io.axiRdataIO.bits.rdata, Mux(rState === ok, axiDataReg, 0.U(64.W))))

  //io.ctrlIO.ready          := state === idle // todo
  io.ctrlIO.ready          := state === idle && rState === idle && wState === idle
}