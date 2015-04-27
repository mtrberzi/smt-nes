package io.lp0onfire.smtnes.generators.cpu;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.BinaryConstant;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VerifyPageHandlerDataOut implements CodeGenerator {
  private final String handlerPrefix;
  private final BinaryConstant expectedValue;
  
  public VerifyPageHandlerDataOut(String handlerPrefix, BinaryConstant expectedValue) {
    this.handlerPrefix = handlerPrefix;
    this.expectedValue = expectedValue;
  }
  
  @Override
  public Set<String> getStateVariablesRead() {
    return new HashSet<String>(Arrays.asList(new String[]{
        handlerPrefix + "DataOut"
    }));
  }

  @Override
  public Set<String> getStateVariablesWritten() {
    return new HashSet<>();
  }

  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    
    Symbol DataOut = inputs.get(handlerPrefix + "DataOut");
    exprs.add(new Assertion(new EqualsExpression(DataOut, expectedValue)));
    
    return exprs;
  }
}