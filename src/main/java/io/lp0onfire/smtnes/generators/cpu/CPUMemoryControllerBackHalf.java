package io.lp0onfire.smtnes.generators.cpu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.smt2.*;

// Contains code generators to control access to each page of memory attached to the CPU.
// This is the "back half" of the memory controller, which should be run
// after the front half and after all page handlers.
// It controls muxing DataOut from the activated page handler into the CPU's DataIn.

public class CPUMemoryControllerBackHalf implements CodeGenerator {

  private ArrayList<PageHandler> pageHandlers = new ArrayList<>();
  private Set<String> uniquePageHandlerPrefixes = new HashSet<>();
  
  public CPUMemoryControllerBackHalf(List<PageHandler> pageHandlers) {
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
    // we read chip-select and data-out from every page handler
    for (String prefix : uniquePageHandlerPrefixes) {
      uses.add(prefix + "ChipSelect");
      uses.add(prefix + "DataOut");
    }
    return uses;
  }

  @Override
  public Set<String> getStateVariablesWritten() {
    Set<String> defs = new HashSet<>();
    // we only define one thing
    defs.add("CPU_DataIn");
    
    return defs;
  }

  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    
    // define CPU DataIn
    Symbol DataIn = outputs.get("CPU_DataIn");
    exprs.add(new BitVectorDeclaration(DataIn, new Numeral("8")));
    
    // decode each handler individually
    for (String handlerPrefix : uniquePageHandlerPrefixes) {
      SExpression handlerChipSelect = inputs.get(handlerPrefix + "ChipSelect");
      SExpression handlerDataOut = inputs.get(handlerPrefix + "DataOut");
      exprs.add(new Assertion(new Implication(
          new EqualsExpression(handlerChipSelect, new BinaryConstant("1")), 
          new EqualsExpression(DataIn, handlerDataOut))));
    }
    
    return exprs;
  }

}
