module rom(
	input [7:0]	ascii_in,
	input [3:0]	row,
	input [3:0] col,
	output			data
);

reg [11:0] mem [0:4095];
wire [11:0] row_addr;
wire [3:0] col_addr;

initial begin
	$readmemh("/home/hjz/ysyx-workbench/npc/resource/vga_font.txt", mem);
end

assign col_addr = 4'd12 - col;
assign row_addr = {4'd0, ascii_in} << 4 + {8'd0, row};
assign data = mem[row_addr][col_addr];
/*
always @(col_addr) begin
	$display("%d", col_addr);
end
*/
endmodule
