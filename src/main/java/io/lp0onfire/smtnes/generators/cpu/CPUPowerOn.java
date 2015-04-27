package io.lp0onfire.smtnes.generators.cpu;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.smt2.*;

/**
 * Initialize state variables for the CPU as they would look at power-on. 
 * All CPU variables start with CPU_.
 * The CPU state variables are:
 * - RAM, an array of 2048 bytes (addr=11,data=8) initially all zeroes
 * - A, X, Y, SP: processor registers (width=8)
 * - PC: program counter (width=16)
 * - CalcAddr, TmpAddr: addressing mode calculation temporaries (width=16)
 * - BranchOffset: branch calculation temporary (width=8)
 * - AddressBus: external memory address (width=16)
 * - WriteEnable: external memory write enable: 1=write, 0=read (width=1)
 * - DataOut: external memory data leaving CPU (width=8)
 * - DataIn: external memory data entering CPU (width=8)
 * - ResetSequence: (width=3) when we are in the RESET state, counts down from 8 to 0
 *   to perform a series of reads and eventually load PC.
 * - State: (width=?) serializes CPUState
 */
public class CPUPowerOn implements CodeGenerator {
  
  @Override
  public Set<String> getStateVariablesRead() {
    return new HashSet<>();
  }

  @Override
  public Set<String> getStateVariablesWritten() {
    return new HashSet<>(Arrays.asList("CPU_RAM", 
        "CPU_A", "CPU_X", "CPU_Y", "CPU_PC", "CPU_SP", "CPU_P", 
        "CPU_CalcAddr", "CPU_TmpAddr", "CPU_BranchOffset",
        "CPU_AddressBus", "CPU_WriteEnable", "CPU_DataOut", "CPU_DataIn",
        "CPU_ResetSequence", "CPU_State"));
  }

  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    
    // declare RAM array and initialize to all zeroes
    Symbol RAM = outputs.get("CPU_RAM");
    exprs.add(new ArrayDeclaration(RAM, new Numeral("11"), new Numeral("8")));
    for (int i = 0; i < 2048; ++i) {
      String bits = Integer.toBinaryString(i);
      // zero-pad on the left
      int zeroCount = 11 - bits.length();
      BinaryConstant index = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
      exprs.add(new Assertion(new EqualsExpression(
          new ArrayReadExpression(RAM, index), new BinaryConstant("00000000"))));
    }
    
    // declare CPU registers and initialize to zero (except P, which gets 0x20)
    Symbol A = outputs.get("CPU_A");
    exprs.add(new BitVectorDeclaration(A, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(A, new BinaryConstant("00000000"))));
    
    Symbol X = outputs.get("CPU_X");
    exprs.add(new BitVectorDeclaration(X, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(X, new BinaryConstant("00000000"))));
    
    Symbol Y = outputs.get("CPU_Y");
    exprs.add(new BitVectorDeclaration(Y, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(Y, new BinaryConstant("00000000"))));
    
    Symbol SP = outputs.get("CPU_SP");
    exprs.add(new BitVectorDeclaration(SP, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(SP, new BinaryConstant("00000000"))));
    
    Symbol P = outputs.get("CPU_P");
    exprs.add(new BitVectorDeclaration(P, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(P, new BinaryConstant("00100000"))));
    
    Symbol PC = outputs.get("CPU_PC");
    exprs.add(new BitVectorDeclaration(PC, new Numeral("16")));
    exprs.add(new Assertion(new EqualsExpression(PC, new BinaryConstant("0000000000000000"))));
    
    // declare temporaries CalcAddr, TmpAddr, BranchOffset, set to 0
    
    Symbol CalcAddr = outputs.get("CPU_CalcAddr");
    exprs.add(new BitVectorDeclaration(CalcAddr, new Numeral("16")));
    exprs.add(new Assertion(new EqualsExpression(CalcAddr, new BinaryConstant("0000000000000000"))));
    
    Symbol TmpAddr = outputs.get("CPU_TmpAddr");
    exprs.add(new BitVectorDeclaration(TmpAddr, new Numeral("16")));
    exprs.add(new Assertion(new EqualsExpression(TmpAddr, new BinaryConstant("0000000000000000"))));
    
    Symbol BranchOffset = outputs.get("CPU_BranchOffset");
    exprs.add(new BitVectorDeclaration(BranchOffset, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(BranchOffset, new BinaryConstant("00000000"))));
    
    // declare memory bus variables AddressBus, WriteEnable, DataOut, DataIn and set to 0
    
    Symbol AddressBus = outputs.get("CPU_AddressBus");
    exprs.add(new BitVectorDeclaration(AddressBus, new Numeral("16")));
    exprs.add(new Assertion(new EqualsExpression(AddressBus, new BinaryConstant("0000000000000000"))));
    
    Symbol WriteEnable = outputs.get("CPU_WriteEnable");
    exprs.add(new BitVectorDeclaration(WriteEnable, new Numeral("1")));
    exprs.add(new Assertion(new EqualsExpression(WriteEnable, new BinaryConstant("0"))));
    
    Symbol DataOut = outputs.get("CPU_DataOut");
    exprs.add(new BitVectorDeclaration(DataOut, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(DataOut, new BinaryConstant("00000000"))));

    Symbol DataIn = outputs.get("CPU_DataIn");
    exprs.add(new BitVectorDeclaration(DataIn, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(DataIn, new BinaryConstant("00000000"))));
    
    // declare internal state variables
    Symbol ResetSequence = outputs.get("CPU_ResetSequence");
    exprs.add(new BitVectorDeclaration(ResetSequence, new Numeral("3")));
    exprs.add(new Assertion(new EqualsExpression(ResetSequence, new BinaryConstant("000"))));
    
    Symbol State = outputs.get("CPU_State");
    exprs.add(new BitVectorDeclaration(State, new Numeral(Integer.toString(CPUState.getStateWidth()))));
    exprs.add(new Assertion(new EqualsExpression(State, CPUState.Resetting.toBinaryConstant())));
    
    return exprs;
  }

}
