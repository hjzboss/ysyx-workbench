module Ps2(
  input        clock,
  input        reset,
  input        io_ps2Clk,
  input        io_ps2Data,
  output [7:0] io_current0,
  output [7:0] io_current1,
  output [7:0] io_asc0,
  output [7:0] io_asc1,
  output [7:0] io_total0,
  output [7:0] io_total1
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
`endif // RANDOMIZE_REG_INIT
  reg [9:0] buffer; // @[Ps2.scala 56:23]
  reg [3:0] count; // @[Ps2.scala 58:22]
  reg [2:0] ps2ClkSync; // @[Ps2.scala 60:27]
  reg [3:0] one; // @[Ps2.scala 64:20]
  reg [3:0] ten; // @[Ps2.scala 65:20]
  wire [3:0] _ps2ClkSync_T = {ps2ClkSync, 1'h0}; // @[Ps2.scala 67:29]
  wire [3:0] _GEN_196 = {{3'd0}, io_ps2Clk}; // @[Ps2.scala 67:37]
  wire [3:0] _ps2ClkSync_T_1 = _ps2ClkSync_T | _GEN_196; // @[Ps2.scala 67:37]
  wire  sampling = ps2ClkSync[2] & ~ps2ClkSync[1]; // @[Ps2.scala 68:29]
  reg [7:0] current0T; // @[Ps2.scala 70:26]
  reg [7:0] current1T; // @[Ps2.scala 71:26]
  reg [7:0] asc0T; // @[Ps2.scala 72:22]
  reg [7:0] asc1T; // @[Ps2.scala 73:22]
  wire  _T_1 = count == 4'ha; // @[Ps2.scala 79:17]
  wire  _T_9 = ~buffer[0] & io_ps2Data & ^buffer[9:1]; // @[Ps2.scala 80:57]
  wire  _T_12 = ~reset; // @[Ps2.scala 81:15]
  wire  _T_14 = buffer[8:1] != 8'hf0; // @[Ps2.scala 82:28]
  wire [7:0] _GEN_64 = 4'hf == buffer[4:1] ? 8'h8e : 8'hc0; // @[Ps2.scala 23:16 39:23]
  wire [7:0] _GEN_65 = 4'he == buffer[4:1] ? 8'h86 : _GEN_64; // @[Ps2.scala 23:16 38:23]
  wire [7:0] _GEN_66 = 4'hd == buffer[4:1] ? 8'ha1 : _GEN_65; // @[Ps2.scala 23:16 37:23]
  wire [7:0] _GEN_67 = 4'hc == buffer[4:1] ? 8'hc6 : _GEN_66; // @[Ps2.scala 23:16 36:23]
  wire [7:0] _GEN_68 = 4'hb == buffer[4:1] ? 8'h83 : _GEN_67; // @[Ps2.scala 23:16 35:23]
  wire [7:0] _GEN_69 = 4'ha == buffer[4:1] ? 8'h88 : _GEN_68; // @[Ps2.scala 23:16 34:23]
  wire [7:0] _GEN_70 = 4'h9 == buffer[4:1] ? 8'h90 : _GEN_69; // @[Ps2.scala 23:16 33:22]
  wire [7:0] _GEN_71 = 4'h8 == buffer[4:1] ? 8'h80 : _GEN_70; // @[Ps2.scala 23:16 32:22]
  wire [7:0] _GEN_72 = 4'h7 == buffer[4:1] ? 8'hf8 : _GEN_71; // @[Ps2.scala 23:16 31:22]
  wire [7:0] _GEN_73 = 4'h6 == buffer[4:1] ? 8'h82 : _GEN_72; // @[Ps2.scala 23:16 30:22]
  wire [7:0] _GEN_74 = 4'h5 == buffer[4:1] ? 8'h92 : _GEN_73; // @[Ps2.scala 23:16 29:22]
  wire [7:0] _GEN_75 = 4'h4 == buffer[4:1] ? 8'h99 : _GEN_74; // @[Ps2.scala 23:16 28:22]
  wire [7:0] _GEN_76 = 4'h3 == buffer[4:1] ? 8'hb0 : _GEN_75; // @[Ps2.scala 23:16 27:22]
  wire [7:0] _GEN_77 = 4'h2 == buffer[4:1] ? 8'ha4 : _GEN_76; // @[Ps2.scala 23:16 26:22]
  wire [7:0] _GEN_78 = 4'h1 == buffer[4:1] ? 8'hf9 : _GEN_77; // @[Ps2.scala 23:16 25:22]
  wire [7:0] current0T_seg_1 = 4'h0 == buffer[4:1] ? 8'hc0 : _GEN_78; // @[Ps2.scala 23:16 24:22]
  wire [7:0] _GEN_80 = 4'hf == buffer[8:5] ? 8'h8e : 8'hc0; // @[Ps2.scala 23:16 39:23]
  wire [7:0] _GEN_81 = 4'he == buffer[8:5] ? 8'h86 : _GEN_80; // @[Ps2.scala 23:16 38:23]
  wire [7:0] _GEN_82 = 4'hd == buffer[8:5] ? 8'ha1 : _GEN_81; // @[Ps2.scala 23:16 37:23]
  wire [7:0] _GEN_83 = 4'hc == buffer[8:5] ? 8'hc6 : _GEN_82; // @[Ps2.scala 23:16 36:23]
  wire [7:0] _GEN_84 = 4'hb == buffer[8:5] ? 8'h83 : _GEN_83; // @[Ps2.scala 23:16 35:23]
  wire [7:0] _GEN_85 = 4'ha == buffer[8:5] ? 8'h88 : _GEN_84; // @[Ps2.scala 23:16 34:23]
  wire [7:0] _GEN_86 = 4'h9 == buffer[8:5] ? 8'h90 : _GEN_85; // @[Ps2.scala 23:16 33:22]
  wire [7:0] _GEN_87 = 4'h8 == buffer[8:5] ? 8'h80 : _GEN_86; // @[Ps2.scala 23:16 32:22]
  wire [7:0] _GEN_88 = 4'h7 == buffer[8:5] ? 8'hf8 : _GEN_87; // @[Ps2.scala 23:16 31:22]
  wire [7:0] _GEN_89 = 4'h6 == buffer[8:5] ? 8'h82 : _GEN_88; // @[Ps2.scala 23:16 30:22]
  wire [7:0] _GEN_90 = 4'h5 == buffer[8:5] ? 8'h92 : _GEN_89; // @[Ps2.scala 23:16 29:22]
  wire [7:0] _GEN_91 = 4'h4 == buffer[8:5] ? 8'h99 : _GEN_90; // @[Ps2.scala 23:16 28:22]
  wire [7:0] _GEN_92 = 4'h3 == buffer[8:5] ? 8'hb0 : _GEN_91; // @[Ps2.scala 23:16 27:22]
  wire [7:0] _GEN_93 = 4'h2 == buffer[8:5] ? 8'ha4 : _GEN_92; // @[Ps2.scala 23:16 26:22]
  wire [7:0] _GEN_94 = 4'h1 == buffer[8:5] ? 8'hf9 : _GEN_93; // @[Ps2.scala 23:16 25:22]
  wire [7:0] current1T_seg_1 = 4'h0 == buffer[8:5] ? 8'hc0 : _GEN_94; // @[Ps2.scala 23:16 24:22]
  wire [7:0] _GEN_96 = 8'h2d == buffer[8:1] ? 8'h52 : 8'h0; // @[Ps2.scala 46:15 50:26]
  wire [7:0] _GEN_97 = 8'h24 == buffer[8:1] ? 8'h45 : _GEN_96; // @[Ps2.scala 46:15 49:26]
  wire [7:0] _GEN_98 = 8'h1d == buffer[8:1] ? 8'h57 : _GEN_97; // @[Ps2.scala 46:15 48:26]
  wire [7:0] asc0T_seg_1 = 8'h15 == buffer[8:1] ? 8'h51 : _GEN_98; // @[Ps2.scala 46:15 47:26]
  wire [7:0] _GEN_100 = 4'hf == asc0T_seg_1[3:0] ? 8'h8e : 8'hc0; // @[Ps2.scala 23:16 39:23]
  wire [7:0] _GEN_101 = 4'he == asc0T_seg_1[3:0] ? 8'h86 : _GEN_100; // @[Ps2.scala 23:16 38:23]
  wire [7:0] _GEN_102 = 4'hd == asc0T_seg_1[3:0] ? 8'ha1 : _GEN_101; // @[Ps2.scala 23:16 37:23]
  wire [7:0] _GEN_103 = 4'hc == asc0T_seg_1[3:0] ? 8'hc6 : _GEN_102; // @[Ps2.scala 23:16 36:23]
  wire [7:0] _GEN_104 = 4'hb == asc0T_seg_1[3:0] ? 8'h83 : _GEN_103; // @[Ps2.scala 23:16 35:23]
  wire [7:0] _GEN_105 = 4'ha == asc0T_seg_1[3:0] ? 8'h88 : _GEN_104; // @[Ps2.scala 23:16 34:23]
  wire [7:0] _GEN_106 = 4'h9 == asc0T_seg_1[3:0] ? 8'h90 : _GEN_105; // @[Ps2.scala 23:16 33:22]
  wire [7:0] _GEN_107 = 4'h8 == asc0T_seg_1[3:0] ? 8'h80 : _GEN_106; // @[Ps2.scala 23:16 32:22]
  wire [7:0] _GEN_108 = 4'h7 == asc0T_seg_1[3:0] ? 8'hf8 : _GEN_107; // @[Ps2.scala 23:16 31:22]
  wire [7:0] _GEN_109 = 4'h6 == asc0T_seg_1[3:0] ? 8'h82 : _GEN_108; // @[Ps2.scala 23:16 30:22]
  wire [7:0] _GEN_110 = 4'h5 == asc0T_seg_1[3:0] ? 8'h92 : _GEN_109; // @[Ps2.scala 23:16 29:22]
  wire [7:0] _GEN_111 = 4'h4 == asc0T_seg_1[3:0] ? 8'h99 : _GEN_110; // @[Ps2.scala 23:16 28:22]
  wire [7:0] _GEN_112 = 4'h3 == asc0T_seg_1[3:0] ? 8'hb0 : _GEN_111; // @[Ps2.scala 23:16 27:22]
  wire [7:0] _GEN_113 = 4'h2 == asc0T_seg_1[3:0] ? 8'ha4 : _GEN_112; // @[Ps2.scala 23:16 26:22]
  wire [7:0] _GEN_114 = 4'h1 == asc0T_seg_1[3:0] ? 8'hf9 : _GEN_113; // @[Ps2.scala 23:16 25:22]
  wire [7:0] asc0T_seg_2 = 4'h0 == asc0T_seg_1[3:0] ? 8'hc0 : _GEN_114; // @[Ps2.scala 23:16 24:22]
  wire [7:0] _GEN_120 = 4'hf == asc0T_seg_1[7:4] ? 8'h8e : 8'hc0; // @[Ps2.scala 23:16 39:23]
  wire [7:0] _GEN_121 = 4'he == asc0T_seg_1[7:4] ? 8'h86 : _GEN_120; // @[Ps2.scala 23:16 38:23]
  wire [7:0] _GEN_122 = 4'hd == asc0T_seg_1[7:4] ? 8'ha1 : _GEN_121; // @[Ps2.scala 23:16 37:23]
  wire [7:0] _GEN_123 = 4'hc == asc0T_seg_1[7:4] ? 8'hc6 : _GEN_122; // @[Ps2.scala 23:16 36:23]
  wire [7:0] _GEN_124 = 4'hb == asc0T_seg_1[7:4] ? 8'h83 : _GEN_123; // @[Ps2.scala 23:16 35:23]
  wire [7:0] _GEN_125 = 4'ha == asc0T_seg_1[7:4] ? 8'h88 : _GEN_124; // @[Ps2.scala 23:16 34:23]
  wire [7:0] _GEN_126 = 4'h9 == asc0T_seg_1[7:4] ? 8'h90 : _GEN_125; // @[Ps2.scala 23:16 33:22]
  wire [7:0] _GEN_127 = 4'h8 == asc0T_seg_1[7:4] ? 8'h80 : _GEN_126; // @[Ps2.scala 23:16 32:22]
  wire [7:0] _GEN_128 = 4'h7 == asc0T_seg_1[7:4] ? 8'hf8 : _GEN_127; // @[Ps2.scala 23:16 31:22]
  wire [7:0] _GEN_129 = 4'h6 == asc0T_seg_1[7:4] ? 8'h82 : _GEN_128; // @[Ps2.scala 23:16 30:22]
  wire [7:0] _GEN_130 = 4'h5 == asc0T_seg_1[7:4] ? 8'h92 : _GEN_129; // @[Ps2.scala 23:16 29:22]
  wire [7:0] _GEN_131 = 4'h4 == asc0T_seg_1[7:4] ? 8'h99 : _GEN_130; // @[Ps2.scala 23:16 28:22]
  wire [7:0] _GEN_132 = 4'h3 == asc0T_seg_1[7:4] ? 8'hb0 : _GEN_131; // @[Ps2.scala 23:16 27:22]
  wire [7:0] _GEN_133 = 4'h2 == asc0T_seg_1[7:4] ? 8'ha4 : _GEN_132; // @[Ps2.scala 23:16 26:22]
  wire [7:0] _GEN_134 = 4'h1 == asc0T_seg_1[7:4] ? 8'hf9 : _GEN_133; // @[Ps2.scala 23:16 25:22]
  wire [7:0] asc1T_seg_2 = 4'h0 == asc0T_seg_1[7:4] ? 8'hc0 : _GEN_134; // @[Ps2.scala 23:16 24:22]
  wire  _one_T = one == 4'h9; // @[Ps2.scala 88:26]
  wire [3:0] _one_T_2 = one + 4'h1; // @[Ps2.scala 88:44]
  wire [3:0] _one_T_3 = one == 4'h9 ? 4'h0 : _one_T_2; // @[Ps2.scala 88:21]
  wire [3:0] _ten_T_2 = ten + 4'h1; // @[Ps2.scala 89:39]
  wire [3:0] _ten_T_3 = _one_T ? _ten_T_2 : ten; // @[Ps2.scala 89:21]
  wire [7:0] _GEN_136 = buffer[8:1] != 8'hf0 ? current0T_seg_1 : current0T; // @[Ps2.scala 82:41 83:21 70:26]
  wire [7:0] _GEN_137 = buffer[8:1] != 8'hf0 ? current1T_seg_1 : current1T; // @[Ps2.scala 82:41 84:21 71:26]
  wire [7:0] _GEN_138 = buffer[8:1] != 8'hf0 ? asc0T_seg_2 : asc0T; // @[Ps2.scala 82:41 86:17 72:22]
  wire [7:0] _GEN_139 = buffer[8:1] != 8'hf0 ? asc1T_seg_2 : asc1T; // @[Ps2.scala 82:41 87:17 73:22]
  wire [3:0] _GEN_140 = buffer[8:1] != 8'hf0 ? _one_T_3 : one; // @[Ps2.scala 82:41 88:15 64:20]
  wire [3:0] _GEN_141 = buffer[8:1] != 8'hf0 ? _ten_T_3 : ten; // @[Ps2.scala 82:41 89:15 65:20]
  wire [15:0] _GEN_0 = {{15'd0}, io_ps2Data}; // @[Ps2.scala 95:54]
  wire [15:0] _buffer_T = _GEN_0 << count; // @[Ps2.scala 95:54]
  wire [15:0] _buffer_T_2 = ~_buffer_T; // @[Ps2.scala 95:28]
  wire [15:0] _GEN_197 = {{6'd0}, buffer}; // @[Ps2.scala 95:25]
  wire [15:0] _buffer_T_3 = _GEN_197 & _buffer_T_2; // @[Ps2.scala 95:25]
  wire [15:0] _buffer_T_5 = _buffer_T_3 | _buffer_T; // @[Ps2.scala 95:67]
  wire [3:0] _count_T_1 = count + 4'h1; // @[Ps2.scala 96:22]
  wire [15:0] _GEN_154 = count == 4'ha ? 16'h0 : _buffer_T_5; // @[Ps2.scala 79:27 92:14 95:14]
  wire [15:0] _GEN_162 = sampling ? _GEN_154 : {{6'd0}, buffer}; // @[Ps2.scala 56:23 78:27]
  wire [7:0] _GEN_164 = 4'hf == one ? 8'h8e : 8'hc0; // @[Ps2.scala 23:16 39:23]
  wire [7:0] _GEN_165 = 4'he == one ? 8'h86 : _GEN_164; // @[Ps2.scala 23:16 38:23]
  wire [7:0] _GEN_166 = 4'hd == one ? 8'ha1 : _GEN_165; // @[Ps2.scala 23:16 37:23]
  wire [7:0] _GEN_167 = 4'hc == one ? 8'hc6 : _GEN_166; // @[Ps2.scala 23:16 36:23]
  wire [7:0] _GEN_168 = 4'hb == one ? 8'h83 : _GEN_167; // @[Ps2.scala 23:16 35:23]
  wire [7:0] _GEN_169 = 4'ha == one ? 8'h88 : _GEN_168; // @[Ps2.scala 23:16 34:23]
  wire [7:0] _GEN_170 = 4'h9 == one ? 8'h90 : _GEN_169; // @[Ps2.scala 23:16 33:22]
  wire [7:0] _GEN_171 = 4'h8 == one ? 8'h80 : _GEN_170; // @[Ps2.scala 23:16 32:22]
  wire [7:0] _GEN_172 = 4'h7 == one ? 8'hf8 : _GEN_171; // @[Ps2.scala 23:16 31:22]
  wire [7:0] _GEN_173 = 4'h6 == one ? 8'h82 : _GEN_172; // @[Ps2.scala 23:16 30:22]
  wire [7:0] _GEN_174 = 4'h5 == one ? 8'h92 : _GEN_173; // @[Ps2.scala 23:16 29:22]
  wire [7:0] _GEN_175 = 4'h4 == one ? 8'h99 : _GEN_174; // @[Ps2.scala 23:16 28:22]
  wire [7:0] _GEN_176 = 4'h3 == one ? 8'hb0 : _GEN_175; // @[Ps2.scala 23:16 27:22]
  wire [7:0] _GEN_177 = 4'h2 == one ? 8'ha4 : _GEN_176; // @[Ps2.scala 23:16 26:22]
  wire [7:0] _GEN_178 = 4'h1 == one ? 8'hf9 : _GEN_177; // @[Ps2.scala 23:16 25:22]
  wire [7:0] _GEN_180 = 4'hf == ten ? 8'h8e : 8'hc0; // @[Ps2.scala 23:16 39:23]
  wire [7:0] _GEN_181 = 4'he == ten ? 8'h86 : _GEN_180; // @[Ps2.scala 23:16 38:23]
  wire [7:0] _GEN_182 = 4'hd == ten ? 8'ha1 : _GEN_181; // @[Ps2.scala 23:16 37:23]
  wire [7:0] _GEN_183 = 4'hc == ten ? 8'hc6 : _GEN_182; // @[Ps2.scala 23:16 36:23]
  wire [7:0] _GEN_184 = 4'hb == ten ? 8'h83 : _GEN_183; // @[Ps2.scala 23:16 35:23]
  wire [7:0] _GEN_185 = 4'ha == ten ? 8'h88 : _GEN_184; // @[Ps2.scala 23:16 34:23]
  wire [7:0] _GEN_186 = 4'h9 == ten ? 8'h90 : _GEN_185; // @[Ps2.scala 23:16 33:22]
  wire [7:0] _GEN_187 = 4'h8 == ten ? 8'h80 : _GEN_186; // @[Ps2.scala 23:16 32:22]
  wire [7:0] _GEN_188 = 4'h7 == ten ? 8'hf8 : _GEN_187; // @[Ps2.scala 23:16 31:22]
  wire [7:0] _GEN_189 = 4'h6 == ten ? 8'h82 : _GEN_188; // @[Ps2.scala 23:16 30:22]
  wire [7:0] _GEN_190 = 4'h5 == ten ? 8'h92 : _GEN_189; // @[Ps2.scala 23:16 29:22]
  wire [7:0] _GEN_191 = 4'h4 == ten ? 8'h99 : _GEN_190; // @[Ps2.scala 23:16 28:22]
  wire [7:0] _GEN_192 = 4'h3 == ten ? 8'hb0 : _GEN_191; // @[Ps2.scala 23:16 27:22]
  wire [7:0] _GEN_193 = 4'h2 == ten ? 8'ha4 : _GEN_192; // @[Ps2.scala 23:16 26:22]
  wire [7:0] _GEN_194 = 4'h1 == ten ? 8'hf9 : _GEN_193; // @[Ps2.scala 23:16 25:22]
  wire [15:0] _GEN_198 = reset ? 16'h0 : _GEN_162; // @[Ps2.scala 56:{23,23}]
  wire [3:0] _GEN_199 = reset ? 4'h0 : _ps2ClkSync_T_1; // @[Ps2.scala 60:{27,27} 67:14]
  wire  _GEN_201 = sampling & _T_1 & _T_9; // @[Ps2.scala 81:15]
  assign io_current0 = current0T; // @[Ps2.scala 74:15]
  assign io_current1 = current1T; // @[Ps2.scala 75:15]
  assign io_asc0 = asc0T; // @[Ps2.scala 76:11]
  assign io_asc1 = asc1T; // @[Ps2.scala 77:11]
  assign io_total0 = 4'h0 == one ? 8'hc0 : _GEN_178; // @[Ps2.scala 23:16 24:22]
  assign io_total1 = 4'h0 == ten ? 8'hc0 : _GEN_194; // @[Ps2.scala 23:16 24:22]
  always @(posedge clock) begin
    buffer <= _GEN_198[9:0]; // @[Ps2.scala 56:{23,23}]
    if (reset) begin // @[Ps2.scala 58:22]
      count <= 4'h0; // @[Ps2.scala 58:22]
    end else if (sampling) begin // @[Ps2.scala 78:27]
      if (count == 4'ha) begin // @[Ps2.scala 79:27]
        count <= 4'h0; // @[Ps2.scala 93:13]
      end else begin
        count <= _count_T_1; // @[Ps2.scala 96:13]
      end
    end
    ps2ClkSync <= _GEN_199[2:0]; // @[Ps2.scala 60:{27,27} 67:14]
    if (reset) begin // @[Ps2.scala 64:20]
      one <= 4'h0; // @[Ps2.scala 64:20]
    end else if (sampling) begin // @[Ps2.scala 78:27]
      if (count == 4'ha) begin // @[Ps2.scala 79:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 80:89]
          one <= _GEN_140;
        end
      end
    end
    if (reset) begin // @[Ps2.scala 65:20]
      ten <= 4'h0; // @[Ps2.scala 65:20]
    end else if (sampling) begin // @[Ps2.scala 78:27]
      if (count == 4'ha) begin // @[Ps2.scala 79:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 80:89]
          ten <= _GEN_141;
        end
      end
    end
    if (reset) begin // @[Ps2.scala 70:26]
      current0T <= 8'hc0; // @[Ps2.scala 70:26]
    end else if (sampling) begin // @[Ps2.scala 78:27]
      if (count == 4'ha) begin // @[Ps2.scala 79:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 80:89]
          current0T <= _GEN_136;
        end
      end
    end
    if (reset) begin // @[Ps2.scala 71:26]
      current1T <= 8'hc0; // @[Ps2.scala 71:26]
    end else if (sampling) begin // @[Ps2.scala 78:27]
      if (count == 4'ha) begin // @[Ps2.scala 79:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 80:89]
          current1T <= _GEN_137;
        end
      end
    end
    if (reset) begin // @[Ps2.scala 72:22]
      asc0T <= 8'hc0; // @[Ps2.scala 72:22]
    end else if (sampling) begin // @[Ps2.scala 78:27]
      if (count == 4'ha) begin // @[Ps2.scala 79:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 80:89]
          asc0T <= _GEN_138;
        end
      end
    end
    if (reset) begin // @[Ps2.scala 73:22]
      asc1T <= 8'hc0; // @[Ps2.scala 73:22]
    end else if (sampling) begin // @[Ps2.scala 78:27]
      if (count == 4'ha) begin // @[Ps2.scala 79:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 80:89]
          asc1T <= _GEN_139;
        end
      end
    end
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (sampling & _T_1 & _T_9 & ~reset) begin
          $fwrite(32'h80000002,"buffer=%x\n",buffer[8:1]); // @[Ps2.scala 81:15]
        end
    `ifdef PRINTF_COND
      end
    `endif
    `endif // SYNTHESIS
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (_GEN_201 & _T_14 & _T_12) begin
          $fwrite(32'h80000002,"current=%x%x\n",io_current1,io_current0); // @[Ps2.scala 85:17]
        end
    `ifdef PRINTF_COND
      end
    `endif
    `endif // SYNTHESIS
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  buffer = _RAND_0[9:0];
  _RAND_1 = {1{`RANDOM}};
  count = _RAND_1[3:0];
  _RAND_2 = {1{`RANDOM}};
  ps2ClkSync = _RAND_2[2:0];
  _RAND_3 = {1{`RANDOM}};
  one = _RAND_3[3:0];
  _RAND_4 = {1{`RANDOM}};
  ten = _RAND_4[3:0];
  _RAND_5 = {1{`RANDOM}};
  current0T = _RAND_5[7:0];
  _RAND_6 = {1{`RANDOM}};
  current1T = _RAND_6[7:0];
  _RAND_7 = {1{`RANDOM}};
  asc0T = _RAND_7[7:0];
  _RAND_8 = {1{`RANDOM}};
  asc1T = _RAND_8[7:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
