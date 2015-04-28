package io.lp0onfire.smtnes.generators.cpu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.Z3;
import io.lp0onfire.smtnes.smt2.*;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestCPUResetSequence {

  class VerifyResetSequence implements CodeGenerator {

    private final BinaryConstant expectedResetSequence;
    
    public VerifyResetSequence(BinaryConstant expected) {
      this.expectedResetSequence = expected;
    }
    
    @Override
    public Set<String> getStateVariablesRead() {
      return new HashSet<>(Arrays.asList("CPU_ResetSequence"));
    }

    @Override
    public Set<String> getStateVariablesWritten() {
      return new HashSet<>();
    }

    @Override
    public List<SExpression> generateCode(Map<String, Symbol> inputs,
        Map<String, Symbol> outputs) {
      List<SExpression> exprs = new LinkedList<>();
      
      Symbol ResetSequence = inputs.get("CPU_ResetSequence");
      exprs.add(new Assertion(new EqualsExpression(ResetSequence, expectedResetSequence)));
      
      return exprs;
    }
    
  }
  
  @Test(timeout=10000)
  public void testPowerUpInReset() throws IOException {
    // test that the first reset state is 000
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    // initialize CPU once
    CodeGenerator cpuPowerOn = new CPUPowerOn();
    exprs.addAll(reg.apply(cpuPowerOn));
    
    // check that our initial reset state is good
    CodeGenerator verifyReset0 = new VerifyResetSequence(new BinaryConstant("000")); 
    exprs.addAll(reg.apply(verifyReset0));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
    
  }
  
  @Test(timeout=10000)
  public void testResetStates() throws IOException {
    // test that reset states are moved through in order from 000 to 111
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
    
    CodeGenerator memoryControllerFront = new CPUMemoryControllerFrontHalf(pageHandlers);
    CodeGenerator memoryControllerBack = new CPUMemoryControllerBackHalf(pageHandlers);
    
    // initialize CPU once
    CodeGenerator cpuPowerOn = new CPUPowerOn();
    exprs.addAll(reg.apply(cpuPowerOn));
    
    CodeGenerator cpuCycle = new CPUCycle();
    
    BinaryConstant[] expectedReset = new BinaryConstant[]{
        new BinaryConstant("000"),
        new BinaryConstant("001"),
        new BinaryConstant("010"),
        new BinaryConstant("011"),
        new BinaryConstant("100"),
        new BinaryConstant("101"),
        new BinaryConstant("110"),
        new BinaryConstant("111"),
    };
    
    // each cycle should increment reset sequence by 1
    for (int i = 0; i < 8; ++i) {
      CodeGenerator verifyResetN = new VerifyResetSequence(expectedReset[i]);
      
      exprs.addAll(reg.apply(verifyResetN));
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
    
  }
  
}
