package jzcore

import chisel3._
import chisel3.util._
import utils._

/*
trait HasCacheIO {
  implicit val cacheConfig: CacheConfig
  val io = IO(new CacheIO)
}

// 四路组相联, 16KB，随机替换算法
class Cache(entryNum: Int, wayNum: Int, blockWidth: Int, tagWidth: Int) extends Module {
  val io = IO(
    // cpu和cache的接口
    

    // axi总线请求信号
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    
    // 仲裁信号
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())
  )

  val wayWidth = log2Ceil(wayNum.U)

  // 随机替换计数器
  val rcnt = RegInit(0.U(wayWidth.W))
  rcnt := rcnt + 1.U(wayWidth.W)

  val setNum = entryNum / wayNum
  val cache = List.fill(wayNum)(new CacheBank(setNum, blockWidth, tagWidth))
  (0 until wayNum).map(i => cache(i).io.)
}

class CacheBank(entryNum: Int, blockWidth: Int, tagWidth: Int) extends Module {
  val io = IO(new Bundle {
    val valid = Output(Bool())
    val dirty = Output(Bool())
    val tag   = Output(UInt(tagWidth.W))
    val rdata = Output()
  })


}
*/

// 四路组相联cache
class ICache extends Module {
  val io = IO(new Bundle {
    // 和cpu的接口
    val readIO      = Flipped(Decoupled(new CacheIO))

    // axi总线请求信号
    val axiRaddrIO  = Decoupled(new RaddrIO)
    val axiRdataIO  = Flipped(Decoupled(new RdataIO))
    val axiWaddrIO  = Decoupled(new WaddrIO)
    val axiWdataIO  = Decoupled(new WdataIO)
    val axiBrespIO  = Flipped(Decoupled(new BrespIO))    
    // 仲裁信号
    val axiReq      = Output(Bool())
    val axiGrant    = Input(Bool())
    val axiReady    = Output(Bool())
  })


}
