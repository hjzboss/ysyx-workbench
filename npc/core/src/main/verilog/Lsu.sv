module Lsu (
  input [63:0]    raddr,
  output [63:0]   rdata,

  input           wvalid,
  input [63:0]    waddr,
  input [63:0]    wdata,
  input [7:0]     wmask
);

// 由于地址是以8对齐，因此对于要写入或者读入的数据要移位处理
reg[63:0] rdata_full;
assign rdata = rdata_full >> ({3'd0, raddr[2:0]} << 3); // align

reg[7:0] mask;
assign mask = wmask << waddr[2:0];

import "DPI-C" function void pmem_read(
  input longint raddr, output longint rdata);
import "DPI-C" function void pmem_write(
  input longint waddr, input longint wdata, input byte wmask);

always @(*) begin
  pmem_read(raddr, rdata_full);
  if (wvalid)
    pmem_write(waddr, wdata, mask);
end
endmodule