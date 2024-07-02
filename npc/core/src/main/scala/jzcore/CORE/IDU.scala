package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings
import chisel3.util.experimental.BoringUtils

// todo: mret指令需要处理，mret会设置mstatus中的mie为mpie
class IDU extends Module with HasInstrType{
  val io = IO(new Bundle {
    val flush     = Input(Bool())
    val stall     = Input(Bool())

    val validIn   = Input(Bool())

    val bpuTrain  = if(Settings.getString("core") == "normal") Some(Flipped(new BPUTrainIO)) else None

    // 来自ifu
    val in        = Flipped(new InstrFetch)

    // 来自exu
    val regWrite  = Flipped(new RFWriteIO)
    val csrWrite  = Flipped(new CSRWriteIO)

    // 送给exu的控制信号
    val datasrc   = new DataSrcIO
    val aluCtrl   = new AluIO
    val ctrl      = new CtrlFlow

    // 写回ifu
    val redirect  = new RedirectIO

    val rs1       = Output(UInt(5.W))
    val rs2       = Output(UInt(5.W))
    
    // 旁路数据
    val lsuForward  = Input(UInt(64.W))
    val wbuForward  = Input(UInt(64.W))
    val exuForward  = Input(UInt(64.W))

    val forwardA  = Input(Forward())
    val forwardB  = Input(Forward())

    val timerInt  = Input(Bool()) // clint int

    val isBr      = Output(Bool())

    val debugIn   = if(Settings.get("sim")) Some(Flipped(new DebugIO)) else None
    val debugOut  = if(Settings.get("sim")) Some(new DebugIO) else None
  })

  val grf       = Module(new GRF)
  val csr       = Module(new CSR)

  val inst      = io.in.inst
  val op        = inst(6, 0)
  val rs1       = inst(19, 15)
  val rs2       = inst(24, 20)
  val rd        = inst(11, 7)
  val csrReg    = inst(31, 20)

  val int       = csr.io.int & io.validIn

  // 译码
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

  val grfRs1 = Mux(instrtype === InstrD, 10.U(5.W), rs1)
  val grfRs2 = rs2

  // register file
  grf.io.rs1          := grfRs1
  grf.io.rs2          := grfRs2
  grf.io.wen          := io.regWrite.wen
  grf.io.waddr        := io.regWrite.rd
  grf.io.wdata        := io.regWrite.value

  io.rs1              := rs1
  io.rs2              := rs2

  val csrRaddr         = Wire(UInt(12.W))
  csrRaddr            := Mux(systemCtrl === System.ecall || csr.io.int, CsrId.mtvec, Mux(systemCtrl === System.mret, CsrId.mepc, csrReg))
  csr.io.raddr        := csrRaddr
  csr.io.waddr        := io.csrWrite.waddr
  csr.io.wen          := io.csrWrite.wen
  csr.io.wdata        := io.csrWrite.wdata
  csr.io.mret         := io.csrWrite.mret
  // exception
  csr.io.exception    := io.csrWrite.exception
  csr.io.epc          := io.csrWrite.epc(31, 0)
  csr.io.no           := io.csrWrite.no
  csr.io.timerInt     := io.timerInt
  val exception        = systemCtrl === System.ecall | int // type of exception
  val excepInsr        = instrtype === InstrE // 异常指令（ecall, mret）

  // branch detected
  // forward
  val opA = LookupTreeDefault(io.forwardA, grf.io.src1, List(
    Forward.exuData     -> io.exuForward,
    Forward.lsuData     -> io.lsuForward,
    Forward.wbuData     -> io.wbuForward,
    Forward.normal      -> grf.io.src1
  ))
  val opB = LookupTreeDefault(io.forwardB, grf.io.src2, List(
    Forward.exuData     -> io.exuForward,
    Forward.lsuData     -> io.lsuForward,
    Forward.wbuData     -> io.wbuForward,
    Forward.normal      -> grf.io.src2
  ))
  // brmark compute
  val brMark = LookupTreeDefault(aluOp, false.B, List(
    AluOp.jump      -> true.B,
    AluOp.beq       -> (opA === opB),
    AluOp.bne       -> (opA =/= opB),
    AluOp.blt       -> (opA.asSInt() < opB.asSInt()),
    AluOp.bge       -> (opA.asSInt() >= opB.asSInt()),
    AluOp.bgeu      -> (opA >= opB),
    AluOp.bltu      -> (opA < opB),
  ))

