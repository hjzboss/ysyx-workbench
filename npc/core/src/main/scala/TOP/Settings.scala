package top

import chisel3._
import chisel3.util._

// 提供了两种仿真模式：供verilator仿真环境使用的模式和供soc验证的模式，使用sim参数来进行切换
// 乘法器提供了三种：快速（直接使用*），booth和wallance，在mul中选择
// 除法器提供了两种：快速（直接使用/和%）和恢复余数法rest，在div中选择
// 提供了三种核：单周期核、快速核（没有cache和axi）和常规核（带cache和axi），单周期核只能使用fast版本的乘除法器
object DefaultSettings {
  def apply() = Map(
    "ResetVector" -> 0x80000000L,
    "TestVector"  -> 0x00000000L,
    "SocResetVector" -> 0x30000000L,
    "mul"         -> "booth", // fast, booth, wallace
    "div"         -> "rest", // fast, rest
    "sim"         -> true, // verilator mode， false is soc mode， no debug
    "core"        -> "fast", // single, fast, normal
    "btb_num"     -> 64, // btb entry num
    "ras_num"     -> 8 // ras entry num
  )
}

object Settings {
  var settings = DefaultSettings()
  def get(field: String) = {
    settings(field).asInstanceOf[Boolean]
  }
  def getLong(field: String) = {
    settings(field).asInstanceOf[Long]
  }
  def getInt(field: String) = {
    settings(field).asInstanceOf[Int]
  }
  def getString(field: String) = {
    settings(field).asInstanceOf[String]
  }
}