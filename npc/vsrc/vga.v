module vga(
    input           pclk,     //25MHz时钟
    input           reset,    //置位
    //input  [23:0]   vga_data, //上层模块提供的VGA颜色数据
    input						rom_data, //rom提供的像素信息：黑或白
		output [9:0]    h_addr,   //提供给rom进行寻址的行列信息
    output [9:0]    v_addr,
		output [6:0]		x,				//显存的横坐标，用于寻址当前所在像素点所在字符的ascii码
		output [4:0]		y,				//显存的纵坐标
    output          hsync,    //行同步和列同步信号
    output          vsync,
    output          valid,    //消隐信号
    output [7:0]    vga_r,    //红绿蓝颜色信号
    output [7:0]    vga_g,
    output [7:0]    vga_b
);

  //640x480分辨率下的VGA参数设置
  parameter    h_frontporch = 96;
  parameter    h_active = 144;
  parameter    h_backporch = 784;
  parameter    h_total = 800;

  parameter    v_frontporch = 2;
  parameter    v_active = 35;
  parameter    v_backporch = 515;
  parameter    v_total = 525;

  //像素计数值
  reg [9:0]    x_cnt;
  reg [9:0]    y_cnt;
  wire         h_valid;
  wire         v_valid;
	reg [3:0]		 sum_x;
	reg [4:0]		 sum_y;	

	reg [6:0]		tmp_x;
	reg [4:0]		tmp_y;

	//wire [9:0]	h_addr;
	//wire [9:0]	v_addr;

  always @(posedge pclk) //行像素计数
			if (reset == 1'b1) begin
        x_cnt <= 1;
				sum_x <= 1;
			end
      else
      begin
				if (x_cnt == h_total) begin
            x_cnt <= 1;
						sum_x <= 4'd1;
				end
        else
            x_cnt <= x_cnt + 10'd1;
						sum_x <= ((sum_x == 9) || (x_cnt < 10'd145) || (x_cnt >= 10'd784)) ? 1 : (sum_x + 1);  
      end
	
  always @(posedge pclk)  //列像素计数
			if (reset == 1'b1) begin
        y_cnt <= 1;
				sum_y <= 1;
			end
      else
      begin
				if (y_cnt == v_total & x_cnt == h_total) begin
            y_cnt <= 1;
						sum_y <= 1;
				end
				else if (x_cnt == h_total) begin
            y_cnt <= y_cnt + 10'd1;
						sum_y <=((sum_y == 16) || (y_cnt <= 10'd35) || (y_cnt >= 10'd515)) ? 1 : (sum_y + 1);
				end
			end

	//x轴
	always @(posedge pclk) begin
			if (reset == 1'b1)
					tmp_x <= 0;	
			else if (x_cnt == h_total)
					tmp_x <= 0;
			else if (sum_x == 9)
					tmp_x <= tmp_x + 1; 
			else
					tmp_x <= tmp_x;
	end

	//y轴
	always @(posedge pclk) begin
			if (reset == 1'b1)
					tmp_y <= 0;
			else if (y_cnt == v_total && x_cnt == h_total)
					tmp_y <= 0;
			else if (sum_y == 16 && x_cnt == h_total)
					tmp_y <= tmp_y + 1;
			else
					tmp_y <= tmp_y;
	end
  //生成同步信号
  assign hsync = (x_cnt > h_frontporch);
  assign vsync = (y_cnt > v_frontporch);
  //生成消隐信号
  assign h_valid = (x_cnt > h_active) & (x_cnt <= h_backporch);
  assign v_valid = (y_cnt > v_active) & (y_cnt <= v_backporch);
  assign valid = h_valid & v_valid;
  //计算当前有效像素坐标
  assign h_addr = h_valid ? (x_cnt - 10'd145) : {10{1'b0}};
  assign v_addr = v_valid ? (y_cnt - 10'd36) : {10{1'b0}};

	assign x = h_valid ? tmp_x : 7'd0; 
	assign y = v_valid ? tmp_y : 5'd0;
  //设置输出的颜色值
  assign vga_r = rom_data ? 8'hff : 8'd0;
  assign vga_g = rom_data ? 8'hff : 8'd0;
  assign vga_b = rom_data ? 8'hff : 8'd0;
/*	
	always @(tmp_x) begin
		$display("%d", tmp_x);
	end*/
endmodule
