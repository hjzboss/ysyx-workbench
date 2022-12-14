module vmem (
	input						clk,
	input						reset,
	//ps2
	input	[7:0]			key_in,
	input						p_valid,
	//vga
	input [6:0]			x,
	input [4:0]			y,
	input [9:0]			v_addr,
	//rom
	output [7:0]		ascii_out,
	output [3:0]		row
);
//回车键ascii码
parameter ENTER = 10;
//reg [23:0] vga_mem [524287:0];
reg [7:0] vga_mem [0:4095];

//输入字符的行指针和列指针
reg [6:0] x_ptr;
reg [4:0] y_ptr;

integer i;

always @(posedge clk) begin
	if (reset) begin
		x_ptr <= 7'd0;
		y_ptr <= 5'd0;
	end
	else if (p_valid) begin
		// 当指针指向行末或者输入为换行符时
		if (x_ptr == 7'd69 || key_in == ENTER) begin
			x_ptr <= 7'd0;
			y_ptr <= y_ptr + 1;
		end
		else begin
			x_ptr <= x_ptr + 1;
			y_ptr <= y_ptr;
		end
	end
	else begin
		x_ptr <= x_ptr;
		y_ptr <= y_ptr;
	end
end

//向显存写入键盘输入数据
always @(posedge clk) begin
	if (reset) begin
		for (i=0; i<4096; i=i+1) begin
			vga_mem[i] <= 8'd0;
		end
	end
	else if (p_valid)
		vga_mem[{x_ptr, y_ptr}] <= key_in;
	else
		vga_mem[{x_ptr, y_ptr}] <= vga_mem[{x_ptr, y_ptr}];
end

assign ascii_out = vga_mem[{x, y}];
assign row = v_addr - ({5'd0,y} << 4);
endmodule
