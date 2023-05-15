package jzcore

import chisel3._
import chisel3.util._
import utils._


/**
  * output [127:0] Q	读数据
  * input CLK	时钟
  * input CEN	使能信号, 低电平有效
  * input WEN	写使能信号, 低电平有效
  * input [127:0] BWEN	写掩码信号, 掩码粒度为1bit, 低电平有效
  * input [5:0] A	读写地址
  * input [127:0] D	写数据
  */
class Ram extends BlackBox {
  val io = IO(new Bundle {
    val Q     = Output(UInt(128.U))
    val CLK   = Input(Bool())
    val CEN   = Input(Bool())
    val WEN   = Input(Bool())
    val BWEN  = Input(UInt(128.U))
    val A     = Input(UInt(6.W))
    val D     = Input(UInt(128.U)) 
  })
}