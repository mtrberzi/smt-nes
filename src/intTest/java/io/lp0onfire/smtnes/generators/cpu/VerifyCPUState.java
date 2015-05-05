package io.lp0onfire.smtnes.generators.cpu;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VerifyCPUState implements CodeGenerator {

  private final CPUState expectedState;
  
  public VerifyCPUState(CPUState expected) {
    this.expectedState = expected;
  }
  
  @Override
  public Set<String> getStateVariablesRead() {
    return new HashSet<String>(Arrays.asList(new String[]{
        "CPU_State"
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
    
    exprs.add(new Assertion(new EqualsExpression(inputs.get("CPU_State"), expectedState.toBinaryConstant())));
    
    return exprs;
  }
  
}
