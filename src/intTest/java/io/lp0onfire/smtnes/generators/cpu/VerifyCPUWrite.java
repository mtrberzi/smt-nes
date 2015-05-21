package io.lp0onfire.smtnes.generators.cpu;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.smt2.*;

// Fast way of looking for a bus write without the overhead of RAM.
public class VerifyCPUWrite implements CodeGenerator {

  private final SExpression expectedAddress;
  private final SExpression expectedData;
  
  public VerifyCPUWrite(SExpression addr, SExpression data) {
    this.expectedAddress = addr;
    this.expectedData = data;
  }
  
  @Override
  public Set<String> getStateVariablesRead() {
    return new HashSet<String>(Arrays.asList(new String[]{
        "CPU_AddressBus", "CPU_WriteEnable", "CPU_DataOut"
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
    Symbol Address = inputs.get("CPU_AddressBus");
    Symbol WriteEnable = inputs.get("CPU_WriteEnable");
    Symbol Data = inputs.get("CPU_DataOut");
    exprs.add(new AndExpression(
            new EqualsExpression(WriteEnable, new BinaryConstant("1")), 
            new EqualsExpression(Address, expectedAddress),
            new EqualsExpression(Data, expectedData)
            ));
    return exprs;
  }

}
