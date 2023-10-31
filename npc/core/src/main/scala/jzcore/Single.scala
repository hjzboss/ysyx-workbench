package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

// single cycle cpu, just for debug
class Single extends Module with HasResetVector with HasInstrType {
  val io = IO(new Bundle {
    val debug           = if(Settings.get("sim")) Some(new DebugIO) else None
    val lsFlag          = if(Settings.get("sim")) Some(Output(Bool())) else None
  })

  // IFU
  val redirectValid= WireDefault(false.B)
  val brAddrPre    = Wire(UInt(32.W))
  val pc           = RegInit(resetVector.U(32.W))
  val snpc         = pc + 4.U(32.W)
  pc              := Mux(redirectValid, brAddrPre, snpc)
  val imem         = Module(new IMEM)
  imem.io.pc      := pc


  // IDU
  val grf       = Module(new GRF)
  val csr       = Module(new CSR)

  val inst      = imem.io.inst
  val op        = inst(6, 0)
  val rs1       = inst(19, 15)
  val rs2       = inst(24, 20)
  val rd        = inst(11, 7)
  val csrReg    = inst(31, 20)
  val ctrlList  = ListLookup(inst, Instruction.DecodeDefault, RV64IM.table)
  val lsctrl    = ListLookup(inst, Instruction.LsDefault, RV64IM.lsTypeTable)
  val instrtype = ctrlList(0)
  val aluOp     = ctrlList(3)
  val aluSrc1   = ctrlList(1)
  val aluSrc2   = ctrlList(2)
  val lsType    = lsctrl(0)
  val loadMem   = lsctrl(2)
  val wmask     = lsctrl(1)
  val memEn     = lsctrl(3)
  val csrType   = isCsr(instrtype)
  val imm       = LookupTreeDefault(instrtype, 0.U, List(
                    InstrZ    -> ZeroExt(inst(19, 15), 64),
                    InstrI    -> SignExt(inst(31, 20), 64),
                    InstrIJ   -> SignExt(inst(31, 20), 64),
                    InstrS    -> SignExt(Cat(inst(31, 25), inst(11, 7)), 64),
                    InstrB    -> SignExt(Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)), 64),
                    InstrU    -> SignExt(Cat(inst(31, 12), 0.U(12.W)), 64),
                    InstrJ    -> SignExt(Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)), 64)
                  ))
  val systemCtrl = ListLookup(inst, Instruction.SystemDefault, RV64IM.systemCtrl)(0)
  // register file
  grf.io.rs1           := Mux(instrtype === InstrD, 10.U(5.W), rs1)
  grf.io.rs2           := rs2

  // csr file
  val csrRaddr         = Wire(UInt(12.W))
  csrRaddr            := Mux(systemCtrl === System.ecall || csr.io.int, CsrId.mtvec, Mux(systemCtrl === System.mret, CsrId.mepc, csrReg))
  csr.io.raddr        := csrRaddr
  csr.io.timerInt     := false.B // todo

  val src1             = Mux(instrtype === InstrE || csrType, csr.io.rdata, grf.io.src1)
  val src2             = Mux(csrType, grf.io.src1, grf.io.src2)
  // 当一条指令产生中断时，其向寄存器写回和访存信号都要清零
  val br               = isBr(instrtype)
  val isJalr           = instrtype === InstrIJ
  val memWen           = memEn === MemEn.store
  val memRen           = memEn === MemEn.load

  val brMark = LookupTreeDefault(aluOp, false.B, List(
    AluOp.jump      -> true.B,
    AluOp.beq       -> (opA === opB),
    AluOp.bne       -> (opA =/= opB),
    AluOp.blt       -> (opA.asSInt() < opB.asSInt()),
    AluOp.bge       -> (opA.asSInt() >= opB.asSInt()),
    AluOp.bgeu      -> (opA >= opB),
    AluOp.bltu      -> (opA < opB),
  ))

  // EXU
  val alu   = Module(new Alu)
  // 操作数选择
  val opA = Mux(aluSrc1 === SrcType.pc, ZeroExt(pc, 64), Mux(aluSrc1 === SrcType.nul, 0.U(64.W), src1))
  val opB = Mux(aluSrc2 === SrcType.reg, src2, Mux(aluSrc2 === SrcType.plus4, 4.U(64.W), imm))
  // alu
  val aluOut            = alu.io.aluOut
  alu.io.stall         := false.B
  alu.io.flush         := false.B
  alu.io.opA           := opA
  alu.io.opB           := opB
  alu.io.aluOp         := aluOp
  // branch addrint
  val brAddr           = Mux(isJalr, src1(31, 0), pc) + imm(31, 0)
  // ecall mret
  brAddrPre            := Mux(systemCtrl === System.ecall | systemCtrl === System.mret, src1(31, 0), brAddr)
  redirectValid        := (br && alu.io.brMark) || systemCtrl === System.ecall || systemCtrl === System.mret


  // LSU
  val addr        = aluOut
  val readTrans   = memRen
  val writeTrans  = memWen
  val hasTrans    = readTrans || writeTrans
  val pmem        = Module(new Pmem) // debug
  // debug
  pmem.io.raddr         := addr
  pmem.io.rvalid        := readTrans
  pmem.io.waddr         := addr
  pmem.io.wdata         := src2 << Cat(addr(2, 0), 0.U(3.W))
  pmem.io.mask          := wmask << addr(2, 0)
  val align64            = Cat(addr(2, 0), 0.U(3.W)) // debug
  val rdata              = pmem.io.rdata >> align64
  val lsuOut             = LookupTree(lsType, Seq(
                            LsType.ld   -> rdata,
                            LsType.lw   -> SignExt(rdata(31, 0), 64),
                            LsType.lh   -> SignExt(rdata(15, 0), 64),
                            LsType.lb   -> SignExt(rdata(7, 0), 64),
                            LsType.lbu  -> ZeroExt(rdata(7, 0), 64),
                            LsType.lhu  -> ZeroExt(rdata(15, 0), 64),
                            LsType.lwu  -> ZeroExt(rdata(31, 0), 64),
                            LsType.sd   -> rdata,
                            LsType.sw   -> rdata,
                            LsType.sh   -> rdata,
                            LsType.sb   -> rdata,
                            LsType.nop  -> rdata
                          ))


  // WBU
  grf.io.wen          := regWen(instrtype)
  grf.io.waddr        := rd
  grf.io.wdata        := Mux(csrType, opA, Mux(loadMem.asBool, lsuOut, aluOut))
  csr.io.waddr        := csrRaddr
  csr.io.wen          := csrType
  csr.io.wdata        := aluOut
  csr.io.mret         := systemCtrl === System.mret
  csr.io.exception    := systemCtrl === System.ecall
  csr.io.epc          := pc
  csr.io.no           := Mux(systemCtrl === System.ecall, "hb".U(64.W), 0.U)

  // ebreak
  if(Settings.get("sim")) {
    val stop             = Module(new Stop)
    stop.io.valid       := instrtype === InstrD // ebreak
    stop.io.haltRet     := opA

    io.debug.get.nextPc := Mux(redirectValid, brAddrPre, snpc)
    io.debug.get.pc     := pc
    io.debug.get.inst   := inst
    io.debug.get.valid  := true.B

    io.lsFlag.get       := hasTrans
  }
}