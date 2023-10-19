module SimGRF (
    input clock,
    input reset,

    // read port
    input [4:0] rs1,
    input [4:0] rs2,
    output [63:0] src1,
    output [63:0] src2,

    // write port
    input [4:0] waddr,
    input wen,
    input [63:0] wdata
);

reg [63:0] rf [0:31];

import "DPI-C" function void set_gpr_ptr(input logic [63:0] a []);

initial set_gpr_ptr(rf);  // rf为通用寄存器的二维数组变量

// forward
assign src1 = wen && waddr == rs1 ? wdata : rf[rs1];
assign src2 = wen && waddr == rs2 ? wdata : rf[rs2];

always @(posedge clock) begin
  if (wen && waddr != 5'd0) rf[waddr] <= wdata;
end

endmodule