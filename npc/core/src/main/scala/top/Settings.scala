package top

import chisel3._
import chisel3.util._

object DefaultSettings {
  def apply() = Map(
    "ResetVector" -> 0x80000000L,
    "TestVector"  -> 0x00000000L,
    "SocResetVector" -> 0x30000000L,
    "lowpower"    -> true,
    "sim"         -> true
  )
}

object Settings {
  var settings: Map[String, AnyVal] = DefaultSettings()
  def get(field: String) = {
    settings(field).asInstanceOf[Boolean]
  }
  def getLong(field: String) = {
    settings(field).asInstanceOf[Long]
  }
  def getInt(field: String) = {
    settings(field).asInstanceOf[Int]
  }
}