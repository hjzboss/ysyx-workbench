module top(
	input [1:0] y,
	input [3:0] x,

	output f
);

assign f = x[y];

/*
always @(*) begin
	case (y)
		2'b00: f = x[0];
		2'b01: f = x[1];
		2'b10: f = x[2];
		2'b11: f = x[3];
	endcase
end
*/
endmodule
