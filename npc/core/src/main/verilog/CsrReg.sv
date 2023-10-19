module CsrReg (
    input[63:0] mstatus,
    input[63:0] mtvec,
    input[63:0] mepc,
    input[63:0] mcause
);

wire [63:0] csr [0:3];

assign csr[0] = mstatus;
assign csr[1] = mtvec;
assign csr[2] = mepc;
assign csr[3] = mcause;

import "DPI-C" function void set_csr_ptr(input logic [63:0] a []);
initial set_csr_ptr(csr);  // rf为通用寄存器的二维数组变量

endmodule