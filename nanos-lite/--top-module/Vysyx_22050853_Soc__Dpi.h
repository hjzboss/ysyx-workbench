// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Prototypes for DPI import and export functions.
//
// Verilator includes this file in all generated .cpp files that use DPI functions.
// Manually include this file where DPI .c import functions are declared to ensure
// the C functions match the expectations of the DPI imports.

#ifndef VERILATED_VYSYX_22050853_SOC__DPI_H_
#define VERILATED_VYSYX_22050853_SOC__DPI_H_  // guard

#include "svdpi.h"

#ifdef __cplusplus
extern "C" {
#endif


    // DPI IMPORTS
    // DPI import at /home/hjz/ysyx-workbench/npc/core/src/main/verilog/Stop.sv:7:30
    extern void c_break(long long halt_ret, long long pc);
    // DPI import at /home/hjz/ysyx-workbench/npc/core/src/main/verilog/IMEM.sv:6:30
    extern void imem_read(int raddr, int* rdata);
    // DPI import at /home/hjz/ysyx-workbench/npc/core/src/main/verilog/Pmem.sv:11:30
    extern void pmem_read(long long raddr, long long* rdata);
    // DPI import at /home/hjz/ysyx-workbench/npc/core/src/main/verilog/Pmem.sv:13:30
    extern void pmem_write(long long waddr, long long wdata, char wmask);
    // DPI import at /home/hjz/ysyx-workbench/npc/core/src/main/verilog/CsrReg.sv:15:30
    extern void set_csr_ptr(const svOpenArrayHandle a);
    // DPI import at /home/hjz/ysyx-workbench/npc/core/src/main/verilog/SimGRF.sv:19:30
    extern void set_gpr_ptr(const svOpenArrayHandle a);

#ifdef __cplusplus
}
#endif

#endif  // guard
