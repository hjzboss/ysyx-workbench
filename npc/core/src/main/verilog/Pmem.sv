module Pmem (
  input[63:0] raddr,
  output reg[63:0] rdata,
  input rvalid,

  input[63:0] waddr,
  input[63:0] wdata,
  input[7:0] mask,

  output visit_advice // 是否访问了设备
);

`define CONFIG_TIMER_MMIO 0xa0000048
`define CONFIG_SERIAL_MMIO 0xa00003f8
`define CONFIG_FB_ADDR 0xa1000000
`define CONFIG_VGA_CTL_MMIO 0xa0000100
`define SYNC_ADDR (CONFIG_VGA_CTL_MMIO + 8)

#define CONFIG_I8042_DATA_MMIO 0xa0000080

import "DPI-C" function void pmem_read(
  input longint raddr, output longint rdata);
import "DPI-C" function void pmem_write(
  input longint waddr, input longint wdata, input byte wmask);

//assign visit_advice = (rvalid || mask != 8'd0) && ((raddr == CONFIG_FB_ADDR || raddr == CONFIG_TIMER_MMIO || raddr == CONFIG_SERIAL_MMIO || raddr == CONFIG_VGA_CTL_MMIO || raddr == SYNC_ADDR)
//                      || (raddr == CONFIG_FB_ADDR || raddr == CONFIG_TIMER_MMIO || raddr == CONFIG_SERIAL_MMIO || raddr == CONFIG_VGA_CTL_MMIO || raddr == SYNC_ADDR))

assign visit_advice = (rvalid && (raddr == `CONFIG_FB_ADDR || raddr == `CONFIG_TIMER_MMIO || raddr == `CONFIG_SERIAL_MMIO || raddr == `CONFIG_VGA_CTL_MMIO || raddr == `SYNC_ADDR))
                      || (mask != 8'd0 && (waddr == `CONFIG_FB_ADDR || waddr == `CONFIG_TIMER_MMIO || waddr == `CONFIG_SERIAL_MMIO || waddr == `CONFIG_VGA_CTL_MMIO || waddr == `SYNC_ADDR))

always @(*) begin
  if (rvalid)
    pmem_read(raddr, rdata);
  else
    rdata = 64'd0;
end

always @(*) begin
  pmem_write(waddr, wdata, mask);
end

endmodule
