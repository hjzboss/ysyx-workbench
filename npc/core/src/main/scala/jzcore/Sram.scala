package jzcore

import chisel3._
import chisel3.util._
import utils._

class Sram extends Module {
  val io = IO(new Bundle {
    val raddrIO  = Flipped(Decoupled(new AddrIO))
    val rdataIO = Decoupled(new DataIO)
  })

  val pmem               = Module(new Pmem)

  io.raddrIO.ready      := true.B

  val raddrFire          = io.raddrIO.valid && io.raddrIO.ready

  val rvalid             = RegInit(false.B)
  rvalid                := raddrFire
  io.rdataIO.valid       := rvalid

  val dataFire           = rvalid && io.dataIO.ready

  pmem.io.raddr         := io.raddrIO.bits.addr
  pmem.io.rvalid        := dataFire
  io.rdataIO.bits.data  := pmem.io.rdata
}