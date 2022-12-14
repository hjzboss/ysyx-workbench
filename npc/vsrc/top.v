module top(
	input clock,
	input reset,
	input ps2_clk,
	input ps2_data,
	output VGA_CLK,
	output VGA_HSYNC,
	output VGA_VSYNC,
	output VGA_BLANK_N,
	output [7:0] VGA_R,
	output [7:0] VGA_G,
	output [7:0] VGA_B	
);

wire [23:0] vga_data;
wire [9:0] h_addr;
wire [9:0] v_addr;
wire [6:0] x;
wire [4:0] y;
wire p_valid;
wire [7:0] ascii_out;
wire [3:0] row;
wire [3:0] col;
wire rom_data;
wire [7:0] key_in;

vga u_vga (
	.pclk(clock),
	.reset(reset),
	.rom_data(rom_data),
	.h_addr(h_addr),
	.v_addr(v_addr),
	.x(x),
	.y(y),
	.hsync(VGA_HSYNC),
	.vsync(VGA_VSYNC),
	.valid(VGA_BLANK_N),
	.vga_r(VGA_R),
	.vga_g(VGA_G),
	.vga_b(VGA_B)
);

vmem u_vmem (
	.clk(clock),
	.reset(reset),
	.key_in(key_in),
	.p_valid(p_valid),
	.x(x),
	.y(y),
	.h_addr(h_addr),
	.v_addr(v_addr),
	.ascii_out(ascii_out),
	.row(row),
	.col(col)
);

rom u_rom(
	.ascii_in(8'd66),
	.row(row),
	.col(col),
	.data(rom_data)
);

ps2 u_ps2(
	.clk(clock),
	.reset(reset),
	.ps2_clk(ps2_clk),
	.ps2_data(ps2_data),
	.key_data(key_in),
	.valid(p_valid)
);
endmodule
