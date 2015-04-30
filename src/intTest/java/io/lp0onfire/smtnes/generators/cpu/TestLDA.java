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

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestLDA {
  
  @Test(timeout=60 * 1000)
  public void testLDA_IMM() throws IOException {
    // opcode A9
    
    // run the program
    // $0000: A9 5A  LDA #$5A
    // and expect to see $5A in A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0, 0xA9);
    burner.write(1, 0x5A);
    // reset vector
    burner.write(0xFFD, 0x00); // high byte
    burner.write(0xFFC, 0x00); // low byte
    
    
    // map the first and last page into RAM
    ArrayList<PageHandler> pageHandlers = new ArrayList<>(16);
    PageHandler ramPageHandler = burner.getCPUPageHandler();
    PageHandler nullPageHandler = new NullPageHandler();
    pageHandlers.add(0, ramPageHandler);
    for (int i = 1; i < 15; ++i) {
      pageHandlers.add(i, nullPageHandler);
    }
    pageHandlers.add(15, ramPageHandler);
    
    CodeGenerator memoryControllerFront = new CPUMemoryControllerFrontHalf(pageHandlers);
    CodeGenerator memoryControllerBack = new CPUMemoryControllerBackHalf(pageHandlers);
    
    // initialize CPU once
    CodeGenerator cpuPowerOn = new CPUPowerOn();
    exprs.addAll(reg.apply(cpuPowerOn));
    
    // initialize RAM
    CodeGenerator initRAM = burner.getInitializer();
    exprs.addAll(reg.apply(initRAM));
    
    CodeGenerator cpuCycle = new CPUCycle();
    CodeGenerator verifyStateInstructionFetch = new VerifyCPUState(CPUState.InstructionFetch);
    
    // reset sequence
    for (int i = 0; i < 8; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    exprs.addAll(reg.apply(verifyStateInstructionFetch));
    
    // execute instruction
    // LDA immediate takes 2 cycles
    
    for (int i = 0; i < 2; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that we are in state InstructionFetch
    exprs.addAll(reg.apply(verifyStateInstructionFetch));
    // check that the program counter is pointing to $0003
    CodeGenerator verifyPC = new CodeGenerator() {

      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_PC"
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
        
        Symbol PC = inputs.get("CPU_PC");
        exprs.add(new Assertion(new EqualsExpression(PC, new BinaryConstant("0000000000000011"))));
        
        return exprs;
      }
      
    };
    exprs.addAll(reg.apply(verifyPC));
    // check that A = $5A
    CodeGenerator verifyA = new CodeGenerator() {

      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_A"
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
        
        Symbol A = inputs.get("CPU_A");
        exprs.add(new Assertion(new EqualsExpression(A, new BinaryConstant("01011010"))));
        
        return exprs;
      }
      
    };
    exprs.addAll(reg.apply(verifyA));
    
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
    
  }
  
  // TODO check the values in P after executing this instruction
  
}
