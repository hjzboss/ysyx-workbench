package jzcore

import chisel3._
import chisel3.util._
import utils._


// 加减交替法
class Divider extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(DivInput))
    val out     = Decoupled(DivOutput)
  })

  val idle :: busy :: Nil = Enum(3)
  val state = RegInit(idle)

  
}