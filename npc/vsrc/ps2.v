module ps2(
	input clk,
	input reset,
	input ps2_clk,
	input ps2_data,
	output [7:0] key_data,
	output reg valid
);

		localparam IDLE = 0, START = 1, END = 2;

    reg [9:0] buffer;        // ps2_data bits
    reg [3:0] count;  // count ps2_data bits
    reg [2:0] ps2_clk_sync;
		reg [1:0] state;// next_state;
		reg [7:0] current_data;

    always @(posedge clk) begin
        ps2_clk_sync <=  {ps2_clk_sync[1:0],ps2_clk};
    end
		
		//wire key_in = sampling & (count == 4'd10) & (buffer[0] == 0) & (ps2_data) & (^buffer[9:1]);	
    wire sampling = ps2_clk_sync[2] & ~ps2_clk_sync[1];
		
		assign key_data = current_data;
		
		/*
		always @(*) begin
				case (state)
					IDLE: next_state = key_in ? START : IDLE;
					START: next_state = (key_in && buffer[8:1] == 8'hf0) ? END : START;
					END: next_state = IDLE;
					default: next_state = IDLE;
				endcase
		end

		always @(posedge clk) begin
				if (reset)
						valid <= 0;
				else if (state == START && key_in)
						valid <= 1;
				else
						valid <= 0;
		end

		always @(posedge clk) begin
				if (reset)
						state <= IDLE;
				else
						state <= next_state;
		end*/

    always @(posedge clk) begin
        if (reset) begin // reset
            count <= 0;
						buffer <= 0;
						state <= IDLE;
						valid <= 0;
        end
        else begin
            if (sampling) begin
              if (count == 4'd10) begin
                if ((buffer[0] == 0) &&  // start bit
                    (ps2_data)       &&  // stop bit
                    (^buffer[9:1])) begin      // odd  parity
										case (state)
											IDLE: begin 
													state <= START;
													current_data <= buffer[8:1];
													$display("receive %x", buffer[8:1]);
													valid <= 1;
												end
											START: begin 
													if (buffer[8:1] == 8'hf0) begin
															state <= END;
															valid <= 0;
													end
													else begin
															state <= START;
															current_data <= buffer[8:1];
															$display("receive %x", buffer[8:1]);
															valid <= 1;
													end
												end
											END: begin
													state <= IDLE;
													valid <= 0;
												end
											default: begin 
													state <= IDLE;
													valid <= 0;
												end
										endcase
                end
								else begin
										valid <= 0;
								end
                count <= 0;     // for next
              end else begin
									buffer[count] <= ps2_data;  // store ps2_data
									count <= count + 3'b1;
									valid <= 0;
              end
            end
						else begin
								valid <= 0;
						end
        end
    end
		/*
		always @(valid) begin
				$display("%d", valid);
		end*/
endmodule
