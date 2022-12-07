module (
	input clk,
	input rst,
	output reg [15:0] led
);

reg [31:0] count;

always @(posedge clk) begin
	if (rst)
		count <= 0;
	else if (count == 5000000)
		count <= 0;
	else
		count <= count + 1;
end

always @(posedge clk) begin
	if (rst)
		led <= 1;
	else if (count == 5000000)
		led <= (led == 16'h8000) ? 16'd1 : led << 1;
	else
		led <= led;	
end
endmodule
