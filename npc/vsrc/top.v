module top(
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
`endif // RANDOMIZE_REG_INIT
  reg [9:0] buffer; // @[Ps2.scala 63:23]
  reg [3:0] count; // @[Ps2.scala 65:22]
  reg [2:0] ps2ClkSync; // @[Ps2.scala 67:27]
  reg [3:0] one; // @[Ps2.scala 71:20]
  reg [3:0] ten; // @[Ps2.scala 72:20]
  wire [3:0] _ps2ClkSync_T = {ps2ClkSync, 1'h0}; // @[Ps2.scala 74:29]
  wire [3:0] _GEN_100 = {{3'd0}, io_ps2Clk}; // @[Ps2.scala 74:37]
  wire [3:0] _ps2ClkSync_T_1 = _ps2ClkSync_T | _GEN_100; // @[Ps2.scala 74:37]
  wire  sampling = ps2ClkSync[2] & ~ps2ClkSync[1]; // @[Ps2.scala 75:29]
  wire  _T_1 = count == 4'ha; // @[Ps2.scala 78:17]
  wire  _T_9 = ~buffer[0] & io_ps2Data & ^buffer[9:1]; // @[Ps2.scala 79:57]
  wire  _T_11 = buffer[8:1] != 8'hf0; // @[Ps2.scala 80:28]
  wire [7:0] _GEN_0 = 4'hf == buffer[4:1] ? 8'h8e : 8'hc0; // @[Ps2.scala 30:16 46:23]
  wire [7:0] _GEN_1 = 4'he == buffer[4:1] ? 8'h86 : _GEN_0; // @[Ps2.scala 30:16 45:23]
  wire [7:0] _GEN_2 = 4'hd == buffer[4:1] ? 8'ha1 : _GEN_1; // @[Ps2.scala 30:16 44:23]
  wire [7:0] _GEN_3 = 4'hc == buffer[4:1] ? 8'hc6 : _GEN_2; // @[Ps2.scala 30:16 43:23]
  wire [7:0] _GEN_4 = 4'hb == buffer[4:1] ? 8'h83 : _GEN_3; // @[Ps2.scala 30:16 42:23]
  wire [7:0] _GEN_5 = 4'ha == buffer[4:1] ? 8'h88 : _GEN_4; // @[Ps2.scala 30:16 41:23]
  wire [7:0] _GEN_6 = 4'h9 == buffer[4:1] ? 8'h90 : _GEN_5; // @[Ps2.scala 30:16 40:22]
  wire [7:0] _GEN_7 = 4'h8 == buffer[4:1] ? 8'h80 : _GEN_6; // @[Ps2.scala 30:16 39:22]
  wire [7:0] _GEN_8 = 4'h7 == buffer[4:1] ? 8'hf8 : _GEN_7; // @[Ps2.scala 30:16 38:22]
  wire [7:0] _GEN_9 = 4'h6 == buffer[4:1] ? 8'h82 : _GEN_8; // @[Ps2.scala 30:16 37:22]
  wire [7:0] _GEN_10 = 4'h5 == buffer[4:1] ? 8'h92 : _GEN_9; // @[Ps2.scala 30:16 36:22]
  wire [7:0] _GEN_11 = 4'h4 == buffer[4:1] ? 8'h99 : _GEN_10; // @[Ps2.scala 30:16 35:22]
  wire [7:0] _GEN_12 = 4'h3 == buffer[4:1] ? 8'hb0 : _GEN_11; // @[Ps2.scala 30:16 34:22]
  wire [7:0] _GEN_13 = 4'h2 == buffer[4:1] ? 8'ha4 : _GEN_12; // @[Ps2.scala 30:16 33:22]
  wire [7:0] _GEN_14 = 4'h1 == buffer[4:1] ? 8'hf9 : _GEN_13; // @[Ps2.scala 30:16 32:22]
  wire [7:0] io_current0_seg = 4'h0 == buffer[4:1] ? 8'hc0 : _GEN_14; // @[Ps2.scala 30:16 31:22]
  wire [7:0] _GEN_16 = 4'hf == buffer[8:5] ? 8'h8e : 8'hc0; // @[Ps2.scala 30:16 46:23]
  wire [7:0] _GEN_17 = 4'he == buffer[8:5] ? 8'h86 : _GEN_16; // @[Ps2.scala 30:16 45:23]
  wire [7:0] _GEN_18 = 4'hd == buffer[8:5] ? 8'ha1 : _GEN_17; // @[Ps2.scala 30:16 44:23]
  wire [7:0] _GEN_19 = 4'hc == buffer[8:5] ? 8'hc6 : _GEN_18; // @[Ps2.scala 30:16 43:23]
  wire [7:0] _GEN_20 = 4'hb == buffer[8:5] ? 8'h83 : _GEN_19; // @[Ps2.scala 30:16 42:23]
  wire [7:0] _GEN_21 = 4'ha == buffer[8:5] ? 8'h88 : _GEN_20; // @[Ps2.scala 30:16 41:23]
  wire [7:0] _GEN_22 = 4'h9 == buffer[8:5] ? 8'h90 : _GEN_21; // @[Ps2.scala 30:16 40:22]
  wire [7:0] _GEN_23 = 4'h8 == buffer[8:5] ? 8'h80 : _GEN_22; // @[Ps2.scala 30:16 39:22]
  wire [7:0] _GEN_24 = 4'h7 == buffer[8:5] ? 8'hf8 : _GEN_23; // @[Ps2.scala 30:16 38:22]
  wire [7:0] _GEN_25 = 4'h6 == buffer[8:5] ? 8'h82 : _GEN_24; // @[Ps2.scala 30:16 37:22]
  wire [7:0] _GEN_26 = 4'h5 == buffer[8:5] ? 8'h92 : _GEN_25; // @[Ps2.scala 30:16 36:22]
  wire [7:0] _GEN_27 = 4'h4 == buffer[8:5] ? 8'h99 : _GEN_26; // @[Ps2.scala 30:16 35:22]
  wire [7:0] _GEN_28 = 4'h3 == buffer[8:5] ? 8'hb0 : _GEN_27; // @[Ps2.scala 30:16 34:22]
  wire [7:0] _GEN_29 = 4'h2 == buffer[8:5] ? 8'ha4 : _GEN_28; // @[Ps2.scala 30:16 33:22]
  wire [7:0] _GEN_30 = 4'h1 == buffer[8:5] ? 8'hf9 : _GEN_29; // @[Ps2.scala 30:16 32:22]
  wire [7:0] io_current1_seg = 4'h0 == buffer[8:5] ? 8'hc0 : _GEN_30; // @[Ps2.scala 30:16 31:22]
  wire [7:0] _GEN_32 = 8'h2d == buffer[8:1] ? 8'h52 : 8'h0; // @[Ps2.scala 53:15 57:26]
  wire [7:0] _GEN_33 = 8'h24 == buffer[8:1] ? 8'h45 : _GEN_32; // @[Ps2.scala 53:15 56:26]
  wire [7:0] _GEN_34 = 8'h1d == buffer[8:1] ? 8'h57 : _GEN_33; // @[Ps2.scala 53:15 55:26]
  wire [7:0] io_asc0_seg = 8'h15 == buffer[8:1] ? 8'h51 : _GEN_34; // @[Ps2.scala 53:15 54:26]
  wire  _one_T = one == 4'h9; // @[Ps2.scala 86:26]
  wire [3:0] _one_T_2 = one + 4'h1; // @[Ps2.scala 86:44]
  wire [3:0] _one_T_3 = one == 4'h9 ? 4'h0 : _one_T_2; // @[Ps2.scala 86:21]
  wire [3:0] _ten_T_2 = ten + 4'h1; // @[Ps2.scala 87:39]
  wire [3:0] _ten_T_3 = _one_T ? _ten_T_2 : ten; // @[Ps2.scala 87:21]
  wire [7:0] _GEN_40 = buffer[8:1] != 8'hf0 ? io_current0_seg : 8'h0; // @[Ps2.scala 21:15 80:41 81:23]
  wire [7:0] _GEN_41 = buffer[8:1] != 8'hf0 ? io_current1_seg : 8'h0; // @[Ps2.scala 22:15 80:41 82:23]
  wire [3:0] _GEN_42 = buffer[8:1] != 8'hf0 ? io_asc0_seg[3:0] : 4'h0; // @[Ps2.scala 25:11 80:41 84:19]
  wire [3:0] _GEN_43 = buffer[8:1] != 8'hf0 ? io_asc0_seg[7:4] : 4'h0; // @[Ps2.scala 26:11 80:41 85:19]
  wire [3:0] _GEN_44 = buffer[8:1] != 8'hf0 ? _one_T_3 : one; // @[Ps2.scala 80:41 86:15 71:20]
  wire [3:0] _GEN_45 = buffer[8:1] != 8'hf0 ? _ten_T_3 : ten; // @[Ps2.scala 80:41 87:15 72:20]
  wire [7:0] _GEN_46 = ~buffer[0] & io_ps2Data & ^buffer[9:1] ? _GEN_40 : 8'h0; // @[Ps2.scala 21:15 79:89]
  wire [7:0] _GEN_47 = ~buffer[0] & io_ps2Data & ^buffer[9:1] ? _GEN_41 : 8'h0; // @[Ps2.scala 22:15 79:89]
  wire [3:0] _GEN_48 = ~buffer[0] & io_ps2Data & ^buffer[9:1] ? _GEN_42 : 4'h0; // @[Ps2.scala 25:11 79:89]
  wire [3:0] _GEN_49 = ~buffer[0] & io_ps2Data & ^buffer[9:1] ? _GEN_43 : 4'h0; // @[Ps2.scala 26:11 79:89]
  wire [15:0] _GEN_15 = {{15'd0}, io_ps2Data}; // @[Ps2.scala 92:38]
  wire [15:0] _buffer_T = _GEN_15 << count; // @[Ps2.scala 92:38]
  wire [15:0] _GEN_101 = {{6'd0}, buffer}; // @[Ps2.scala 92:24]
  wire [15:0] _buffer_T_1 = _GEN_101 | _buffer_T; // @[Ps2.scala 92:24]
  wire [3:0] _count_T_1 = count + 4'h1; // @[Ps2.scala 93:22]
  wire [7:0] _GEN_52 = count == 4'ha ? _GEN_46 : 8'h0; // @[Ps2.scala 21:15 78:27]
  wire [7:0] _GEN_53 = count == 4'ha ? _GEN_47 : 8'h0; // @[Ps2.scala 22:15 78:27]
  wire [3:0] _GEN_54 = count == 4'ha ? _GEN_48 : 4'h0; // @[Ps2.scala 25:11 78:27]
  wire [3:0] _GEN_55 = count == 4'ha ? _GEN_49 : 4'h0; // @[Ps2.scala 26:11 78:27]
  wire [15:0] _GEN_59 = count == 4'ha ? {{6'd0}, buffer} : _buffer_T_1; // @[Ps2.scala 63:23 78:27 92:14]
  wire [3:0] _GEN_62 = sampling ? _GEN_54 : 4'h0; // @[Ps2.scala 25:11 77:27]
  wire [3:0] _GEN_63 = sampling ? _GEN_55 : 4'h0; // @[Ps2.scala 26:11 77:27]
  wire [15:0] _GEN_67 = sampling ? _GEN_59 : {{6'd0}, buffer}; // @[Ps2.scala 63:23 77:27]
  wire [7:0] _GEN_68 = 4'hf == one ? 8'h8e : 8'hc0; // @[Ps2.scala 30:16 46:23]
  wire [7:0] _GEN_69 = 4'he == one ? 8'h86 : _GEN_68; // @[Ps2.scala 30:16 45:23]
  wire [7:0] _GEN_70 = 4'hd == one ? 8'ha1 : _GEN_69; // @[Ps2.scala 30:16 44:23]
  wire [7:0] _GEN_71 = 4'hc == one ? 8'hc6 : _GEN_70; // @[Ps2.scala 30:16 43:23]
  wire [7:0] _GEN_72 = 4'hb == one ? 8'h83 : _GEN_71; // @[Ps2.scala 30:16 42:23]
  wire [7:0] _GEN_73 = 4'ha == one ? 8'h88 : _GEN_72; // @[Ps2.scala 30:16 41:23]
  wire [7:0] _GEN_74 = 4'h9 == one ? 8'h90 : _GEN_73; // @[Ps2.scala 30:16 40:22]
  wire [7:0] _GEN_75 = 4'h8 == one ? 8'h80 : _GEN_74; // @[Ps2.scala 30:16 39:22]
  wire [7:0] _GEN_76 = 4'h7 == one ? 8'hf8 : _GEN_75; // @[Ps2.scala 30:16 38:22]
  wire [7:0] _GEN_77 = 4'h6 == one ? 8'h82 : _GEN_76; // @[Ps2.scala 30:16 37:22]
  wire [7:0] _GEN_78 = 4'h5 == one ? 8'h92 : _GEN_77; // @[Ps2.scala 30:16 36:22]
  wire [7:0] _GEN_79 = 4'h4 == one ? 8'h99 : _GEN_78; // @[Ps2.scala 30:16 35:22]
  wire [7:0] _GEN_80 = 4'h3 == one ? 8'hb0 : _GEN_79; // @[Ps2.scala 30:16 34:22]
  wire [7:0] _GEN_81 = 4'h2 == one ? 8'ha4 : _GEN_80; // @[Ps2.scala 30:16 33:22]
  wire [7:0] _GEN_82 = 4'h1 == one ? 8'hf9 : _GEN_81; // @[Ps2.scala 30:16 32:22]
  wire [7:0] _GEN_84 = 4'hf == ten ? 8'h8e : 8'hc0; // @[Ps2.scala 30:16 46:23]
  wire [7:0] _GEN_85 = 4'he == ten ? 8'h86 : _GEN_84; // @[Ps2.scala 30:16 45:23]
  wire [7:0] _GEN_86 = 4'hd == ten ? 8'ha1 : _GEN_85; // @[Ps2.scala 30:16 44:23]
  wire [7:0] _GEN_87 = 4'hc == ten ? 8'hc6 : _GEN_86; // @[Ps2.scala 30:16 43:23]
  wire [7:0] _GEN_88 = 4'hb == ten ? 8'h83 : _GEN_87; // @[Ps2.scala 30:16 42:23]
  wire [7:0] _GEN_89 = 4'ha == ten ? 8'h88 : _GEN_88; // @[Ps2.scala 30:16 41:23]
  wire [7:0] _GEN_90 = 4'h9 == ten ? 8'h90 : _GEN_89; // @[Ps2.scala 30:16 40:22]
  wire [7:0] _GEN_91 = 4'h8 == ten ? 8'h80 : _GEN_90; // @[Ps2.scala 30:16 39:22]
  wire [7:0] _GEN_92 = 4'h7 == ten ? 8'hf8 : _GEN_91; // @[Ps2.scala 30:16 38:22]
  wire [7:0] _GEN_93 = 4'h6 == ten ? 8'h82 : _GEN_92; // @[Ps2.scala 30:16 37:22]
  wire [7:0] _GEN_94 = 4'h5 == ten ? 8'h92 : _GEN_93; // @[Ps2.scala 30:16 36:22]
  wire [7:0] _GEN_95 = 4'h4 == ten ? 8'h99 : _GEN_94; // @[Ps2.scala 30:16 35:22]
  wire [7:0] _GEN_96 = 4'h3 == ten ? 8'hb0 : _GEN_95; // @[Ps2.scala 30:16 34:22]
  wire [7:0] _GEN_97 = 4'h2 == ten ? 8'ha4 : _GEN_96; // @[Ps2.scala 30:16 33:22]
  wire [7:0] _GEN_98 = 4'h1 == ten ? 8'hf9 : _GEN_97; // @[Ps2.scala 30:16 32:22]
  wire [15:0] _GEN_102 = reset ? 16'h0 : _GEN_67; // @[Ps2.scala 63:{23,23}]
  wire [3:0] _GEN_103 = reset ? 4'h0 : _ps2ClkSync_T_1; // @[Ps2.scala 67:{27,27} 74:14]
  assign io_current0 = sampling ? _GEN_52 : 8'h0; // @[Ps2.scala 21:15 77:27]
  assign io_current1 = sampling ? _GEN_53 : 8'h0; // @[Ps2.scala 22:15 77:27]
  assign io_asc0 = {{4'd0}, _GEN_62};
  assign io_asc1 = {{4'd0}, _GEN_63};
  assign io_total0 = 4'h0 == one ? 8'hc0 : _GEN_82; // @[Ps2.scala 30:16 31:22]
  assign io_total1 = 4'h0 == ten ? 8'hc0 : _GEN_98; // @[Ps2.scala 30:16 31:22]
  always @(posedge clock) begin
    buffer <= _GEN_102[9:0]; // @[Ps2.scala 63:{23,23}]
    if (reset) begin // @[Ps2.scala 65:22]
      count <= 4'h0; // @[Ps2.scala 65:22]
    end else if (sampling) begin // @[Ps2.scala 77:27]
      if (count == 4'ha) begin // @[Ps2.scala 78:27]
        count <= 4'h0; // @[Ps2.scala 90:13]
      end else begin
        count <= _count_T_1; // @[Ps2.scala 93:13]
      end
    end
    ps2ClkSync <= _GEN_103[2:0]; // @[Ps2.scala 67:{27,27} 74:14]
    if (reset) begin // @[Ps2.scala 71:20]
      one <= 4'h0; // @[Ps2.scala 71:20]
    end else if (sampling) begin // @[Ps2.scala 77:27]
      if (count == 4'ha) begin // @[Ps2.scala 78:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 79:89]
          one <= _GEN_44;
        end
      end
    end
    if (reset) begin // @[Ps2.scala 72:20]
      ten <= 4'h0; // @[Ps2.scala 72:20]
    end else if (sampling) begin // @[Ps2.scala 77:27]
      if (count == 4'ha) begin // @[Ps2.scala 78:27]
        if (~buffer[0] & io_ps2Data & ^buffer[9:1]) begin // @[Ps2.scala 79:89]
          ten <= _GEN_45;
        end
      end
    end
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (sampling & _T_1 & _T_9 & _T_11 & ~reset) begin
          $fwrite(32'h80000002,"%x",buffer[8:1]); // @[Ps2.scala 83:17]
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
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
