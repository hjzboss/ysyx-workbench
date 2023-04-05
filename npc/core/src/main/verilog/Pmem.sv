module Pmem (
  input[63:0] raddr,
  output reg[63:0] rdata,
  input rvalid
);

import "DPI-C" function void pmem_read(
  input longint raddr, output longint rdata);

always @(*) begin
  if (rvalid)
    pmem_read(raddr, rdata);
  else
    rdata = 64'd0;
end

endmodule
