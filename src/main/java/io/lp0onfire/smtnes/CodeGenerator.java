package io.lp0onfire.smtnes;

import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CodeGenerator {

  // Returns a list of all symbol names whose values are used by generated code.
  // When the code generator is invoked, the latest stateful instances of these symbols
  // will be provided as arguments.
  Set<String> getStateVariablesRead();
  
  // Returns a list of all symbol names whose values are defined by generated code.
  // When the code generator is invoked, fresh stateful instances of these symbols
  // will be provided as arguments.
  Set<String> getStateVariablesWritten();
  
  List<SExpression> generateCode(Map<String, Symbol> inputs, Map<String, Symbol> outputs);
  
}
