module top(
  input        clock,
  input  [2:0] io_addr,
  input        io_wen,
  input        io_ren,
  input  [7:0] io_wData,
  output [7:0] io_rData
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
  reg [7:0] mem [0:7]; // @[Memory.scala 17:16]
  wire [2:0] mem_io_rData_MPORT_addr; // @[Memory.scala 17:16]
  wire [7:0] mem_io_rData_MPORT_data; // @[Memory.scala 17:16]
  wire [7:0] mem_MPORT_data; // @[Memory.scala 17:16]
  wire [2:0] mem_MPORT_addr; // @[Memory.scala 17:16]
  wire  mem_MPORT_mask; // @[Memory.scala 17:16]
  wire  mem_MPORT_en; // @[Memory.scala 17:16]
  assign mem_io_rData_MPORT_addr = io_addr;
  assign mem_io_rData_MPORT_data = mem[mem_io_rData_MPORT_addr]; // @[Memory.scala 17:16]
  assign mem_MPORT_data = io_wData;
  assign mem_MPORT_addr = io_addr;
  assign mem_MPORT_mask = 1'h1;
  assign mem_MPORT_en = io_wen;
  assign io_rData = io_ren ? mem_io_rData_MPORT_data : 8'h0; // @[Memory.scala 21:{16,27} 22:25]
	initial begin
	 $readmemh("/home/hjz/ysyx-workbench/npc/chisel-tep/src/main/scala/memory/mem.hex", mem);
	end
  always @(posedge clock) begin
    if (mem_MPORT_en & mem_MPORT_mask) begin
      mem[mem_MPORT_addr] <= mem_MPORT_data; // @[Memory.scala 17:16]
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 8; initvar = initvar+1)
    mem[initvar] = _RAND_0[7:0];
`endif // RANDOMIZE_MEM_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
