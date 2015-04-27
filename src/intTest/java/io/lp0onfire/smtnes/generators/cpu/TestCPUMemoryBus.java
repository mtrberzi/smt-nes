package io.lp0onfire.smtnes.generators.cpu;

import static org.junit.Assert.assertTrue;
import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.Z3;
import io.lp0onfire.smtnes.smt2.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

// Integration test for all of the following:
// CPU memory controller front half and back half,
// CPU RAM handler

public class TestCPUMemoryBus {

  private static BinaryConstant ramInitialValue = new BinaryConstant("10100101");
  
  class InitCPURAM implements CodeGenerator {

    @Override
    public Set<String> getStateVariablesRead() {
      return new HashSet<>();
    }

    @Override
    public Set<String> getStateVariablesWritten() {
      return new HashSet<String>(Arrays.asList(new String[]{
          "CPU_RAM"
      }));
    }

    @Override
    public List<SExpression> generateCode(Map<String, Symbol> inputs,
        Map<String, Symbol> outputs) {
      List<SExpression> exprs = new LinkedList<>();
      // declare RAM array and initialize to a test pattern
      Symbol RAM = outputs.get("CPU_RAM");
      exprs.add(new ArrayDeclaration(RAM, new Numeral("11"), new Numeral("8")));
      for (int i = 0; i < 2048; ++i) {
        String bits = Integer.toBinaryString(i);
        // zero-pad on the left
        int zeroCount = 11 - bits.length();
        BinaryConstant index = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
        exprs.add(new Assertion(new EqualsExpression(
            new ArrayReadExpression(RAM, index), ramInitialValue)));
      }
      return exprs;
    }
    
  }
  
  class VerifyCPUDataIn implements CodeGenerator {
    private final BinaryConstant expectedValue;
    
    public VerifyCPUDataIn(BinaryConstant expectedValue) {
      this.expectedValue = expectedValue;
    }
    
    @Override
    public Set<String> getStateVariablesRead() {
      return new HashSet<String>(Arrays.asList(new String[]{
          "CPU_DataIn"
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
      
      Symbol DataIn = inputs.get("CPU_DataIn");
      exprs.add(new Assertion(new EqualsExpression(DataIn, expectedValue)));
      
      return exprs;
    }
  }
  
  @Test(timeout=60 * 1000)
  public void testMemoryBus_Read() throws IOException {
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ArrayList<PageHandler> pageHandlers = new ArrayList<>();
    PageHandler ramPageHandler = new CPURAMHandler();
    PageHandler nullPageHandler = new NullPageHandler();
    for (int i = 0; i < 2; ++i) {
      pageHandlers.add(i, ramPageHandler);
    }
    for (int i = 2; i < 16; ++i) {
      pageHandlers.add(i, nullPageHandler);
    }
    
    CodeGenerator initRAM = new InitCPURAM();
    CodeGenerator busDriver = new BusDriver(
        new BinaryConstant("0000" + "000000001111"), new BinaryConstant("0"), new BinaryConstant("11111111"));
    CodeGenerator memoryControllerFront = new CPUMemoryControllerFrontHalf(pageHandlers);
    CodeGenerator memoryControllerBack = new CPUMemoryControllerBackHalf(pageHandlers);
    CodeGenerator verifyDataIn = new VerifyCPUDataIn(ramInitialValue);
    
    exprs.addAll(reg.apply(initRAM));
    exprs.addAll(reg.apply(busDriver));
    exprs.addAll(reg.apply(memoryControllerFront));
    exprs.addAll(reg.apply(nullPageHandler));
    exprs.addAll(reg.apply(ramPageHandler));
    exprs.addAll(reg.apply(memoryControllerBack));
    exprs.addAll(reg.apply(verifyDataIn));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
  }
  
}
