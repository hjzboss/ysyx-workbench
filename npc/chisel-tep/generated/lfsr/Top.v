module Top(
  input        clock,
  input        reset,
  output [7:0] io_led
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] shift; // @[Lfsr.scala 11:22]
  reg [31:0] cnt; // @[Lfsr.scala 12:20]
  wire  _cnt_T = cnt == 32'hf4240; // @[Lfsr.scala 14:18]
  wire [31:0] _cnt_T_2 = cnt + 32'h1; // @[Lfsr.scala 14:40]
  wire  highest = shift[4] ^ shift[3] ^ shift[2] ^ shift[0]; // @[Lfsr.scala 16:50]
  wire [7:0] _shift_T_1 = {highest,shift[7:1]}; // @[Lfsr.scala 17:22]
  assign io_led = shift; // @[Lfsr.scala 19:10]
  always @(posedge clock) begin
    if (reset) begin // @[Lfsr.scala 11:22]
      shift <= 8'h1; // @[Lfsr.scala 11:22]
    end else if (_cnt_T) begin // @[Lfsr.scala 15:26]
      shift <= _shift_T_1; // @[Lfsr.scala 17:11]
    end
    if (reset) begin // @[Lfsr.scala 12:20]
      cnt <= 32'h0; // @[Lfsr.scala 12:20]
    end else if (cnt == 32'hf4240) begin // @[Lfsr.scala 14:13]
      cnt <= 32'h0;
    end else begin
      cnt <= _cnt_T_2;
    end
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
  shift = _RAND_0[7:0];
  _RAND_1 = {1{`RANDOM}};
  cnt = _RAND_1[31:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
