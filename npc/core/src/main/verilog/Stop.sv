module Stop (
  input valid,
  input[63:0] haltRet
);

import "DPI-C" function void c_break(input longint halt_ret);

always @(*) begin
  if (valid) begin
    c_break(haltRet);
  end
end

endmodule