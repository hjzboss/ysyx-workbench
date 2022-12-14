module ps2(
	input clk,
	input reset,
	input ps2_clk,
	input ps2_data,
	output reg [7:0] key_data,
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
		
		//assign key_data = current_data;
		
		always @(*) begin
			case (current_data)
				8'h15: key_data = 8'h71;
				8'h1d: key_data = 8'h77;
				8'h24: key_data = 8'h65;
				8'h2d: key_data = 8'h72;
				8'h2c: key_data = 8'h74;
				8'h35: key_data = 8'h79;
				8'h3c: key_data = 8'h75;
				8'h43: key_data = 8'h6c;
				8'h44: key_data = 8'h6f;
				8'h4d: key_data = 8'h70;
				8'h1c: key_data = 8'h61;
				8'h1b: key_data = 8'h73;
				8'h23: key_data = 8'h64;
				8'h2b: key_data = 8'h66;
				8'h34: key_data = 8'h67;
				8'h33: key_data = 8'h68;
				8'h3b: key_data = 8'h6a;
				8'h42: key_data = 8'h6b;
				8'h4b: key_data = 8'h6c;
				8'h1a: key_data = 8'h7a;
				8'h22: key_data = 8'h78;
				8'h21: key_data = 8'h63;
				8'h2a: key_data = 8'h76;
				8'h32: key_data = 8'h62;
				8'h31: key_data = 8'h6e;
				8'h3a: key_data = 8'h6d;
				8'h5a: key_data = 8'h0d;//换行键
				8'h66: key_data = 8'h08;
				default: key_data = 8'd0;
			endcase
		end

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
													$display("receive=%x, ascii=%x", buffer[8:1], key_data);
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
