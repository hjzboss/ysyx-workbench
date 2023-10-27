module Pmem (
  input[63:0] raddr,
  output reg[63:0] rdata,
  input rvalid,

  input[63:0] waddr,
  input[63:0] wdata,
  input[7:0] mask
);

import "DPI-C" function void pmem_read(
  input longint raddr, output longint rdata);
import "DPI-C" function void pmem_write(
  input longint waddr, input longint wdata, input byte wmask);

always @(*) begin
  if (rvalid)
    pmem_read(raddr, rdata);
  else
    rdata = {2{$random}};
end

always @(*) begin
  pmem_write(waddr, wdata, mask);
end

endmodule
