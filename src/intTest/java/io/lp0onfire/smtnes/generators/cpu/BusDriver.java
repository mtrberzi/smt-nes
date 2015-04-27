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

public class BusDriver implements CodeGenerator {

  private final BinaryConstant driverAddress;
  private final BinaryConstant driverWriteEnable;
  private final BinaryConstant driverDataOut;
  
  public BusDriver(BinaryConstant address, BinaryConstant writeEnable, BinaryConstant dataOut) {
    this.driverAddress = address;
    this.driverWriteEnable = writeEnable;
    this.driverDataOut = dataOut;
  }
  
  @Override
  public Set<String> getStateVariablesRead() {
    return new HashSet<>();
  }

  @Override
  public Set<String> getStateVariablesWritten() {
    return new HashSet<>(Arrays.asList("CPU_AddressBus", "CPU_WriteEnable", "CPU_DataOut"));
  }

  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    
    Symbol AddressBus = outputs.get("CPU_AddressBus");
    exprs.add(new BitVectorDeclaration(AddressBus, new Numeral("16")));
    exprs.add(new Assertion(new EqualsExpression(AddressBus, driverAddress)));
    
    Symbol WriteEnable = outputs.get("CPU_WriteEnable");
    exprs.add(new BitVectorDeclaration(WriteEnable, new Numeral("1")));
    exprs.add(new Assertion(new EqualsExpression(WriteEnable, driverWriteEnable)));
    
    Symbol DataOut = outputs.get("CPU_DataOut");
    exprs.add(new BitVectorDeclaration(DataOut, new Numeral("8")));
    exprs.add(new Assertion(new EqualsExpression(DataOut, driverDataOut)));
    
    return exprs;
  }
  
}
