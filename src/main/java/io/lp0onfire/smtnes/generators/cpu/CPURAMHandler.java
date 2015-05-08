package io.lp0onfire.smtnes.generators.cpu;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.smt2.*;

public class CPURAMHandler extends PageHandler {

  // If true, generates equality expressions involving array terms,
  // which cannot be handled by some solvers (such as STP).
  // If false, replaces these with equivalent expressions asserting that
  // each element in corresponding arrays is equal. This does not rely on
  // array extensionality, but it also causes an exponential blow-up of the
  // input formula, since 2^N terms must be generated to copy an
  // array that uses N address bits.
  private static boolean useArrayExtensionality = true;
  public static void setUseArrayExtensionality(boolean b) {
    useArrayExtensionality = b;
  }
  
  @Override
  public String getHandlerPrefix() {
    return "RAM_";
  }

  @Override
  public Set<String> getCustomStateVariablesRead() {
    return new HashSet<String>(Arrays.asList(new String[]{
        "CPU_RAM"
    }));
  }

  @Override
  public Set<String> getCustomStateVariablesWritten() {
    return new HashSet<String>(Arrays.asList(new String[]{
        "CPU_RAM"
    }));
  }
  
  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    // declare CPURAM_DataOut
    Symbol DataOut = outputs.get(getHandlerPrefix() + "DataOut");
    exprs.add(new BitVectorDeclaration(DataOut, new Numeral("8")));
    // declare CPU_RAM (next)
    Symbol RAM_next = outputs.get("CPU_RAM");
    exprs.add(new ArrayDeclaration(RAM_next, new Numeral("11"), new Numeral("8")));
    
    // get symbols that we will read:
    // ChipSelect, Address, WriteEnable, CPU DataOut (our DataIn), CPU_RAM (current)
    Symbol ChipSelect = inputs.get(getHandlerPrefix() + "ChipSelect");
    Symbol Address = inputs.get("CPU_AddressBus");
    Symbol WriteEnable = inputs.get("CPU_WriteEnable");
    Symbol DataIn = inputs.get("CPU_DataOut");
    Symbol RAM_current = inputs.get("CPU_RAM");
    
    // extract 11-bit address
    SExpression RAM_Address = new BitVectorExtractExpression(Address, new Numeral("10"), new Numeral("0"));
       
    // if ChipSelect = 0, don't do anything:
    // RAM_next <= RAM_current and DataOut <= 0x00
    if (useArrayExtensionality) {
      exprs.add(new Assertion(new Implication(new EqualsExpression(ChipSelect, new BinaryConstant("0")), 
          new EqualsExpression(RAM_next, RAM_current))));
    } else {
      for (int i = 0; i < 2048; ++i) {
        // convert i to an 11-bit constant
        String bits = Integer.toBinaryString(i);
        // zero-pad on the left
        int zeroCount = 11 - bits.length();
        BinaryConstant address = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
        exprs.add(new Assertion(new Implication(new EqualsExpression(ChipSelect, new BinaryConstant("0")), 
            new EqualsExpression(
                new ArrayReadExpression(RAM_next, address), 
                new ArrayReadExpression(RAM_current, address)))));
      }
    }
    exprs.add(new Assertion(new Implication(new EqualsExpression(ChipSelect, new BinaryConstant("0")), 
        new EqualsExpression(DataOut, new BinaryConstant("00000000")))));
    
    // if ChipSelect = 1 and WriteEnable = 0, this is a read:
    // RAM_next <= RAM_current and DataOut <= RAM_current[Address & 0x07FF]
    // (the lowest 11 bits of Address)
    
    if (useArrayExtensionality) {
      exprs.add(new Assertion(new Implication(
          new AndExpression(
              new EqualsExpression(ChipSelect, new BinaryConstant("1")),
              new EqualsExpression(WriteEnable, new BinaryConstant("0"))
              ), 
          new EqualsExpression(RAM_next, RAM_current))));
    } else {
      for (int i = 0; i < 2048; ++i) {
        // convert i to an 11-bit constant
        String bits = Integer.toBinaryString(i);
        // zero-pad on the left
        int zeroCount = 11 - bits.length();
        BinaryConstant address = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
        exprs.add(new Assertion(new Implication(new AndExpression(
            new EqualsExpression(ChipSelect, new BinaryConstant("1")),
            new EqualsExpression(WriteEnable, new BinaryConstant("0"))
            ), 
            new EqualsExpression(
                new ArrayReadExpression(RAM_next, address), 
                new ArrayReadExpression(RAM_current, address)))));
      }
    }
    exprs.add(new Assertion(new Implication(
        new AndExpression(
            new EqualsExpression(ChipSelect, new BinaryConstant("1")),
            new EqualsExpression(WriteEnable, new BinaryConstant("0"))
            ), 
        new EqualsExpression(DataOut, new ArrayReadExpression(RAM_current, RAM_Address)))));
    
    // if ChipSelect = 1 and WriteEnable = 1, this is a write:
    // RAM_next <= store(RAM_current, Address & 0x07FF, DataIn) and
    // (??? this is probably correct to simulate a tri-state shared data bus) DataOut <= DataIn
    
    if (useArrayExtensionality) {
      exprs.add(new Assertion(new Implication(
          new AndExpression(
              new EqualsExpression(ChipSelect, new BinaryConstant("1")),
              new EqualsExpression(WriteEnable, new BinaryConstant("1"))
              ), 
          new EqualsExpression(RAM_next, new ArrayWriteExpression(RAM_current, RAM_Address, DataIn)))));
    } else {
      for (int i = 0; i < 2048; ++i) {
        // convert i to an 11-bit constant
        String bits = Integer.toBinaryString(i);
        // zero-pad on the left
        int zeroCount = 11 - bits.length();
        BinaryConstant address = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
        exprs.add(new Assertion(new Implication(new AndExpression(
            new EqualsExpression(ChipSelect, new BinaryConstant("1")),
            new EqualsExpression(WriteEnable, new BinaryConstant("1"))
            ), 
            new EqualsExpression(
                new ArrayReadExpression(RAM_next, address), 
                new ArrayReadExpression(new ArrayWriteExpression(RAM_current, RAM_Address, DataIn), address)))));
      }
    }
    exprs.add(new Assertion(new Implication(
        new AndExpression(
            new EqualsExpression(ChipSelect, new BinaryConstant("1")),
            new EqualsExpression(WriteEnable, new BinaryConstant("1"))
            ), 
        new EqualsExpression(DataOut, DataIn))));
    
    return exprs;
  }

}
