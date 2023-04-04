package jzcore

import chisel3._
import chisel3.util._
import utils._

class EXU extends Module {
  val io = IO(new Bundle {
    val difftest  = Output(Bool())
    val inst      = Output(UInt(32.W))

    // 来自idu
    val in        = Flipped(Decoupled(new CtrlFlow))
/*
    val datasrc   = Flipped(new DataSrcIO)
    val aluCtrl   = Flipped(new AluIO)
    val ctrl      = Flipped(new Ctrl)
*/
    // 写回idu
    val regWrite  = Decoupled(new RFWriteIO)
    val csrWrite  = Decoupled(new CSRWriteIO)

    // 写回ifu
    val redirect  = Decoupled(new RedirectIO)
  })

  val inFire                    = io.in.valid && io.in.ready
  val regFire                   = io.regWrite.valid && io.regWrite.ready
  val csrFire                   = io.csrWrite.valid && io.csrWrite.ready
  val exception                 = io.in.bits.sysInsType === System.ecall

  val idle :: busy :: Nil = Enum(2)
  // 寄存器文件写状态机
  val regState = RegInit(idle)
  regState := MuxLookup(regState, idle, List(
    idle      -> Mux(inFire && io.in.bits.regWen, busy, idle),
    busy      -> Mux(regFire, idle, busy)
  ))

  // csr写状态机
  val csrState = RegInit(idle)
  csrState := MuxLookup(csrState, idle, List(
    idle      -> Mux(inFire && io.in.bits.isCsr, busy, idle),
    busy      -> Mux(csrFire, idle, busy)
  ))

  // ecall状态机
  val excepState = RegInit(idle)
  excepState := MuxLookup(excepState, idle, List(
    idle      -> Mux(inFire && exception, busy, idle),
    busy      -> Mux(csrFire, idle, busy)
  ))


  // 重定向状态机, todo
  val redirectState = RegInit(idle)
  redirectState := MuxLookup(redirectState, idle, List(
    idle      -> Mux(io.redirect.valid && io.redirect.ready, idle, Mux(!io.redirect.valid, idle, busy)),
    busy      -> Mux(io.redirect.ready, idle, busy)
  ))


  val alu   = Module(new Alu)
  val lsu   = Module(new Lsu)
  val stop  = Module(new Stop) // ebreak，调试环境使用

  val aluSrc1 = io.in.bits.aluSrc1
  val aluSrc2 = io.in.bits.aluSrc2

  // todo: forward
  val opAPre = io.in.bits.src1
  val opBPre = io.in.bits.src2

  val opA = Mux(aluSrc1 === SrcType.pc, io.in.bits.pc, Mux(aluSrc1 === SrcType.nul, 0.U(64.W), opAPre))
  val opB = Mux(aluSrc2 === SrcType.reg, opBPre, Mux(aluSrc2 === SrcType.plus4, 4.U(64.W), io.in.bits.imm))

  val lsType  = io.in.bits.lsType
  val rdata   = lsu.io.rdata
  val wmask   = io.in.bits.wmask

  val lsuOut  = LookupTree(lsType, Seq(
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

  io.in.ready                  := true.B // todo，待实现访存状态机

  val aluOut                    = alu.io.aluOut

  alu.io.opA                   := opA
  alu.io.opB                   := opB
  alu.io.aluOp                 := io.in.bits.aluOp

  // todo
  lsu.io.raddr                 := Mux(io.in.bits.loadMem, aluOut, 0.U(64.W))
  lsu.io.waddr                 := aluOut
  lsu.io.wdata                 := io.in.bits.wdata
  lsu.io.wmask                 := wmask
  lsu.io.wvalid                := io.redirect.ready // todo: 将lsu改称axi接口

  // regfile
  // todo, mem
  val regValue                  = Mux(io.in.bits.loadMem, lsuOut, Mux(io.in.bits.isCsr, opAPre, aluOut))
  // 寄存器文件的地址和数据锁存（未ready时)
  //val rdReg = RegEnable(io.in.bits.rd, regState === idle && io.regWrite.valid)
  //val regValueReg = RegEnable(regValue, regState === idle && io.regWrite.valid)
  val regValueReg               = Reg(UInt(64.W))
  regValueReg                  := Mux(regState === idle, regValue, regValueReg)
  val rdReg                     = Reg(UInt(64.W))
  rdReg                        := Mux(regState === idle, io.in.bits.rd, rdReg)
  io.regWrite.bits.value       := Mux(regState === idle, regValue, regValueReg)
  io.regWrite.valid            := (regState === idle && inFire && io.in.bits.regWen) || regState === busy
  io.regWrite.bits.rd          := Mux(regState === idle, io.in.bits.rd, rdReg)

  // todo: branch addr
  val brAddrOpA                 = Mux(io.in.bits.isJalr, opAPre, io.in.bits.pc)
  val brAddrPre                 = brAddrOpA + io.in.bits.imm

  // ecall mret
  val brAddr                    = Mux(io.in.bits.sysInsType === System.ecall, opAPre, Mux(io.in.bits.sysInsType === System.mret, aluOut, brAddrPre))
  val redirectReg               = RegEnable(brAddr, redirectState === idle && io.redirect.valid)
  io.redirect.bits.brAddr      := Mux(redirectState === idle, brAddr, redirectReg)
  io.redirect.valid            := Mux((io.in.bits.br && alu.io.brMark) || io.in.bits.sysInsType === System.ecall || io.in.bits.sysInsType === System.mret, true.B, false.B) // todo

  // csr
  // csr数据锁存
  val no                        = Mux(io.in.bits.sysInsType === System.ecall, "hb".U, 0.U)
/*
  val csrWaddrReg               = RegEnable(io.in.bits.csrWaddr, csrState === idle && io.csrWrite.valid)
  val csrWdataReg               = RegEnable(aluOut, csrState === idle && io.csrWrite.valid)
  val epcReg                    = RegEnable(io.in.bits.pc, csrState === idle && exception)
  val noReg                     = RegEnable(no, csrState === idle && exception)
*/
  val csrWaddrReg               = Reg(UInt(2.W))
  val csrWdataReg               = Reg(UInt(64.W))
  val epcReg                    = Reg(UInt(64.W))
  val noReg                     = Reg(UInt(4.W))
  csrWaddrReg                  := Mux(csrState === idle, io.in.bits.csrWaddr, csrWaddrReg)
  csrWdataReg                  := Mux(csrState === idle, aluOut, csrWdataReg)
  epcReg                       := Mux(excepState === idle, io.in.bits.pc, epcReg)
  noReg                        := Mux(excepState === idle, no, noReg)
  io.csrWrite.bits.waddr       := Mux(csrState === idle, io.in.bits.csrWaddr, csrWaddrReg)
  io.csrWrite.bits.wdata       := Mux(csrState === idle, aluOut, csrWdataReg)
  io.csrWrite.valid            := (csrState === idle && io.in.bits.isCsr) || csrState === busy
  // exception
  io.csrWrite.bits.exception   := (excepState === idle && exception) || excepState === busy
  io.csrWrite.bits.epc         := Mux(excepState === idle, io.in.bits.pc, epcReg)
  io.csrWrite.bits.no          := Mux(excepState === idle, no, noReg)

  // ebreak
  stop.io.valid                := Mux(io.in.bits.sysInsType === System.ebreak, true.B, false.B)
  stop.io.haltRet              := io.in.bits.src1

  // difftest
  io.difftest                  := io.regWrite.ready && io.csrWrite.ready && io.redirect.ready
  io.inst                      := io.in.bits.inst
}