module top(
  input        clock,
  input        reset,
  output [6:0] io_seg0,
  output [6:0] io_seg1
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg [31:0] cnt; // @[Count.scala 12:20]
  reg [3:0] one; // @[Count.scala 13:20]
  reg [3:0] ten; // @[Count.scala 14:20]
  wire  _cnt_T = cnt == 32'hffff; // @[Count.scala 16:18]
  wire [31:0] _cnt_T_2 = cnt + 32'h1; // @[Count.scala 16:40]
  wire [3:0] _ten_T_2 = ten + 4'h1; // @[Count.scala 20:40]
  wire [3:0] _one_T_1 = one + 4'h1; // @[Count.scala 22:18]
  wire [6:0] _GEN_4 = 4'h9 == one ? 7'h10 : 7'h40; // @[Count.scala 27:11 29:16 38:24]
  wire [6:0] _GEN_5 = 4'h8 == one ? 7'h0 : _GEN_4; // @[Count.scala 29:16 37:24]
  wire [6:0] _GEN_6 = 4'h7 == one ? 7'h78 : _GEN_5; // @[Count.scala 29:16 36:24]
  wire [6:0] _GEN_7 = 4'h6 == one ? 7'h2 : _GEN_6; // @[Count.scala 29:16 35:24]
  wire [6:0] _GEN_8 = 4'h5 == one ? 7'h12 : _GEN_7; // @[Count.scala 29:16 34:24]
  wire [6:0] _GEN_9 = 4'h4 == one ? 7'h19 : _GEN_8; // @[Count.scala 29:16 33:24]
  wire [6:0] _GEN_10 = 4'h3 == one ? 7'h30 : _GEN_9; // @[Count.scala 29:16 32:24]
  wire [6:0] _GEN_11 = 4'h2 == one ? 7'h24 : _GEN_10; // @[Count.scala 29:16 31:24]
  wire [6:0] _GEN_13 = 4'h9 == ten ? 7'h10 : 7'h40; // @[Count.scala 28:11 40:16 49:24]
  wire [6:0] _GEN_14 = 4'h8 == ten ? 7'h0 : _GEN_13; // @[Count.scala 40:16 48:24]
  wire [6:0] _GEN_15 = 4'h7 == ten ? 7'h78 : _GEN_14; // @[Count.scala 40:16 47:24]
  wire [6:0] _GEN_16 = 4'h6 == ten ? 7'h2 : _GEN_15; // @[Count.scala 40:16 46:24]
  wire [6:0] _GEN_17 = 4'h5 == ten ? 7'h12 : _GEN_16; // @[Count.scala 40:16 45:24]
  wire [6:0] _GEN_18 = 4'h4 == ten ? 7'h19 : _GEN_17; // @[Count.scala 40:16 44:24]
  wire [6:0] _GEN_19 = 4'h3 == ten ? 7'h30 : _GEN_18; // @[Count.scala 40:16 43:24]
  wire [6:0] _GEN_20 = 4'h2 == ten ? 7'h24 : _GEN_19; // @[Count.scala 40:16 42:24]
  assign io_seg0 = 4'h1 == one ? 7'h79 : _GEN_11; // @[Count.scala 29:16 30:24]
  assign io_seg1 = 4'h1 == ten ? 7'h79 : _GEN_20; // @[Count.scala 40:16 41:24]
  always @(posedge clock) begin
    if (reset) begin // @[Count.scala 12:20]
      cnt <= 32'h0; // @[Count.scala 12:20]
    end else if (cnt == 32'hffff) begin // @[Count.scala 16:13]
      cnt <= 32'h0;
    end else begin
      cnt <= _cnt_T_2;
    end
    if (reset) begin // @[Count.scala 13:20]
      one <= 4'h0; // @[Count.scala 13:20]
    end else if (_cnt_T) begin // @[Count.scala 17:26]
      if (one == 4'h9) begin // @[Count.scala 18:24]
        one <= 4'h0; // @[Count.scala 19:11]
      end else begin
        one <= _one_T_1; // @[Count.scala 22:11]
      end
    end
    if (reset) begin // @[Count.scala 14:20]
      ten <= 4'h0; // @[Count.scala 14:20]
    end else if (_cnt_T) begin // @[Count.scala 17:26]
      if (one == 4'h9) begin // @[Count.scala 18:24]
        if (ten == 4'h9) begin // @[Count.scala 20:17]
          ten <= 4'h0;
        end else begin
          ten <= _ten_T_2;
        end
      end
    end
    `ifndef SYNTHESIS
    `ifdef PRINTF_COND
      if (`PRINTF_COND) begin
    `endif
        if (~reset) begin
          $fwrite(32'h80000002,"ten = %d, one = %d\n",ten,one); // @[Count.scala 52:9]
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
  cnt = _RAND_0[31:0];
  _RAND_1 = {1{`RANDOM}};
  one = _RAND_1[3:0];
  _RAND_2 = {1{`RANDOM}};
  ten = _RAND_2[3:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
