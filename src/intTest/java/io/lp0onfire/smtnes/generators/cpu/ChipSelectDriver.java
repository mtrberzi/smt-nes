package io.lp0onfire.smtnes.generators.cpu;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.BinaryConstant;
import io.lp0onfire.smtnes.smt2.BitVectorDeclaration;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.Numeral;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChipSelectDriver implements CodeGenerator {

  private final String csName;
  private final BinaryConstant csValue;
  
  public ChipSelectDriver(String chipSelectPrefix, BinaryConstant chipSelectValue) {
    csName = chipSelectPrefix + "ChipSelect";
    csValue = chipSelectValue;
  }
  
  @Override
  public Set<String> getStateVariablesRead() {
    return new HashSet<>();
  }

  @Override
  public Set<String> getStateVariablesWritten() {
    return new HashSet<String>(Arrays.asList(new String[]{
        csName
    }));
  }

  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    Symbol CS = outputs.get(csName);
    exprs.add(new BitVectorDeclaration(CS, new Numeral("1")));
    exprs.add(new Assertion(new EqualsExpression(CS, csValue)));
    return exprs;
  }
  
}
