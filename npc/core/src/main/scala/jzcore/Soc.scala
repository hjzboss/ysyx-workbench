package jzcore

import chisel3._
import chisel3.util._
import utils._

class Soc extends Module {
  val io = IO(new Bundle {
    // 传给仿真环境
    val debug     = new DebugIO

    // 防止被优化
    val valid1     = Output(Bool())
    val valid2     = Output(Bool())
    //val valid3     = Output(Bool())
  })

  val rsram = Module(new Sram)
  val wsram = Module(new Sram)
  val core  = Module(new JzCore)

  core.io.axiRaddrIO <> rsram.io.raddrIO
  core.io.axiRdataIO <> rsram.io.rdataIO
  // todo: 实现仲裁
  rsram.io.waddrIO.valid := false.B
  rsram.io.waddrIO.bits.addr := "h80000000".U
  rsram.io.wdataIO.valid := false.B
  rsram.io.wdataIO.bits.wdata := 0.U
  rsram.io.wdataIO.bits.wstrb := 0.U
  rsram.io.brespIO.ready := false.B


  core.io.raddrIO    <> wsram.io.raddrIO
  core.io.rdataIO    <> wsram.io.rdataIO
  core.io.axiWaddrIO <> wsram.io.waddrIO
  core.io.axiWdataIO <> wsram.io.wdataIO
  core.io.axiBrespIO <> wsram.io.brespIO

  // 仿真环境
  io.debug        := core.io.debug
  io.valid1       := core.io.axiWaddrIO.valid
  io.valid2       := wsram.io.waddrIO.valid
}