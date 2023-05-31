package jzcore

import chisel3._
import chisel3.util._
import utils._

class CacheStage1 extends Module {
  val io = IO(new Bundle {
    val toStage1        = Flipped(new Stage1IO)
    val toStage2        = new Stage2IO

    val stall           = Input(Bool())

    // data array
    //val sram0_rdata     = Input(UInt(128.W))
    val sram0_cen       = Output(Bool())
    val sram0_wen       = Output(Bool())
    //val sram0_wmask     = Output(UInt(128.W))
    val sram0_addr      = Output(UInt(6.W))
    //val sram0_wdata     = Output(UInt(128.W)) 

    //val sram1_rdata     = Input(UInt(128.W))
    val sram1_cen       = Output(Bool())
    val sram1_wen       = Output(Bool())
    //val sram1_wmask     = Output(UInt(128.W))
    val sram1_addr      = Output(UInt(6.W))
    //val sram1_wdata     = Output(UInt(128.W)) 

    //val sram2_rdata     = Input(UInt(128.W))
    val sram2_cen       = Output(Bool())
    val sram2_wen       = Output(Bool())
    //val srvalidam2_wdata     = Output(UInt(128.W)) 

    //val sram3_rdata     = Input(UInt(128.W))
    val sram3_cen       = Output(Bool())
    val sram3_wen       = Output(Bool())
    //val sram3_wmask     = Output(UInt(128.W))
    val sram3_addr      = Output(UInt(6.W))
    //val sram3_wdata     = Output(UInt(128.W)) 
  })

  // decode
  val tag        = io.toStage1.addr(31, 10)
  val index      = io.toStage1.addr(9, 4)
  val align      = io.toStage1.addr(3, 2)

  val valid      = !io.stall

  io.sram0_cen  := !valid
  io.sram1_cen  := !valid
  io.sram2_cen  := !valid
  io.sram3_cen  := !valid
  io.sram0_wen  := true.B
  io.sram1_wen  := true.B
  io.sram2_wen  := true.B
  io.sram3_wen  := true.B
  io.sram0_addr := index
  io.sram1_addr := index
  io.sram2_addr := index
  io.sram3_addr := index

  io.toStage2.index     := index
  io.toStage2.tag       := tag
  io.toStage2.align     := align
  io.toStage2.pc        := io.toStage1.addr
  io.toStage2.cacheable := io.toStage1.cacheable
}