  if(Settings.getString("core") == "normal") {
    val brAddrPre        = Wire(UInt(32.W))
    brAddrPre           := Mux(instrtype === InstrIJ, opA(31, 0), io.in.pc(31, 0)) + io.datasrc.imm(31, 0)
    val brAddr           = Mux(excepInsr | int, csr.io.rdata(31, 0), Mux(brMark, brAddrPre(31, 0), io.in.pc + 4.U))
    io.redirect.brAddr  := brAddr
    io.redirect.valid   := (io.validIn & (brAddr =/= io.in.npc)) | int // 分支预测错误时进行跳转

    val call             = ((rd === 1.U) | (rd === 5.U)) & ((((rs1 === 1.U) | (rs1 === 5.U)) & (rd === rs1)) | ((rs1 =/= 1.U) & (rs1 =/= 5.U)))
    val ret              = (rd =/= 1.U) & (rd =/= 5.U) & ((rs1 === 1.U) | (rs1 === 5.U))

    // btb train
    // 当是分支指令且预测错误时更新btb，当不是分支指令且预测错误时无效btb
    // 发生定时器中断时不训练btb
    io.bpuTrain.get.train   := ((isBr(instrtype) & brMark) | excepInsr) & (brAddr =/= io.in.npc) & !int
    io.bpuTrain.get.pc      := io.in.pc
    io.bpuTrain.get.target  := brAddr
    io.bpuTrain.get.brType  := Mux(call, BrType.call, Mux(ret, BrType.ret, BrType.jump))
    io.bpuTrain.get.invalid := !((isBr(instrtype) & brMark) | excepInsr) & (brAddr =/= io.in.npc) & !int
    if(Settings.get("perf") && Settings.get("sim")) {
      BoringUtils.addSource(io.redirect.valid & !io.stall, "bpuMiss")
      BoringUtils.addSource(io.validIn & !io.stall, "bpuReq")
    }
  } else {
    // fast core
    val brAddrPre         = Wire(UInt(32.W))
    brAddrPre            := Mux(instrtype === InstrIJ, opA(31, 0), io.in.pc(31, 0)) + io.datasrc.imm(31, 0)
    val brAddr            = Mux(excepInsr | int, csr.io.rdata(31, 0), brAddrPre(31, 0))
    io.redirect.brAddr   := brAddr
    io.redirect.valid    := (isBr(instrtype) & brMark) | excepInsr | int
  }

  // data output
  io.datasrc.pc       := io.in.pc(31, 0)
  io.datasrc.src1     := Mux(csrType | int | excepInsr, csr.io.rdata, opA)
  io.datasrc.src2     := Mux(csrType, opA, opB)
  io.datasrc.imm      := imm

  io.isBr             := isBr(instrtype) & !int

  // 当一条指令产生中断时，其向寄存器写回和访存信号都要清零
  io.ctrl.rd          := rd
  io.ctrl.regWen      := regWen(instrtype) & !int
  io.ctrl.lsType      := lsType
  io.ctrl.loadMem     := loadMem
  io.ctrl.wmask       := wmask
  io.ctrl.csrWen      := csrType & !int
  io.ctrl.csrWaddr    := csrRaddr
  // ecall优先级大于clint
  io.ctrl.excepNo     := Mux(systemCtrl === System.ecall, "hb".U(64.W), Mux(csr.io.int, true.B ## 7.U(63.W), 0.U)) // todo: only syscall and timer
  io.ctrl.exception   := exception // type of exception
  io.ctrl.csrChange   := instrtype === InstrE | io.ctrl.csrWen | int // change csr status(include mret), TODO
  io.ctrl.mret        := systemCtrl === System.mret // change mstatus
  io.ctrl.memWen      := memEn === MemEn.store & !int
  io.ctrl.memRen      := memEn === MemEn.load & !int
  io.ctrl.rs1         := Mux(csrType | int | excepInsr, 0.U(5.W), grfRs1) // 如果是csr或者异常中断指令就会将rs1清零，防止exu阶段旁路转发
  io.ctrl.rs2         := Mux(csrType, grfRs1, grfRs2)
  io.ctrl.coherence   := instrtype === InstrF & !int

  io.aluCtrl.aluSrc1  := aluSrc1
  io.aluCtrl.aluSrc2  := aluSrc2
  io.aluCtrl.aluOp    := Mux(!int, aluOp, AluOp.nop)

  if(Settings.get("sim")) {
    io.ctrl.ebreak.get    := instrtype === InstrD // ebreak
    io.ctrl.haltRet.get   := opA

    io.debugOut.get.inst   := io.debugIn.get.inst
    io.debugOut.get.pc     := io.debugIn.get.pc
    io.debugOut.get.nextPc := Mux(io.redirect.valid, io.redirect.brAddr, io.debugIn.get.nextPc)
    io.debugOut.get.valid  := io.debugIn.get.valid
  }
}
