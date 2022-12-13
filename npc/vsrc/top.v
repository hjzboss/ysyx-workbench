module top(
	input clock,
	input reset,
	output VGA_CLK,
	output VGA_HSYNC,
	output VGA_VSYNC,
	output VGA_BLANK_N,
	output [7:0] VGA_R,
	output [7:0] VGA_G,
	output [7:0] VGA_B	
);

//wire [23:0] vga_data;
wire [9:0] h_addr;
wire [9:0] v_addr;

vga u_vga (
	.pclk(clock),
	.reset(reset),
	.vga_data(24'hFF0000),
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
