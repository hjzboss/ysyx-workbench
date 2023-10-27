module DpiGrf (
    input [63:0] reg0 ,
    input [63:0] reg1 ,
    input [63:0] reg2 ,
    input [63:0] reg3 ,
    input [63:0] reg4 ,
    input [63:0] reg5 ,
    input [63:0] reg6 ,
    input [63:0] reg7 ,
    input [63:0] reg8 ,
    input [63:0] reg9 ,
    input [63:0] reg10,
    input [63:0] reg11,
    input [63:0] reg12,
    input [63:0] reg13,
    input [63:0] reg14,
    input [63:0] reg15,
    input [63:0] reg16,
    input [63:0] reg17,
    input [63:0] reg18,
    input [63:0] reg19,
    input [63:0] reg20,
    input [63:0] reg21,
    input [63:0] reg22,
    input [63:0] reg23,
    input [63:0] reg24,
    input [63:0] reg25,
    input [63:0] reg26,
    input [63:0] reg27,
    input [63:0] reg28,
    input [63:0] reg29,
    input [63:0] reg30,
    input [63:0] reg31
);

wire [63:0] rf [0:31];

assign rf[0]    = reg0 ;
assign rf[1]    = reg1 ;
assign rf[2]    = reg2 ;
assign rf[3]    = reg3 ;
assign rf[4]    = reg4 ;
assign rf[5]    = reg5 ;
assign rf[6]    = reg6 ;
assign rf[7]    = reg7 ;
assign rf[8]    = reg8 ;
assign rf[9]    = reg9 ;
assign rf[10]   = reg10;
assign rf[11]   = reg11;
assign rf[12]   = reg12;
assign rf[13]   = reg13;
assign rf[14]   = reg14;
assign rf[15]   = reg15;
assign rf[16]   = reg16;
assign rf[17]   = reg17;
assign rf[18]   = reg18;
assign rf[19]   = reg19;
assign rf[20]   = reg20;
assign rf[21]   = reg21;
assign rf[22]   = reg22;
assign rf[23]   = reg23;
assign rf[24]   = reg24;
assign rf[25]   = reg25;
assign rf[26]   = reg26;
assign rf[27]   = reg27;
assign rf[28]   = reg28;
assign rf[29]   = reg29;
assign rf[30]   = reg30;
assign rf[31]   = reg31;

import "DPI-C" function void set_gpr_ptr(input logic [63:0] a []);

initial set_gpr_ptr(rf);  // rf为通用寄存器的二维数组变量
endmodule