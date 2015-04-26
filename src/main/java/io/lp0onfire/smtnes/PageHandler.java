package io.lp0onfire.smtnes;

import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class PageHandler implements CodeGenerator {
  
  // Return a unique prefix ending with an underscore for variables belonging to this handler.
  public abstract String getHandlerPrefix();

  // Every page handler must respond to (prefix)ChipSelect and write (prefix)DataOut 
  // as well as CPU signals
  // Address, WriteEnable, DataOut
  
  @Override
  public Set<String> getStateVariablesRead() {
    Set<String> retval = new HashSet<>();
    retval.add(getHandlerPrefix() + "ChipSelect");
    retval.add("CPU_AddressBus");
    retval.add("CPU_WriteEnable");
    retval.add("CPU_DataOut");
    retval.addAll(getCustomStateVariablesRead());
    return retval;
  }

  public abstract Set<String> getCustomStateVariablesRead();
  
  @Override
  public Set<String> getStateVariablesWritten() {
    Set<String> retval = new HashSet<>();
    retval.add(getHandlerPrefix() + "DataOut");
    retval.addAll(getCustomStateVariablesWritten());
    return retval;
  }

  public abstract Set<String> getCustomStateVariablesWritten();
  
  public abstract List<SExpression> generateCode(Map<String, Symbol> inputs, Map<String, Symbol> outputs);
  
}
