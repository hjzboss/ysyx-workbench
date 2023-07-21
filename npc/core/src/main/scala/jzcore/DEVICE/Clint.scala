package jzcore

import chisel3._
import chisel3.util._
import utils._

// todo: 定时器中断在什么时候处理？
class Clint extends Module {
  val io = IO(new Bundle {
    val clintIO = Flipped(new ClintIO)
    val int  = Output(Bool()) // 中断信号，与mip.mtip直接相连
  })

  //val msip = RegInit(0.U(32.W))
  val mtimecmp = RegInit(0.U(64.W))
  val mtime = RegInit(0.U(64.W))

  mtime := mtime + 1.U

  val MTIMECMP = 0x2004000
  val MTIME    = 0x200BFF8

  val mask = LookupTree(io.clintIO.wmask, List(
              Wmask.double  -> "hffffffffffffffff".U,
              Wmask.word    -> "h00000000ffffffff".U,
              Wmask.half    -> "h000000000000ffff".U,
              Wmask.byte    -> "h00000000000000ff".U,
              Wmask.nop     -> 0.U,
            ))

  when(io.clintIO.wen) {
    when(io.clintIO.addr === MTIMECMP.U) {
      mtimecmp := (mtimecmp & ~mask) | (io.clintIO.wdata & mask)
    }.elsewhen(io.clintIO.addr === MTIME.U) {
      mtime := (mtime & ~mask) | (io.clintIO.wdata & mask)
    }
  }

  io.clintIO.rdata := LookupTreeDefault(io.clintIO.addr, 0.U, List(
    MTIMECMP.U -> mtimecmp,
    MTIME.U    -> mtime
  ))

  io.int := mtime >= mtimecmp
}
