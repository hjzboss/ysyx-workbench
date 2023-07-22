package jzcore

import chisel3._
import chisel3.util._
import utils._


class AxiMaster extends Bundle {
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awid    = Output(UInt(4.W))
  val awaddr  = Output(UInt(32.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))
  val wready  = Input(Bool())
  val wvalid  = Output(Bool())
  val wdata   = Output(UInt(64.W))
  val wstrb   = Output(UInt(8.W))
  val wlast   = Output(Bool())
  val bready  = Output(Bool())
  val bvalid  = Input(Bool())
  val bid     = Input(UInt(4.W))
  val bresp   = Input(UInt(2.W))
  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val arid    = Output(UInt(4.W))
  val araddr  = Output(UInt(32.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))
  val rready  = Output(Bool())
  val rvalid  = Input(Bool())
  val rid     = Input(UInt(4.W))
  val rdata   = Input(UInt(64.W))
  val rresp   = Input(UInt(2.W))
  val rlast   = Input(Bool())
}

class JzCore extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    //val debug       = new DebugIO
    //val finish      = Output(Bool())

    // icache data array
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

    // dcache data array
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

    // axi访存接口
    /*
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))*/
    val interrupt      = Input(Bool())
    val master         = new AxiMaster
    val slave          = Flipped(new AxiMaster)

    /*
    val master_awready = Input(Bool())
    val master_awvalid = Output(Bool())
    val master_awid    = Output(UInt(4.W))
    val master_awaddr  = Output(UInt(32.W))
    val master_awlen   = Output(UInt(8.W))
    val master_awsize  = Output(UInt(3.W))
    val master_awburst = Output(UInt(2.W))
    val master_wready  = Input(Bool())
    val master_wvalid  = Output(Bool())
    val master_wdata   = Output(UInt(64.W))
    val master_wstrb   = Output(UInt(8.W))
    val master_wlast   = Output(Bool())
    val master_bready  = Output(Bool())
    val master_bvalid  = Input(Bool())
    val master_bid     = Input(UInt(4.W))
    val master_bresp   = Input(UInt(2.W))
    val master_arready = Input(Bool())
    val master_arvalid = Output(Bool())
    val master_arid    = Output(UInt(4.W))
    val master_araddr  = Output(UInt(32.W))
    val master_arlen   = Output(UInt(8.W))
    val master_arsize  = Output(UInt(3.W))
    val master_arburst = Output(UInt(2.W))
    val master_rready  = Output(Bool())
    val master_rvalid  = Input(Bool())
    val master_rid     = Input(UInt(4.W))
    val master_rdata   = Input(UInt(64.W))
    val master_rresp   = Input(UInt(2.W))
    val master_rlast   = Input(Bool())

    // useless
    val slave_awready  = Input(Bool())
    val slave_awvalid  = Output(Bool())
    val slave_awid     = Output(UInt(4.W))
    val slave_awaddr   = Output(UInt(32.W))
    val slave_awlen    = Output(UInt(8.W))
    val slave_awsize   = Output(UInt(3.W))
    val slave_awburst  = Output(UInt(2.W))
    val slave_wready   = Input(Bool())
    val slave_wvalid   = Output(Bool())
    val slave_wdata    = Output(UInt(64.W))
    val slave_wstrb    = Output(UInt(8.W))
    val slave_wlast    = Output(Bool())
    val slave_bready   = Output(Bool())
    val slave_bvalid   = Input(Bool())
    val slave_bid      = Input(UInt(4.W))
    val slave_bresp    = Input(UInt(2.W))
    val slave_arready  = Input(Bool())
    val slave_arvalid  = Output(Bool())
    val slave_arid     = Output(UInt(4.W))
    val slave_araddr   = Output(UInt(32.W))
    val slave_arlen    = Output(UInt(8.W))
    val slave_arsize   = Output(UInt(3.W))
    val slave_arburst  = Output(UInt(2.W))
    val slave_rready   = Output(Bool())
    val slave_rvalid   = Input(Bool())
    val slave_rid      = Input(UInt(4.W))
    val slave_rdata    = Input(UInt(64.W))
    val slave_rresp    = Input(UInt(2.W))
    val slave_rlast    = Input(Bool())    
    */
    //val lsFlag      = Output(Bool())
  })

  val ifu     = Module(new IFU)
  val idu     = Module(new IDU)
  val exu     = Module(new EXU)
  val lsu     = Module(new LSU)
  val wbu     = Module(new WBU)
  val ctrl    = Module(new CTRL)
  val arbiter = Module(new AxiArbiter) // todo:仲裁器
  val icache  = Module(new ICache)
  val dcache  = Module(new DCache)
  //val clint   = Module(new Clint)

  val idReg   = Module(new ID_REG)
  val exReg   = Module(new EX_REG)
  val lsReg   = Module(new LS_REG)
  val wbReg   = Module(new WB_REG)
  val forward = Module(new Forwarding)

  //clint.io.clintIO    <> lsu.io.clintIO
  //clint.io.int        <> idu.io.timerInt

  //io.csrAddr  := idu.io.csrAddr
  // 仲裁
  arbiter.io.ifuReq   <> icache.io.axiReq
  arbiter.io.grantIfu <> icache.io.axiGrant
  arbiter.io.ifuReady <> icache.io.axiReady
  arbiter.io.lsuReq   <> dcache.io.axiReq
  arbiter.io.grantLsu <> dcache.io.axiGrant
  arbiter.io.lsuReady <> dcache.io.axiReady
  //arbiter.io.lsuReq   <> lsu.io.axiReq
  //arbiter.io.grantLsu <> lsu.io.axiGrant
  //arbiter.io.lsuReady <> lsu.io.axiReady

  // todo: axi总线替换，axi中burst的判断（外设无法burst），外设无法超过4字节请求
  // axi访问接口
  io.slave.awready := false.B
  io.slave.wready := false.B
  io.slave.bvalid := false.B
  io.slave.bid    := 0.U
  io.slave.bresp  := 0.U
  io.slave.arready:= false.B
  io.slave.rvalid := false.B
  io.slave.rid    := 0.U
  io.slave.rdata  := 0.U
  io.slave.rresp  := 0.U
  io.slave.rlast  := false.B
  val grant = Cat(arbiter.io.grantIfu, arbiter.io.grantLsu)
  when(grant === 2.U) {
    /*
    io.axiRaddrIO <> icache.io.axiRaddrIO
    io.axiRdataIO <> icache.io.axiRdataIO
    io.axiWaddrIO <> icache.io.axiWaddrIO
    io.axiWdataIO <> icache.io.axiWdataIO
    io.axiBrespIO <> icache.io.axiBrespIO*/
    
    io.master.awid := 0.U
    io.master.awvalid := icache.io.axiWaddrIO.valid
    io.master.awaddr := icache.io.axiWaddrIO.bits.addr
    io.master.awlen := icache.io.axiWaddrIO.bits.len
    io.master.awsize := icache.io.axiWaddrIO.bits.size
    io.master.awburst := icache.io.axiWaddrIO.bits.burst
    icache.io.axiWaddrIO.ready := io.master.awready
    icache.io.axiWdataIO.ready := io.master.wready
    io.master.wvalid := icache.io.axiWdataIO.valid
    io.master.wdata := icache.io.axiWdataIO.bits.wdata
    io.master.wstrb := icache.io.axiWdataIO.bits.wstrb
    io.master.wlast := icache.io.axiWdataIO.bits.wlast
    icache.io.axiBrespIO.valid := io.master.bvalid
    icache.io.axiBrespIO.bits.bresp := io.master.bresp
    io.master.bready := icache.io.axiBrespIO.ready
    io.master.arid := 0.U
    io.master.araddr := icache.io.axiRaddrIO.bits.addr
    io.master.arvalid := icache.io.axiRaddrIO.valid
    icache.io.axiRaddrIO.ready := io.master.arready
    io.master.arlen := icache.io.axiRaddrIO.bits.len
    io.master.arsize := icache.io.axiRaddrIO.bits.size
    io.master.arburst := icache.io.axiRaddrIO.bits.burst
    io.master.rready := icache.io.axiRdataIO.ready
    icache.io.axiRdataIO.valid := io.master.rvalid
    icache.io.axiRdataIO.bits.rdata := io.master.rdata
    icache.io.axiRdataIO.bits.rlast := io.master.rlast
    icache.io.axiRdataIO.bits.rresp := io.master.rresp

    dcache.io.axiRaddrIO.ready   := false.B
    dcache.io.axiRdataIO.valid   := false.B
    dcache.io.axiRdataIO.bits.rdata := 0.U
    dcache.io.axiRdataIO.bits.rresp   := 0.U
    dcache.io.axiRdataIO.bits.rlast   := true.B
    dcache.io.axiWaddrIO.ready   := false.B
    dcache.io.axiWdataIO.ready   := false.B
    dcache.io.axiBrespIO.valid   := false.B
    dcache.io.axiBrespIO.bits.bresp   := 0.U
    /*
    lsu.io.axiRaddrIO.ready   := false.B
    lsu.io.axiRdataIO.valid   := false.B
    lsu.io.axiRdataIO.bits.rdata := 0.U
    lsu.io.axiRdataIO.bits.rresp   := 0.U
    lsu.io.axiRdataIO.bits.rlast   := true.B
    lsu.io.axiWaddrIO.ready   := false.B
    lsu.io.axiWdataIO.ready   := false.B
    lsu.io.axiBrespIO.valid   := false.B
    lsu.io.axiBrespIO.bits.bresp   := 0.U
    */
  }
  .elsewhen(grant === 1.U || grant === 3.U) {
    /*
    io.axiRaddrIO <> dcache.io.axiRaddrIO
    io.axiRdataIO <> dcache.io.axiRdataIO
    io.axiWaddrIO <> dcache.io.axiWaddrIO
    io.axiWdataIO <> dcache.io.axiWdataIO
    io.axiBrespIO <> dcache.io.axiBrespIO*/
    /*
    io.axiRaddrIO <> lsu.io.axiRaddrIO
    io.axiRdataIO <> lsu.io.axiRdataIO
    io.axiWaddrIO <> lsu.io.axiWaddrIO
    io.axiWdataIO <> lsu.io.axiWdataIO
    io.axiBrespIO <> lsu.io.axiBrespIO   
    */
    io.master.awid := 0.U
    io.master.awvalid := dcache.io.axiWaddrIO.valid
    io.master.awaddr := dcache.io.axiWaddrIO.bits.addr
    io.master.awlen := dcache.io.axiWaddrIO.bits.len
    io.master.awsize := dcache.io.axiWaddrIO.bits.size
    io.master.awburst := dcache.io.axiWaddrIO.bits.burst
    dcache.io.axiWaddrIO.ready := io.master.awready
    dcache.io.axiWdataIO.ready := io.master.wready
    io.master.wvalid := dcache.io.axiWdataIO.valid
    io.master.wdata := dcache.io.axiWdataIO.bits.wdata
    io.master.wstrb := dcache.io.axiWdataIO.bits.wstrb
    io.master.wlast := dcache.io.axiWdataIO.bits.wlast
    dcache.io.axiBrespIO.valid := io.master.bvalid
    dcache.io.axiBrespIO.bits.bresp := io.master.bresp
    io.master.bready := dcache.io.axiBrespIO.ready
    io.master.arid := 0.U
    io.master.araddr := dcache.io.axiRaddrIO.bits.addr
    io.master.arvalid := dcache.io.axiRaddrIO.valid
    dcache.io.axiRaddrIO.ready := io.master.arready
    io.master.arlen := dcache.io.axiRaddrIO.bits.len
    io.master.arsize := dcache.io.axiRaddrIO.bits.size
    io.master.arburst := dcache.io.axiRaddrIO.bits.burst
    io.master.rready := dcache.io.axiRdataIO.ready
    dcache.io.axiRdataIO.valid := io.master.rvalid
    dcache.io.axiRdataIO.bits.rdata := io.master.rdata
    dcache.io.axiRdataIO.bits.rlast := io.master.rlast
    dcache.io.axiRdataIO.bits.rresp := io.master.rresp

    icache.io.axiRaddrIO.ready   := false.B
    icache.io.axiRdataIO.valid   := false.B
    icache.io.axiRdataIO.bits.rdata := 0.U
    icache.io.axiRdataIO.bits.rlast := true.B
    icache.io.axiRdataIO.bits.rresp   := 0.U
    icache.io.axiWaddrIO.ready   := false.B
    icache.io.axiWdataIO.ready   := false.B
    icache.io.axiBrespIO.valid   := false.B
    icache.io.axiBrespIO.bits.bresp   := 0.U
  }
  .otherwise {
    // 没有请求
    /*
    io.axiRaddrIO.valid       := false.B
    io.axiRaddrIO.bits.addr   := 0.U
    io.axiRaddrIO.bits.len    := 0.U
    io.axiRaddrIO.bits.size   := 0.U
    io.axiRaddrIO.bits.burst  := 0.U
    io.axiWaddrIO.bits.len    := 0.U
    io.axiWaddrIO.bits.size   := 0.U
    io.axiWaddrIO.bits.burst  := 0.U
    io.axiRdataIO.ready       := false.B
    io.axiWaddrIO.valid       := false.B
    io.axiWaddrIO.bits.addr   := 0.U
    io.axiWdataIO.valid       := false.B
    io.axiWdataIO.bits.wdata  := 0.U
    io.axiWdataIO.bits.wstrb  := 0.U
    io.axiWdataIO.bits.wlast  := true.B
    io.axiBrespIO.ready       := false.B*/

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
    io.master.arid := 0.U
    io.master.araddr := 0.U
    io.master.arvalid := false.B
    io.master.arlen := 0.U
    io.master.arsize := 0.U
    io.master.arburst := 0.U
    io.master.rready := false.B

    icache.io.axiRaddrIO.ready   := false.B
    icache.io.axiRdataIO.valid   := false.B
    icache.io.axiRdataIO.bits.rdata := 0.U
    icache.io.axiRdataIO.bits.rresp   := 0.U
    icache.io.axiRdataIO.bits.rlast   := true.B
    icache.io.axiWaddrIO.ready   := false.B
    icache.io.axiWdataIO.ready   := false.B
    icache.io.axiBrespIO.valid   := false.B
    icache.io.axiBrespIO.bits.bresp   := 0.U

    dcache.io.axiRaddrIO.ready   := false.B
    dcache.io.axiRdataIO.valid   := false.B
    dcache.io.axiRdataIO.bits.rdata := 0.U
    dcache.io.axiRdataIO.bits.rlast   := true.B
    dcache.io.axiRdataIO.bits.rresp   := 0.U
    dcache.io.axiWaddrIO.ready   := false.B
    dcache.io.axiWdataIO.ready   := false.B
    dcache.io.axiBrespIO.valid   := false.B
    dcache.io.axiBrespIO.bits.bresp   := 0.U
    /*
    lsu.io.axiRaddrIO.ready   := false.B
    lsu.io.axiRdataIO.valid   := false.B
    lsu.io.axiRdataIO.bits.rdata := 0.U
    lsu.io.axiRdataIO.bits.rlast   := true.B
    lsu.io.axiRdataIO.bits.rresp   := 0.U
    lsu.io.axiWaddrIO.ready   := false.B
    lsu.io.axiWdataIO.ready   := false.B
    lsu.io.axiBrespIO.valid   := false.B
    lsu.io.axiBrespIO.bits.bresp   := 0.U
    */
  }
  
  // ram, dataArray
  icache.io.sram0_rdata <> io.sram0_rdata
  icache.io.sram0_cen   <> io.sram0_cen
  icache.io.sram0_wen   <> io.sram0_wen
  icache.io.sram0_wmask <> io.sram0_wmask
  icache.io.sram0_addr  <> io.sram0_addr
  icache.io.sram0_wdata <> io.sram0_wdata

  icache.io.sram1_rdata <> io.sram1_rdata
  icache.io.sram1_cen   <> io.sram1_cen
  icache.io.sram1_wen   <> io.sram1_wen
  icache.io.sram1_wmask <> io.sram1_wmask
  icache.io.sram1_addr  <> io.sram1_addr
  icache.io.sram1_wdata <> io.sram1_wdata

  icache.io.sram2_rdata <> io.sram2_rdata
  icache.io.sram2_cen   <> io.sram2_cen
  icache.io.sram2_wen   <> io.sram2_wen
  icache.io.sram2_wmask <> io.sram2_wmask
  icache.io.sram2_addr  <> io.sram2_addr
  icache.io.sram2_wdata <> io.sram2_wdata

  icache.io.sram3_rdata <> io.sram3_rdata
  icache.io.sram3_cen   <> io.sram3_cen  //npc_cpu.pc = top->io_debug_pc;
  //npc_cpu.npc = top->io_debug_nextPc;
  icache.io.sram3_wen   <> io.sram3_wen
  icache.io.sram3_wmask <> io.sram3_wmask
  icache.io.sram3_addr  <> io.sram3_addr
  icache.io.sram3_wdata <> io.sram3_wdata

  // dcache
  dcache.io.sram4_rdata <> io.sram4_rdata
  dcache.io.sram4_cen   <> io.sram4_cen
  dcache.io.sram4_wen   <> io.sram4_wen
  dcache.io.sram4_wmask <> io.sram4_wmask
  dcache.io.sram4_addr  <> io.sram4_addr
  dcache.io.sram4_wdata <> io.sram4_wdata

  dcache.io.sram5_rdata <> io.sram5_rdata
  dcache.io.sram5_cen   <> io.sram5_cen
  dcache.io.sram5_wen   <> io.sram5_wen
  dcache.io.sram5_wmask <> io.sram5_wmask
  dcache.io.sram5_addr  <> io.sram5_addr
  dcache.io.sram5_wdata <> io.sram5_wdata

  dcache.io.sram6_rdata <> io.sram6_rdata
  dcache.io.sram6_cen   <> io.sram6_cen
  dcache.io.sram6_wen   <> io.sram6_wen
  dcache.io.sram6_wmask <> io.sram6_wmask
  dcache.io.sram6_addr  <> io.sram6_addr
  dcache.io.sram6_wdata <> io.sram6_wdata

  dcache.io.sram7_rdata <> io.sram7_rdata
  dcache.io.sram7_cen   <> io.sram7_cen
  dcache.io.sram7_wen   <> io.sram7_wen
  dcache.io.sram7_wmask <> io.sram7_wmask
  dcache.io.sram7_addr  <> io.sram7_addr
  dcache.io.sram7_wdata <> io.sram7_wdata

  ifu.io.out          <> icache.io.cpu2cache
  icache.io.cache2cpu <> idReg.io.in
  //ifu.io.out          <> idReg.io.in
  ifu.io.exuRedirect  <> exu.io.redirect
  ifu.io.icRedirect   <> icache.io.redirect
  /*
  ifu.io.valid        <> icache.io.validIn
  icache.io.validOut  <> idReg.io.validIn
  //ifu.io.icacheCtrl <> icache.io.ctrlIO
  //ifu.io.icacheRead <> icache.io.rdataIO
  //ifu.io.icacheWrite<> icache.io.wdataIO
  //idReg.io.validIn    <> ifu.io.valid
  exReg.io.validIn    <> idReg.io.validOut
  lsReg.io.validIn    <> exReg.io.validOut
  wbReg.io.validIn    <> lsReg.io.validOut*/

  // 控制模块
  //ctrl.io.ifuReady  <> ifu.io.ready
  ctrl.io.memRen      := exReg.io.ctrlOut.memRen
  ctrl.io.exRd        := exReg.io.ctrlOut.rd
  ctrl.io.rs1         := idu.io.ctrl.rs1
  ctrl.io.rs2         := idu.io.ctrl.rs2
  ctrl.io.icStall     <> icache.io.stallOut
  ctrl.io.lsuReady    <> lsu.io.ready
  ctrl.io.exuReady    <> exu.io.ready
  ctrl.io.branch      := exu.io.redirect.valid
  ctrl.io.stallICache <> icache.io.stallIn
  ctrl.io.stallIduReg <> idReg.io.stall
  ctrl.io.stallExuReg <> exReg.io.stall
  ctrl.io.stallLsuReg <> lsReg.io.stall
  ctrl.io.stallWbuReg <> wbReg.io.stall
  ctrl.io.stallExu    <> exu.io.stall
  ctrl.io.stallPc     <> ifu.io.stall
  ctrl.io.flushICache <> icache.io.flush
  ctrl.io.flushIduReg <> idReg.io.flush
  exReg.io.flush      := ctrl.io.flushExuReg
  exu.io.flush        := ctrl.io.flushExuReg
  //ctrl.io.flushExuReg <> exReg.io.flush

  forward.io.lsuRd  := lsReg.io.out.rd
  forward.io.wbuRd  := wbReg.io.out.rd
  forward.io.lsuRegWen := lsReg.io.out.regWen
  forward.io.wbuRegWen := wbReg.io.out.regWen
  forward.io.rs1    := exReg.io.ctrlOut.rs1
  forward.io.rs2    := exReg.io.ctrlOut.rs2
  forward.io.wbuCsrWen := wbReg.io.out.csrWen
  forward.io.wbuCsrAddr := wbReg.io.out.csrWaddr
  forward.io.lsuCsrWen := lsReg.io.out.csrWen
  forward.io.lsuCsrAddr:= lsReg.io.out.csrWaddr
  forward.io.csrRen   := exReg.io.ctrlOut.csrRen
  forward.io.csrRaddr := exReg.io.ctrlOut.csrWaddr

  idu.io.in         <> idReg.io.out
  idu.io.regWrite   <> wbu.io.regWrite
  idu.io.csrWrite   <> wbu.io.csrWrite
  idu.io.datasrc    <> exReg.io.datasrcIn
  idu.io.aluCtrl    <> exReg.io.aluCtrlIn
  idu.io.ctrl       <> exReg.io.ctrlIn

  exu.io.datasrc    <> exReg.io.datasrcOut
  exu.io.aluCtrl    <> exReg.io.aluCtrlOut
  exu.io.ctrl       <> exReg.io.ctrlOut
  exu.io.lsuForward := lsReg.io.out.exuOut
  exu.io.wbuForward := wbu.io.regWrite.value // 可能三lsuout或者exuout
  exu.io.csrWbuForward := wbReg.io.out.csrValue
  exu.io.csrLsuForward := lsReg.io.out.csrValue
  exu.io.out        <> lsReg.io.in
  exu.io.forwardA   <> forward.io.forwardA
  exu.io.forwardB   <> forward.io.forwardB

  lsu.io.dcacheCtrl <> dcache.io.ctrlIO
  lsu.io.dcacheRead <> dcache.io.rdataIO
  lsu.io.dcacheWrite<> dcache.io.wdataIO
  lsu.io.dcacheCoh  <> dcache.io.coherence
  lsu.io.in         <> lsReg.io.out
  lsu.io.out        <> wbReg.io.in

  wbu.io.in         <> wbReg.io.out
  /*
  wbReg.io.lsFlagIn <> lsu.io.lsFlag
  io.lsFlag         <> wbReg.io.lsFlagOut // 仿真环境

  ifu.io.debug      <> icache.io.debugIn
  idReg.io.debugIn  <> icache.io.debugOut
  //idReg.io.debugIn  <> ifu.io.debug
  exReg.io.debugIn  <> idReg.io.debugOut
  exu.io.debugIn    <> exReg.io.debugOut
  lsReg.io.debugIn  <> exu.io.debugOut
  wbReg.io.debugIn  <> lsReg.io.debugOut
  io.debug          <> wbReg.io.debugOut
  io.finish         <> wbReg.io.validOut*/
}