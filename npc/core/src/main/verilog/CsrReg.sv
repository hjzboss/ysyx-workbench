module CsrReg (
    input clock,
    input reset,

    // exception
    input exception,
    input [63:0] epc,
    input [3:0] no, 

    // read port
    input [2:0] raddr,
    output [63:0] rdata,

    // write port
    input [2:0] waddr,
    input wen,
    input [63:0] wdata
);

reg [63:0] csr [0:3];

import "DPI-C" function void set_csr_ptr(input logic [63:0] a []);

initial set_csr_ptr(csr);  // rf为通用寄存器的二维数组变量

initial csr[0] = 64'ha00001800; // 初始化mstatus

assign rdata = csr[raddr[1:0]];

always @(posedge clock) begin
  if (wen) csr[waddr[1:0]] <= wdata;
end

// todo
always @(posedge clock) begin
  if (exception) begin
    csr[2] <= epc;
    csr[3] <= {{60{1'b0}}, no};
  end
end
endmodule