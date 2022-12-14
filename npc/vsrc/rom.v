module rom(
	input [7:0]	ascii_in,
	input [9:0]	h_addr,
	input [3:0]	row,
	output			data
);

reg [11:0] mem [0:4095];
wire [11:0] addr;

initial begin
	$readmemh("./resource/vga_font.txt", mem);
end

assign addr = ascii_in << 4 + row;
assign data = mem[addr][h_addr];
endmodule
