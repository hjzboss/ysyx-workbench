package jzcore

import chisel3._
import chisel3.util._
import utils._
import top.Settings

// todo: mret指令需要处理，mret会设置mstatus中的mie为mpie
class IDU extends Module with HasInstrType{
  val io = IO(new Bundle {
    val flush     = Input(Bool())
    val stall     = Input(Bool())

    val validIn   = Input(Bool())

    // 来自ifu
    val in        = Flipped(new InstrFetch)

    // 来自exu
    val regWrite  = Flipped(new RFWriteIO)
    val csrWrite  = Flipped(new CSRWriteIO)

    // 送给exu的控制信号
    val datasrc   = new DataSrcIO
    val aluCtrl   = new AluIO
    val ctrl      = new CtrlFlow

    val timerInt  = Input(Bool()) // clint int
    
    //val mret      = Output(Bool())
  })

  val grf       = Module(new GRF)
  val csr       = Module(new CSR)

  val inst      = io.in.inst
  val op        = inst(6, 0)
  val rs1       = inst(19, 15)
  val rs2       = inst(24, 20)
  val rd        = inst(11, 7)
  val csrReg    = inst(31, 20)

  val int       = csr.io.int && io.validIn

  // 译码
  val ctrlList  = ListLookup(inst, Instruction.DecodeDefault, RV64IM.table)
  val lsctrl    = ListLookup(inst, Instruction.LsDefault, RV64IM.lsTypeTable)
  val instrtype = ctrlList(0)
  val aluOp     = ctrlList(3)
  val aluSrc1   = ctrlList(1)
  val aluSrc2   = ctrlList(2)
  //val lsType    = Wire(UInt(4.W)) // 防止信号被优化
  //lsType       := lsctrl(0)
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
  grf.io.wen           := io.regWrite.wen
  grf.io.waddr         := io.regWrite.rd
  grf.io.wdata         := io.regWrite.value

  val csrRaddr         = Wire(UInt(12.W))
  csrRaddr            := Mux(systemCtrl === System.ecall || csr.io.int, CsrId.mtvec, Mux(systemCtrl === System.mret, CsrId.mepc, csrReg))
  csr.io.raddr        := csrRaddr
  csr.io.waddr        := io.csrWrite.waddr
  csr.io.wen          := io.csrWrite.wen
  csr.io.wdata        := io.csrWrite.wdata
  //csr.io.mret         := systemCtrl === System.mret && io.validIn && !io.flush && !io.stall
  csr.io.mret         := io.csrWrite.mret
  // exception
  csr.io.exception    := io.csrWrite.exception
  csr.io.epc          := io.csrWrite.epc(31, 0)
  csr.io.no           := io.csrWrite.no
  csr.io.timerInt     := io.timerInt
  //csr.io.intResp      := io.validIn && !io.flush && !io.stall
  //csr.io.ecall        := systemCtrl === System.ecall && io.validIn && !io.flush && !io.stall

  io.datasrc.pc       := io.in.pc(31, 0)
  io.datasrc.src1     := Mux(csr.io.int || instrtype === InstrE || csrType, csr.io.rdata, grf.io.src1)
  io.datasrc.src2     := Mux(csrType, grf.io.src1, grf.io.src2)
  io.datasrc.imm      := imm

  // 当一条指令产生中断时，其向寄存器写回和访存信号都要清零
  io.ctrl.rd          := rd
  io.ctrl.br          := isBr(instrtype) | !int
  io.ctrl.regWen      := regWen(instrtype) & !int
  io.ctrl.isJalr      := instrtype === InstrIJ
  io.ctrl.lsType      := lsType
  io.ctrl.loadMem     := loadMem
  io.ctrl.wmask       := wmask
  io.ctrl.csrWen      := csrType & !int
  //io.ctrl.csrRen      := csrType || instrtype === InstrE || int // just for csr forwarding
  io.ctrl.csrWaddr    := csrRaddr
  //io.ctrl.csrWaddr    := Mux(systemCtrl === System.ecall || systemCtrl === System.mret, CsrId.mstatus, csrRaddr) // TODO: ecall mepc的旁路问题
  // ecall优先级大于clint
  io.ctrl.excepNo     := Mux(systemCtrl === System.ecall, "hb".U(64.W), Mux(csr.io.int, true.B ## 7.U(63.W), 0.U)) // todo: only syscall and timer
  io.ctrl.exception   := systemCtrl === System.ecall | int // type of exception
  io.ctrl.csrChange   := io.ctrl.exception | io.ctrl.csrWen // change csr status
  io.ctrl.mret        := systemCtrl === System.mret // change mstatus
  io.ctrl.memWen      := memEn === MemEn.store & !int
  io.ctrl.memRen      := memEn === MemEn.load & !int
  //io.ctrl.sysInsType  := systemCtrl
  io.ctrl.rs1         := Mux(csrType | int, 0.U(5.W), rs1)
  io.ctrl.rs2         := Mux(csrType, rs1, rs2)
  io.ctrl.coherence   := instrtype === InstrF & !int
  //io.ctrl.int         := csr.io.int && io.validIn

  io.aluCtrl.aluSrc1  := aluSrc1
  io.aluCtrl.aluSrc2  := aluSrc2
  io.aluCtrl.aluOp    := Mux(!int, aluOp, AluOp.nop)

  //io.mret             := systemCtrl === System.mret

  if(Settings.get("sim")) {
    io.ctrl.ebreak.get    := instrtype === InstrD // ebreak
  }
}
