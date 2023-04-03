package jzcore

import chisel3._
import chisel3.util._
import utils._

class IDU extends Module with HasInstrType {
  val io = IO(new Bundle {
    //val fetch     = Flipped(new InstrFetch)
    // ifu输入
    val in        = Flipped(Decoupled(new InstrFetch))
    val out       = Decoupled(new CtrlFlow)

    // exu回写
    val regWrite  = Flipped(Decoupled(new RFWriteIO))
    val csrWrite  = Flipped(Decoupled(new CSRWriteIO))

    /*
    val datasrc   = new DataSrcIO
    val out.bits   = new AluIO
    val out.bits      = new out.bits
    */

    // 防止信号被优化
    val lsType    = Output(UInt(4.W))
  })

  val fire = io.out.valid && io.out.ready

  val ok :: d_wait :: Nil = Enum(2)
  val state = RegInit(ok)
  state := MuxLookup(state, ok, List(
    ok         -> Mux(io.in.valid && io.out.ready, ok, d_wait),
    d_wait     -> Mux(fire, ok, d_wait)
  ))

  // 当exu阻塞时锁存数据
  val instReg  = Reg(UInt(32.W))
  val pcReg    = Reg(UInt(64.W))
  instReg      := Mux(state === ok, io.in.bits.inst, instReg)
  pcReg        := Mux(state === ok, io.in.bits.pc, pcReg)

  // 译码
  val inst      = Mux(state === ok, io.in.bits.inst, instReg)
  val pc        = Mux(state === ok, io.in.bits.pc, pcReg)
  val rf        = Module(new RF)
  val csrReg    = Module(new CsrReg)
  val op        = inst(6, 0)
  val rs1       = inst(19, 15)
  val rs2       = inst(24, 20)
  val rd        = inst(11, 7)
  val csr       = inst(31, 20)
  // 产生控制信号
  val ctrlList  = ListLookup(inst, Instruction.DecodeDefault, RV64IM.table)
  val lsctrl    = ListLookup(inst, Instruction.LsDefault, RV64IM.lsTypeTable)
  val instrtype = ctrlList(0)
  val aluOp     = ctrlList(3)
  val aluSrc1   = ctrlList(1)
  val aluSrc2   = ctrlList(2)
  val lsType    = lsctrl(0)
  val loadMem   = lsctrl(2)
  val wmask     = lsctrl(1)
  val imm = LookupTree(instrtype, List(
    InstrZ    -> ZeroExt(inst(19, 15), 64),
    InstrI    -> SignExt(inst(31, 20), 64),
    InstrIJ   -> SignExt(inst(31, 20), 64),
    InstrS    -> SignExt(Cat(inst(31, 25), inst(11, 7)), 64),
    InstrB    -> SignExt(Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)), 64),
    InstrU    -> SignExt(Cat(inst(31, 12), 0.U(12.W)), 64),
    InstrJ    -> SignExt(Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)), 64)
  ))
  // csr寄存器读取
  val csrRaddr = LookupTree(csr, List(
    CsrId.mstatus -> CsrAddr.mstatus,
    CsrId.mtvec   -> CsrAddr.mtvec,
    CsrId.mepc    -> CsrAddr.mepc,
    CsrId.mcause  -> CsrAddr.mcause
  ))
  // 系统指令类型
  val systemCtrl = ListLookup(inst, Instruction.SystemDefault, RV64IM.systemCtrl)(0)

  // registerfile
  rf.io.rs1               := Mux(instrtype === InstrD, 10.U(5.W), rs1)
  rf.io.rs2               := rs2
  rf.io.wen               := io.regWrite.valid && fire // 只有在阻塞的最后才写入寄存器文件
  rf.io.waddr             := io.regWrite.bits.rd
  rf.io.wdata             := io.regWrite.bits.value
  rf.io.clock             := clock
  rf.io.reset             := reset
  io.regWrite.ready       := fire

  // csr regfile
  csrReg.io.raddr         := Mux(systemCtrl === System.ecall, CsrAddr.mtvec, Mux(systemCtrl === System.mret, CsrAddr.mepc, csrRaddr))
  csrReg.io.waddr         := io.csrWrite.bits.waddr
  //csrReg.io.wen           := io.csrWrite.wen
  csrReg.io.wen           := io.csrWrite.valid && fire // 只有在阻塞的最后才写入寄存器文件
  csrReg.io.wdata         := io.csrWrite.bits.wdata
  csrReg.io.clock         := clock
  csrReg.io.reset         := reset
  // exception
  csrReg.io.exception     := io.csrWrite.bits.exception && fire // todo
  csrReg.io.epc           := io.csrWrite.bits.epc
  csrReg.io.no            := io.csrWrite.bits.no
  io.csrWrite.ready       := fire

  io.in.ready             := true.B // idu不会阻塞

  // idu out
  io.out.valid            := true.B // todo
  // data
  io.out.bits.pc          := pc
  io.out.bits.src1        := Mux(systemCtrl === System.mret || instrtype === InstrZ || systemCtrl === System.ecall, csrReg.io.rdata, rf.io.src1)
  io.out.bits.src2        := Mux(instrtype === InstrZ, rf.io.src1, rf.io.src2)
  io.out.bits.imm         := imm
  // ctrl
  io.out.bits.rd          := rd
  io.out.bits.br          := instrtype === InstrIJ || instrtype === InstrJ || instrtype === InstrB
  io.out.bits.regWen      := instrtype =/= InstrB && instrtype =/= InstrS && instrtype =/= InstrD
  io.out.bits.isJalr      := instrtype === InstrIJ
  io.out.bits.lsType      := lsType
  io.out.bits.wdata       := rf.io.src2
  io.out.bits.loadMem     := loadMem
  io.out.bits.wmask       := wmask
  io.out.bits.isCsr       := instrtype === InstrZ
  io.out.bits.csrWaddr    := csrRaddr
  io.out.bits.sysInsType  := systemCtrl
  // alu ctrl
  io.out.bits.aluSrc1     := aluSrc1
  io.out.bits.aluSrc2     := aluSrc2
  io.out.bits.aluOp       := aluOp

  // 防止信号被优化
  io.lsType               := lsType
}