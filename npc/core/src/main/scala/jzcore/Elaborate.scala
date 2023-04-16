import jzcore._
import circt.stage._

object Elaborate extends App {
  def top       = new Soc()
  val useMFC    = true // use MLIR-based firrtl compiler
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))
  if (useMFC) {
    println(args)
    (new ChiselStage).execute(Array("-X", "mverilog", "-o", s"${name}.v"), generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
  } else {
    (new chisel3.stage.ChiselStage).execute(args, generator)
  }
}
