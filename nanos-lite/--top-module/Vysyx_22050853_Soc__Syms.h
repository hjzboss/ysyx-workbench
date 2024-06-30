// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Symbol table internal header
//
// Internal details; most calling programs do not need this header,
// unless using verilator public meta comments.

#ifndef VERILATED_VYSYX_22050853_SOC__SYMS_H_
#define VERILATED_VYSYX_22050853_SOC__SYMS_H_  // guard

#include "verilated.h"

// INCLUDE MODEL CLASS

#include "Vysyx_22050853_Soc.h"

// INCLUDE MODULE CLASSES
#include "Vysyx_22050853_Soc___024root.h"

// DPI TYPES for DPI Export callbacks (Internal use)

// SYMS CLASS (contains all model state)
class alignas(VL_CACHE_LINE_BYTES)Vysyx_22050853_Soc__Syms final : public VerilatedSyms {
  public:
    // INTERNAL STATE
    Vysyx_22050853_Soc* const __Vm_modelp;
    bool __Vm_activity = false;  ///< Used by trace routines to determine change occurred
    uint32_t __Vm_baseCode = 0;  ///< Used by trace routines when tracing multiple models
    VlDeleter __Vm_deleter;
    bool __Vm_didInit = false;

    // MODULE INSTANCE STATE
    Vysyx_22050853_Soc___024root   TOP;

    // CONSTRUCTORS
    Vysyx_22050853_Soc__Syms(VerilatedContext* contextp, const char* namep, Vysyx_22050853_Soc* modelp);
    ~Vysyx_22050853_Soc__Syms();

    // METHODS
    const char* name() { return TOP.name(); }
};

#endif  // guard
