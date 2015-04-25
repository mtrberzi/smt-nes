package io.lp0onfire.smtnes.generators.cpu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.smt2.*;

// Contains code generators to control access to each page of memory attached to the CPU.
// This is only the "front half" of the memory controller, which forwards signals
// from the CPU to the bus devices; the "back half" is implemented elsewhere and should
// be run after this and all page handlers in order to forward the correct
// data-in signal to the CPU.

public class CPUMemoryControllerFrontHalf implements CodeGenerator {

  private List<PageHandler> pageHandlers = new ArrayList<>();
  private Set<String> uniquePageHandlerPrefixes = new HashSet<>();
  
  public CPUMemoryControllerFrontHalf(List<PageHandler> pageHandlers) {
    if (pageHandlers.size() != 16) {
      throw new IllegalArgumentException("must provide exactly 16 page handlers");
    }
    for (int i = 0; i < pageHandlers.size(); ++i) {
      this.pageHandlers.add(pageHandlers.get(i));
      uniquePageHandlerPrefixes.add(pageHandlers.get(i).getHandlerPrefix());
    }
  }

  @Override
  public Set<String> getStateVariablesRead() {
    Set<String> uses = new HashSet<>();
    // we need to read CPU address/write-enable/data-out
    uses.add("CPU_AddressBus");
    uses.add("CPU_WriteEnable");
    uses.add("CPU_DataOut");
    
    return uses;
  }

  @Override
  public Set<String> getStateVariablesWritten() {
    Set<String> defs = new HashSet<>();
    // we write chip-select, address, write-enable, and data-in
    // for every page handler
    for (String prefix : uniquePageHandlerPrefixes) {
      defs.add(prefix + "ChipSelect");
      defs.add(prefix + "Address");
      defs.add(prefix + "WriteEnable");
      defs.add(prefix + "DataIn");
    }
    return defs;
  }

  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    
    List<Symbol> chipSelectLines = new LinkedList<>();
    
    // create definitions of outputs
    for (String prefix : uniquePageHandlerPrefixes) {
      exprs.add(new BitVectorDeclaration(outputs.get(prefix + "ChipSelect"), new Numeral("1")));
      chipSelectLines.add(outputs.get(prefix + "ChipSelect"));
      exprs.add(new BitVectorDeclaration(outputs.get(prefix + "Address"), new Numeral("16")));
      exprs.add(new BitVectorDeclaration(outputs.get(prefix + "WriteEnable"), new Numeral("1")));
      exprs.add(new BitVectorDeclaration(outputs.get(prefix + "DataIn"), new Numeral("8")));
    }
       
    for (int i = 0; i < 16; ++i) {
      // convert i to a 4-bit constant
      String bits = Integer.toBinaryString(i);
      // zero-pad on the left
      int zeroCount = 4 - bits.length();
      BinaryConstant pageNumber = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
      // read the top 4 bits of the address
      SExpression addressPage = new BitVectorExtractExpression(inputs.get("CPU_AddressBus"), new Numeral("15"), new Numeral("12"));
      
      // generate the chip-select logic:
      // if the upper four bits of the address map to page N, set the chip select line for handler N to 1
      // and all other chip select lines to 0
      
      String assertedChipSelect = pageHandlers.get(i).getHandlerPrefix() + "ChipSelect";
      for (Symbol chipSelect : chipSelectLines) {
        if (chipSelect.getName().equals(assertedChipSelect)) {
          exprs.add(new Assertion(new ImpliesExpression(new EqualsExpression(addressPage, pageNumber), 
              new EqualsExpression(chipSelect, new BinaryConstant("1")))));
        } else {
          exprs.add(new Assertion(new ImpliesExpression(new EqualsExpression(addressPage, pageNumber), 
              new EqualsExpression(chipSelect, new BinaryConstant("1")))));
        }
      }
      
      // TODO address/WE/data forwarding
    }
    
    return exprs;
  }
  
}
