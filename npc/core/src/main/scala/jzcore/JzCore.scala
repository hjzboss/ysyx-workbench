package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings


class JzCore extends Module {
  val io = IO(new Bundle {
    val debug           = if(Settings.get("sim")) Some(new DebugIO) else None
    val lsFlag          = if(Settings.get("sim")) Some(Output(Bool())) else None

    // icache data array
    val sram0           = new RamIO
    val sram1           = new RamIO
    val sram2           = new RamIO
    val sram3           = new RamIO

    // dcache data array
    val sram4           = new RamIO
    val sram5           = new RamIO
    val sram6           = new RamIO
    val sram7           = new RamIO

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
  val clint   = Module(new Clint)

  val idReg   = Module(new ID_REG)
  val exReg   = Module(new EX_REG)
  val lsReg   = Module(new LS_REG)
  val wbReg   = Module(new WB_REG)
  val forward = Module(new Forwarding)

  clint.io.clintIO    <> lsu.io.clintIO
  clint.io.int        <> idu.io.timerInt

  //io.csrAddr  := idu.io.csrAddr
  // 仲裁
  arbiter.io.ifuReq   <> icache.io.axiReq
  arbiter.io.grantIfu <> icache.io.axiGrant
  arbiter.io.ifuReady <> icache.io.axiReady
  arbiter.io.lsuReq   <> dcache.io.axiReq
  arbiter.io.grantLsu <> dcache.io.axiGrant
  arbiter.io.lsuReady <> dcache.io.axiReady
  arbiter.io.master0  <> icache.io.master
  arbiter.io.master1  <> dcache.io.master
  io.master           <> arbiter.io.master

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

  /*
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
  }*/
  
  // ram, dataArray
  icache.io.sram0       <> io.sram0
  icache.io.sram1       <> io.sram1
  icache.io.sram2       <> io.sram2
  icache.io.sram3       <> io.sram3

  // dcache
  dcache.io.sram4       <> io.sram4
  dcache.io.sram5       <> io.sram5
  dcache.io.sram6       <> io.sram6
  dcache.io.sram7       <> io.sram7

  ifu.io.out          <> icache.io.cpu2cache
  icache.io.cache2cpu <> idReg.io.in
  //ifu.io.out          <> idReg.io.in
  ifu.io.exuRedirect  <> exu.io.redirect
  ifu.io.icRedirect   <> icache.io.redirect

  ifu.io.valid        <> icache.io.validIn
  icache.io.validOut  <> idReg.io.validIn
  idReg.io.validOut   <> idu.io.validIn
  //ifu.io.icacheCtrl <> icache.io.ctrlIO
  //ifu.io.icacheRead <> icache.io.rdataIO
  //ifu.io.icacheWrite<> icache.io.wdataIO

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
  idReg.io.stall      := ctrl.io.stallIduReg
  idu.io.stall        := ctrl.io.stallIduReg
  ctrl.io.stallExuReg <> exReg.io.stall
  ctrl.io.stallLsuReg <> lsReg.io.stall
  ctrl.io.stallWbuReg <> wbReg.io.stall
  ctrl.io.stallExu    <> exu.io.stall
  ctrl.io.stallPc     <> ifu.io.stall
  ctrl.io.flushICache <> icache.io.flush
  idReg.io.flush      := ctrl.io.flushIduReg
  idu.io.flush        := ctrl.io.flushIduReg
  idReg.io.flush      <> ctrl.io.flushIduReg
  exReg.io.flush      := ctrl.io.flushExuReg
  exu.io.flush        := ctrl.io.flushExuReg

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
  forward.io.mret     <> idu.io.mret
  forward.io.flushExuCsr <> exu.io.flushCsr
  forward.io.flushLsuCsr <> lsu.io.flushCsr
  forward.io.flushWbuCsr <> wbu.io.flushCsr
  forward.io.lsuException := lsReg.io.out.exception
  forward.io.wbuException := wbReg.io.out.exception

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
  exu.io.wbuForward := wbu.io.regWrite.value
  exu.io.csrWbuForward := wbReg.io.out.csrValue
  exu.io.csrLsuForward := lsReg.io.out.csrValue
  exu.io.out        <> lsReg.io.in
  exu.io.forwardA   <> forward.io.forwardA
  exu.io.forwardB   <> forward.io.forwardB
  exu.io.wbuMepc    := wbReg.io.out.pc
  exu.io.lsuMepc    := lsReg.io.out.pc
  exu.io.lsuNo      := lsReg.io.out.excepNo
  exu.io.wbuNo      := wbReg.io.out.excepNo

  lsu.io.dcacheCtrl <> dcache.io.ctrlIO
  lsu.io.dcacheRead <> dcache.io.rdataIO
  lsu.io.dcacheWrite<> dcache.io.wdataIO
  lsu.io.dcacheCoh  <> dcache.io.coherence
  lsu.io.in         <> lsReg.io.out
  lsu.io.out        <> wbReg.io.in

  wbu.io.in         <> wbReg.io.out
  
  if(Settings.get("sim")) {
    wbReg.io.lsFlagIn.get <> lsu.io.lsFlag.get
    io.lsFlag.get         <> wbReg.io.lsFlagOut.get

    ifu.io.debug.get      <> icache.io.debugIn.get
    idReg.io.debugIn.get  <> icache.io.debugOut.get
    exReg.io.debugIn.get  <> idReg.io.debugOut.get
    exu.io.debugIn.get    <> exReg.io.debugOut.get
    lsReg.io.debugIn.get  <> exu.io.debugOut.get
    wbReg.io.debugIn.get  <> lsReg.io.debugOut.get
    io.debug.get          <> wbReg.io.debugOut.get
  }
}