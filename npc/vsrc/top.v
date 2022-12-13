module top(
	input clk,
	input rst,
	output VGA_CLK,
	output VGA_HSYNC，
	output VGA_VSYNC,
	output VGA_BLANK_N,
	output [7:0] VGA_R,
	output [7:0] VGA_G,
	output [7:0] VGA_B	
);

vga u_vga (
	.pclk(clk),
	.reset(rst),
	.vga_data(vga_data),
	.h_addr(h_addr),
	.v_addr(v_addr),
	.hsync(VGA_HSYNC),
	.vsync(VGA_VSYNC),
	.valid(VGA_BLANK_N),
	.vga_r(VGA_R),
	.vga_g(VGA_G),
	.vga_b(VGA_B)
);

vmem u_vmem (
	.h_addr(h_addr),
	.v_addr(v_addr[8:0]),
	.vga_data(vga_data)
);
endmodule
