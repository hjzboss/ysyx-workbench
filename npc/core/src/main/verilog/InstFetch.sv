module InstFetch (
  input [63:0]    pc,
  output [31:0]   inst
);

reg[63:0] inst_tmp;

import "DPI-C" function void inst_read(
  input longint raddr, output int rdata);

always @(*) begin
  inst_read(pc, inst);
  //$display("v: pc=%x, inst=%x", pc, inst);
end
endmodule