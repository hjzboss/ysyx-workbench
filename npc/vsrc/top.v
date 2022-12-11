module Alu(
  input        clock,
  input        reset,
  input  [3:0] io_srcA,
  input  [3:0] io_srcB,
  input  [2:0] io_sel,
  output [3:0] io_out,
  output       io_overflow
);
  wire [3:0] _io_out_T_2 = $signed(io_srcA) + $signed(io_srcB); // @[Alu.scala 19:25]
  wire  _T_1 = $signed(io_srcA) >= 4'sh0; // @[Alu.scala 20:22]
  wire  _T_2 = $signed(io_srcB) >= 4'sh0; // @[Alu.scala 20:40]
  wire  _T_4 = $signed(io_out) < 4'sh0; // @[Alu.scala 20:57]
  wire  _T_6 = $signed(io_srcA) <= 4'sh0; // @[Alu.scala 20:76]
  wire  _T_7 = $signed(io_srcB) <= 4'sh0; // @[Alu.scala 20:94]
  wire  _T_9 = $signed(io_out) > 4'sh0; // @[Alu.scala 20:111]
  wire  _T_11 = $signed(io_srcA) >= 4'sh0 & $signed(io_srcB) >= 4'sh0 & $signed(io_out) < 4'sh0 | $signed(io_srcA) <= 4'sh0
     & $signed(io_srcB) <= 4'sh0 & $signed(io_out) > 4'sh0; // @[Alu.scala 20:64]
  wire [3:0] _io_out_T_5 = $signed(io_srcA) - $signed(io_srcB); // @[Alu.scala 24:25]
  wire [3:0] _io_out_T_7 = ~io_srcA; // @[Alu.scala 27:31]
  wire [3:0] _io_out_T_9 = $signed(io_srcA) & $signed(io_srcB); // @[Alu.scala 28:39]
  wire [3:0] _io_out_T_11 = $signed(io_srcA) | $signed(io_srcB); // @[Alu.scala 29:39]
  wire [3:0] _io_out_T_13 = $signed(io_srcA) ^ $signed(io_srcB); // @[Alu.scala 30:39]
  wire [1:0] _GEN_1 = $signed(io_srcA) < $signed(io_srcB) ? $signed(2'sh1) : $signed(2'sh0); // @[Alu.scala 32:32 33:16 35:16]
  wire [1:0] _GEN_2 = $signed(io_srcA) == $signed(io_srcB) ? $signed(2'sh1) : $signed(2'sh0); // @[Alu.scala 39:34 40:16 42:16]
  wire [1:0] _GEN_3 = 3'h7 == io_sel ? $signed(_GEN_2) : $signed(2'sh0); // @[Alu.scala 15:10 17:19]
  wire [1:0] _GEN_4 = 3'h6 == io_sel ? $signed(_GEN_1) : $signed(_GEN_3); // @[Alu.scala 17:19]
  wire [3:0] _GEN_5 = 3'h5 == io_sel ? $signed(_io_out_T_13) : $signed({{2{_GEN_4[1]}},_GEN_4}); // @[Alu.scala 17:19 30:28]
  wire [3:0] _GEN_6 = 3'h4 == io_sel ? $signed(_io_out_T_11) : $signed(_GEN_5); // @[Alu.scala 17:19 29:28]
  wire [3:0] _GEN_7 = 3'h3 == io_sel ? $signed(_io_out_T_9) : $signed(_GEN_6); // @[Alu.scala 17:19 28:28]
  wire [3:0] _GEN_8 = 3'h2 == io_sel ? $signed(_io_out_T_7) : $signed(_GEN_7); // @[Alu.scala 17:19 27:28]
  wire [3:0] _GEN_9 = 3'h1 == io_sel ? $signed(_io_out_T_5) : $signed(_GEN_8); // @[Alu.scala 17:19 24:14]
  wire  _GEN_10 = 3'h1 == io_sel & (_T_1 & _T_7 & _T_4 | _T_6 & _T_2 & _T_9); // @[Alu.scala 16:15 17:19 25:19]
  assign io_out = 3'h0 == io_sel ? $signed(_io_out_T_2) : $signed(_GEN_9); // @[Alu.scala 17:19 19:14]
  assign io_overflow = 3'h0 == io_sel ? _T_11 : _GEN_10; // @[Alu.scala 17:19]
endmodule
