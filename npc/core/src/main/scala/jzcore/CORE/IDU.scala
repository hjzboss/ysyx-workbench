package jzcore

import chisel3._
import chisel3.util._
import utils._

class IDU extends Module with HasInstrType{
  val io = IO(new Bundle {
    // 来自ifu
    val in        = Flipped(new InstrFetch)

    // 来自exu
    val regWrite  = Flipped(new RFWriteIO)
    val csrWrite  = Flipped(new CSRWriteIO)

/*
    // 旁路控制信号
    val forwardA    = Input(UInt(2.W))
    val forwardB    = Input(UInt(2.W))

    // 旁路数据
    val exuForward  = Input(UInt(64.W))
    val lsuForward  = Input(UInt(64.W))
    val csrForward  = Input(UInt(64.W))
*/

    // 送给exu的控制信号
    val datasrc   = new DataSrcIO
    val aluCtrl   = new AluIO
    val ctrl      = new CtrlFlow
  })

  val rf        = Module(new RF)
  val csrReg    = Module(new CsrReg)
  //val brAlu     = Module(new BrAlu)

  val inst      = io.in.inst
  val op        = inst(6, 0)
  val rs1       = inst(19, 15)
  val rs2       = inst(24, 20)
  val rd        = inst(11, 7)
  val csr       = inst(31, 20)

  // 译码
  val ctrlList  = ListLookup(inst, Instruction.DecodeDefault, RV64IM.table)
  val lsctrl    = ListLookup(inst, Instruction.LsDefault, RV64IM.lsTypeTable)
  val instrtype = ctrlList(0)
  val aluOp     = ctrlList(3)
  val aluSrc1   = ctrlList(1)
  val aluSrc2   = ctrlList(2)
  val lsType    = dontTouch(Wire(UInt(4.W))) // 防止信号被优化
  lsType       := lsctrl(0)
  val loadMem   = lsctrl(2)
  val wmask     = lsctrl(1)
  val memEn     = lsctrl(3)
  val imm       = LookupTree(instrtype, List(
                    InstrZ    -> ZeroExt(inst(19, 15), 64),
                    InstrI    -> SignExt(inst(31, 20), 64),
                    InstrIJ   -> SignExt(inst(31, 20), 64),
                    InstrS    -> SignExt(Cat(inst(31, 25), inst(11, 7)), 64),
                    InstrB    -> SignExt(Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)), 64),
                    InstrU    -> SignExt(Cat(inst(31, 12), 0.U(12.W)), 64),
                    InstrJ    -> SignExt(Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)), 64)
                  ))
  val csrRaddr = LookupTree(csr, List(
    CsrId.mstatus -> CsrAddr.mstatus,
    CsrId.mtvec   -> CsrAddr.mtvec,
    CsrId.mepc    -> CsrAddr.mepc,
    CsrId.mcause  -> CsrAddr.mcause
  ))
  val systemCtrl = ListLookup(inst, Instruction.SystemDefault, RV64IM.systemCtrl)(0)

/*
  // 分支计算
  // forward
  val opAPre = MuxLookup(io.forwardA, io.datasrc.src1, List(
    Forward.lsuData -> io.lsuForward,
    Forward.wbuData -> io.wbuForward,
    Forward.csrData -> io.csrForward,
    Forward.normal  -> io.datasrc.src1
  ))
  val opBPre = MuxLookup(io.forwardB, io.datasrc.src2, List(
    Forward.lsuData -> io.lsuForward,
    Forward.wbuData -> io.wbuForward,
    Forward.csrData -> io.csrForward,
    Forward.normal  -> io.datasrc.src2
  ))
  brAlu.io.opA         := opAPre
  brAlu.io.opB         := opBpre
  brAlu.io.brType      := aluOp
  val brMark           := brAlu.io.brMark
  val brInstr           = instrtype === InstrIJ || instrtype === InstrJ || instrtype === InstrB
  // todo: branch addr
  val brAddrOpA         = Mux(instrtype === InstrIJ, opAPre, io.in.pc)
  val brAddr            = brAddrOpA + imm

  io.redirect.brAddr   := Mux(systemCtrl === System.ecall, opAPre, Mux(systemCtrl === System.mret, aluOut, brAddr))
  io.redirect.valid    := Mux((brInstr && brMark) || systemCtrl === System.ecall || systemCtrl === System.mret, true.B, false.B)
*/

  // registerfile
  rf.io.rs1           := Mux(instrtype === InstrD, 10.U(5.W), rs1)
  rf.io.rs2           := rs2
  rf.io.wen           := io.regWrite.wen
  rf.io.waddr         := io.regWrite.rd
  rf.io.wdata         := io.regWrite.value
  rf.io.clock         := clock
  rf.io.reset         := reset
  
  val raddr            = Wire(UInt(3.W))
  raddr               := Mux(systemCtrl === System.ecall, CsrAddr.mtvec, Mux(systemCtrl === System.mret, CsrAddr.mepc, csrRaddr))
  csrReg.io.raddr     := raddr
  csrReg.io.waddr     := io.csrWrite.waddr
  csrReg.io.wen       := io.csrWrite.wen
  csrReg.io.wdata     := io.csrWrite.wdata
  csrReg.io.clock     := clock
  csrReg.io.reset     := reset
  // exception
  csrReg.io.exception := io.csrWrite.exception
  csrReg.io.epc       := io.csrWrite.epc(31, 0)
  csrReg.io.no        := io.csrWrite.no

  // 输出信息
  io.datasrc.pc       := io.in.pc(31, 0)
  io.datasrc.src1     := Mux(systemCtrl === System.mret || instrtype === InstrZ || systemCtrl === System.ecall, csrReg.io.rdata, rf.io.src1)
  io.datasrc.src2     := Mux(instrtype === InstrZ, rf.io.src1, rf.io.src2)
  io.datasrc.imm      := imm

  io.ctrl.rd          := rd
  io.ctrl.br          := instrtype === InstrIJ || instrtype === InstrJ || instrtype === InstrB
  io.ctrl.regWen      := instrtype =/= InstrB && instrtype =/= InstrS && instrtype =/= InstrD && instrtype =/= InstrN
  io.ctrl.isJalr      := instrtype === InstrIJ
  io.ctrl.lsType      := lsType
  io.ctrl.loadMem     := loadMem
  io.ctrl.wmask       := wmask
  io.ctrl.csrWen      := instrtype === InstrZ
  io.ctrl.csrWaddr    := csrRaddr
  io.ctrl.excepNo     := Mux(systemCtrl === System.ecall, "hb".U, 0.U)
  io.ctrl.exception   := systemCtrl === System.ecall // todo:type of exception, just for ecall now
  io.ctrl.memWen      := memEn === MemEn.store
  io.ctrl.memRen      := memEn === MemEn.load
  io.ctrl.ebreak      := systemCtrl === System.ebreak // debug
  io.ctrl.sysInsType  := systemCtrl
  io.ctrl.rs1         := Mux(instrtype === InstrZ, 0.U(5.W), rs1)
  io.ctrl.rs2         := Mux(instrtype === InstrZ, rs1, rs2)

  io.aluCtrl.aluSrc1  := aluSrc1
  io.aluCtrl.aluSrc2  := aluSrc2
  io.aluCtrl.aluOp    := aluOp
}