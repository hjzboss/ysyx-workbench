module top(
	input [1:0] y,
	input [1:0] x0,
	input [1:0] x1,
	input [1:0] x2,
	input [1:0] x3,
	output reg [1:0] f
);

always @(*) begin
	case (y)
		2'b00: f = x0;
		2'b01: f = x1;
		2'b10: f = x2;
		2'b11: f = x3;
		default: f = 0;
	endcase
end
endmodule
