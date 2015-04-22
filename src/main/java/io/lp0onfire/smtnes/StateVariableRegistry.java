package io.lp0onfire.smtnes;

import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StateVariableRegistry {

  private Map<String, Long> stateIndex = new HashMap<>();
  
  public List<SExpression> apply(CodeGenerator gen) {
    Set<String> variablesRead = gen.getStateVariablesRead();
    Map<String, Symbol> inputs = new HashMap<>();
    for (String var : variablesRead) {
      // if the variable doesn't exist yet, throw an exception
      if (!stateIndex.containsKey(var)) {
        throw new java.lang.IllegalStateException("attempt to use state variable '" + var + "' before it is defined");
      }
      // otherwise, get the index of the latest version
      Long idx = stateIndex.get(var);
      inputs.put(var, new Symbol(var + "_" + idx.toString()));
    }
    Set<String> variablesWritten = gen.getStateVariablesWritten();
    Map<String, Symbol> outputs = new HashMap<>();
    for (String var : variablesWritten) {
      Long idx;
      if (stateIndex.containsKey(var)) {
        // if the variable already exists, increment its index and use the new version
        idx = stateIndex.get(var) + 1L;
        stateIndex.put(var, idx);
      } else {
        // otherwise, use a version with index 0
        idx = 0L;
        stateIndex.put(var, 0L);
      }
      outputs.put(var, new Symbol(var + "_" + idx.toString()));
    }
    // run the generator
    return gen.generateCode(inputs, outputs);
  }
  
}
