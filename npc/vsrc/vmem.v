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
	input [9:0]			h_addr,
	//rom
	output [7:0]		ascii_out,
	output [3:0]		row,
	output [3:0]		col
);
//回车键ascii码
parameter ENTER = 10;
//reg [23:0] vga_mem [524287:0];
reg [7:0] vga_mem [0:4095];
wire [9:0] tmp_row;
wire [9:0] tmp_col;

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
			vga_mem[i] = 8'd0;
		end
	end
	else if (p_valid) begin
		vga_mem[{x_ptr, y_ptr}] <= key_in;
		$display("key_in=%x", key_in);
	end
	else
		vga_mem[{x_ptr, y_ptr}] <= vga_mem[{x_ptr, y_ptr}];
end

assign ascii_out = vga_mem[{x, y}];
assign tmp_row = v_addr - ({5'd0,y} << 4);
assign tmp_col = h_addr - tmp;
assign row = tmp_row[3:0];
assign col = tmp_col[3:0];

wire [9:0] tmp = ({3'd0, x} << 3) + {3'd0,x};

/*
always @(h_addr) begin
	$display("h_addr = %d, tmp = %d, tmp_col = %d", h_addr, tmp, tmp_col);
end*/

endmodule
