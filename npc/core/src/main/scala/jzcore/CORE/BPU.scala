package jzcore

import chisel3._
import top.Settings
import chisel3.util._

// 分支预测模块：BTB+ras，采用静态分支预测
class BPU extends Module with HasResetVector {
  val io = IO(new Bundle{
    val pc = Input(UInt(32.W))
    val npc = Output(UInt(32.W))

    val bpuTrain = new BPUTrainIO
  })
  
  val btb = Module(new BTB)
  val ras = Module(new RAS)

  val snpc = io.pc + 4.U

  btb.io.pc := io.pc
  btb.io.train := io.bpuTrain

  val brType = btb.io.predict.brType
  val target = btb.io.predict.target
  val hit = btb.io.predict.hit

  ras.io.push := (hit & (brType === BrType.call)) | (io.bpuTrain.train & (io.bpuTrain.brType === BrType.call))
  ras.io.pushData := Mux(io.bpuTrain.train & (io.bpuTrain.brType === BrType.call), io.bpuTrain.target, snpc)
  ras.io.pop := (hit & (brType === BrType.ret)) | (io.bpuTrain.train & (io.bpuTrain.brType === BrType.ret))

  io.npc := Mux(!hit, snpc, Mux(brType =/= BrType.ret, target, Mux(ras.io.popVal, ras.io.popData, snpc)))
}

sealed class BTBEntry(tagNum: Int) extends Bundle {
  val tag     = UInt(tagNum.W)
  val brType  = UInt(2.W)
  val target  = UInt(32.W)
}

// 直接映射btb
sealed class BTB extends Module {
  val io = IO(new Bundle{
    val pc = Input(UInt(32.W))  
    // predict
    val predict = new BTBPredIO
    // training
    val train = new BPUTrainIO
  })

  val entryNum = Settings.getInt("btb_num") // btb entry number, todo
  val indexNum = log2Up(entryNum)
  val tagNum = 30 - indexNum // 忽略低两位， TODO: 压缩C扩展忽略最低位

  val btbInit = Wire(new BTBEntry(tagNum))
  btbInit.tag := 0.U
  btbInit.brType := BrType.nul
  btbInit.target := 0.U

  // btb主要结构
  val btbVal  = RegInit(VecInit(List.fill(entryNum)(false.B))) // valid array
  val btbMain = RegInit(VecInit(List.fill(entryNum)(btbInit)))

  val predPc = io.pc
  val trainPc = io.train.pc

  // 预测部分
  val predIndex = predPc(indexNum+1, 2)
  val predTag = predPc(31, indexNum+2)

  val pred = btbMain(predIndex)
  val hit = btbVal(predIndex) & (pred.tag === predTag)

  io.predict.hit    := hit
  io.predict.target := pred.target
  io.predict.brType := pred.brType

  // 训练部分
  val trainIndex = trainPc(indexNum+1, 2)
  val trainTag = predPc(31, indexNum+2)
  when(io.train.train) {
    // 新增btb项
    btbVal(trainIndex) := true.B
    btbMain(trainIndex).tag := trainTag
    btbMain(trainIndex).target := io.train.target
    btbMain(trainIndex).brType := io.train.brType
  }.elsewhen(io.train.invalid) {
    // 无效btb
    btbVal(trainIndex) := false.B
  }
}

// 返回地址栈
sealed class RAS extends Module {
  val io = IO(new Bundle{
    val push = Input(Bool())
    val pop = Input(Bool())

    val pushData = Input(UInt(32.W))
    val popData = Output(UInt(32.W))

    val popVal = Output(Bool()) // 是否是一个有效的pop
  })

  val rasNum = Settings.getInt("ras_num")
  val top = RegInit(0.U(log2Up(rasNum).W)) // 栈顶指针
  val topPlus = top + 1.U

  val stack = RegInit(VecInit(List.fill(rasNum)(0.U(32.W))))
  val count = RegInit(VecInit(List.fill(rasNum)(0.U(5.W)))) // 递归调用计数器, todo: 设置多大？

  val full = top === (rasNum).U
  val empty = (top === 0.U) & (count(top) === 0.U)

  val topData = stack(top)
  val topCount = count(top)

  when(io.push) {
    when(topData === io.pushData) {
      // 同一个call递归调用
      count(top) := Mux(topCount === 31.U, topCount, topCount + 1.U)
    }.elsewhen(!full) {
      stack(topPlus) := io.pushData
      count(topPlus) := count(topPlus) + 1.U
      top := topPlus
    }
  }

  when(io.pop && !empty) {
    when(topCount =/= 0.U) {
      count(top) := topCount - 1.U
    }.otherwise {
      top := top - 1.U
    }
  }

  io.popData := topData
  io.popVal := !empty
}