module IMEM (
  input[31:0] pc,
  output reg[31:0] inst,
);

import "DPI-C" function void imem_read(
  input int raddr, output int rdata);

always @(*) begin
  imem_read(pc, inst);
end

endmodule
