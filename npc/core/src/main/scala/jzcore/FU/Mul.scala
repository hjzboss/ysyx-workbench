package jzcore

import chisel3._
import chisel3.util._
import utils._
import top._


class Mul extends Module {
  val io = IO(new Bundle {
    val flush   = Input(Bool())
    val in      = Flipped(Decoupled(new MultiInput))
    val out     = Decoupled(new MultiOutput)
  })

  if(Settings.get("lowpower")) {
    val booth = Module(new Booth)
    booth.io.flush <> io.flush
    booth.io.in <> io.in
    booth.io.out <> io.out
  } else {
    val wallace = Module(new Wallace)
    wallace.io.flush <> io.flush
    wallace.io.in <> io.in
    wallace.io.out <> io.out
  }
}