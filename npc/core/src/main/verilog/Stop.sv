module Stop (
  input valid,
  input[63:0] haltRet,
  input[63:0] pc
);

import "DPI-C" function void c_break(input longint halt_ret, input longint pc);

always @(*) begin
  if (valid) begin
    c_break(haltRet, pc);
  end
end

endmodule