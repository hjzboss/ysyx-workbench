package top

import chisel3._
import chisel3.util._

// 提供了三种仿真模式： 单周期、快速（没有cache和axi）和常规仿真，单周期仿真只能使用fast版本的乘除法器
object DefaultSettings {
  def apply() = Map(
    "ResetVector" -> 0x80000000L,
    "TestVector"  -> 0x00000000L,
    "SocResetVector" -> 0x30000000L,
    "mul"         -> "fast", // fast, booth, wallance
    "div"         -> "fast", // fast, rest
    "sim"         -> true, // verilator mode， false is soc mode， no debug
    "core"        -> "single" // single, fast, normal
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