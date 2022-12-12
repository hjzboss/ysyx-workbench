module BindsTo_0_Memory(
  input        clock,
  input        reset,
  input  [2:0] io_addr,
  input        io_wen,
  input        io_ren,
  input  [7:0] io_wData,
  output [7:0] io_rData
);

  
initial begin
  $readmemh("/home/hjz/ysyx-workbench/npc/chisel-tep/src/main/scala/memory/mem.hex", top.mem);
end
                      endmodule

bind Memory BindsTo_0_Memory BindsTo_0_Memory_Inst(.*);
