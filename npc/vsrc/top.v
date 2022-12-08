module top (
	input					en,
	input [7:0]		sw,
	
	output				valid,
	output [7:0]	led,

	output [6:0]	seg0
);

localparam ZERO = 7'b0000001, ONE = 7'b1001111, TWO = 7'b0010010, THREE = 7'b0000110, FOUR = 7'b1001100;
localparam FIVE = 7'b0100100, SIX = 7'b0100000, SEVEN = 7'b0001111;

reg [6:0] tmp;

assign led = en ? sw : 0;
assign seg0 = en ? tmp : ZERO;
assign valid = (sw != 8'd0) & en;

always @(*) begin	
	casez (sw)
		8'b00000000:	tmp = ZERO;
		8'b00000001:	tmp = ZERO;
		8'b0000001?:	tmp = ONE;
		8'b000001??:	tmp = TWO;
		8'b00001???:	tmp = THREE;
		8'b0001????:	tmp = FOUR;
		8'b001?????:	tmp = FIVE;
		8'b01??????:	tmp = SIX;
		8'b1???????:	tmp = SEVEN;	
		default:			tmp = ZERO;
	endcase
end
endmodule
