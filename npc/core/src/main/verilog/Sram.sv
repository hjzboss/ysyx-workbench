/*
module Sram (
  input clock,
  input reset,

  // read addr 
  input arvalid,
  input[31:0] araddr,
  output arready,

  // read data
  input rready,
  output[63:0] rdata,
  output[1:0] rresp,
  output reg rvalid

  // write addr
  input[31:0] awaddr,
  input awvalid,
  output awready,

  // write data
  input[63:0] wdata,
  input[7:0] wstrb,
  input wvalid,
  output wready,

  // write response
  output[1:0] bresp,
  output bvalid,
  input bready
);

import "DPI-C" function void pmem_read(
  input longint raddr, output longint rdata);

assign arready = 1'b1; // todo

always @(posedge clock) begin
  if (reset) begin
    rvalid <= 1'b0;
  end
  else if (arready && arvalid) begin
    rvalid <= 1'b1;
  end
  else begin
    rvalid <= 1'b0;
  end
end

always @(*) begin
  if (rvalid && rready)
    pmem_read(araddr, rdata);
  else
    rdata = 64'd0;
end
endmodule
*/