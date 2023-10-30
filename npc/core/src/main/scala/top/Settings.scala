package top

import chisel3._
import chisel3.util._

object DefaultSettings {
  def apply() = Map(
    "ResetVector" -> 0x80000000L,
    "TestVector"  -> 0x00000000L,
    "SocResetVector" -> 0x30000000L,
    "mul"         -> "fast", // fast, booth, wallance
    "div"         -> "fast", // fast, rest
    //"fast"        -> true, // no-cache, no-axi, fast simulation mode
    "sim"         -> true, // verilator mode
    "singlecycle" -> true
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