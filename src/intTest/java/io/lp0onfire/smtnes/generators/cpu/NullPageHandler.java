package io.lp0onfire.smtnes.generators.cpu;

import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.smt2.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullPageHandler extends PageHandler {

  // Null page handler. Discards its inputs, always writes 0 to DataOut.
  
  @Override
  public String getHandlerPrefix() {
    return "NullPage_";
  }

  @Override
  public Set<String> getCustomStateVariablesRead() {
    return new HashSet<String>();
  }

  @Override
  public Set<String> getCustomStateVariablesWritten() {
    return new HashSet<String>();
  }

  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<SExpression>();
    
    // define DataOut
    Symbol DataOut = outputs.get(getHandlerPrefix() + "DataOut");
    exprs.add(new BitVectorDeclaration(DataOut, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(DataOut, new BinaryConstant("00000000"))));
    
    return exprs;
  }
  
}