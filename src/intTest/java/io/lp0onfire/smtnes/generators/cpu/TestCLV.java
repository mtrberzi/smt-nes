package io.lp0onfire.smtnes.generators.cpu;

import static org.junit.Assert.fail;
import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.Z3;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.BinaryConstant;
import io.lp0onfire.smtnes.smt2.BitVectorDeclaration;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.HexConstant;
import io.lp0onfire.smtnes.smt2.NotExpression;
import io.lp0onfire.smtnes.smt2.Numeral;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.junit.Test;

public class TestCLV {

  private void interpretResult(boolean pos, boolean neg) {
    if (pos && !neg) {
      return; // fine
    } else if (pos && neg) {
      fail("contingency check failed; formula is valid regardless of assertion");
    } else if (!pos && neg) {
      fail("contingency check failed; execution produces incorrect value (-pos, +neg)");
    } else { // !pos && !neg
      fail("contingency check failed; formula is unsatisfiable regardless of assertion");
    }
  }
  
  @Test(timeout=60 * 1000)
  public void testCLC() throws IOException {
    // opcode B8
    
    // run the program
    // $0000: B8  CLV
    // with P = $FF
    // and expect to see P = $BF
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0xB8);
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
    
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_P"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol P = outputs.get("CPU_P");
        exprs.add(new BitVectorDeclaration(P, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(P, new HexConstant("FF"))));
        
        return exprs;
      }
    }));
    
    // execute instruction
    // CLC takes 2 cycles
    
    for (int i = 0; i < 2; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check P
    CodeGenerator verifyX = new CodeGenerator() {

      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_P"
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
        
        Symbol P = inputs.get("CPU_P");
        exprs.add(new EqualsExpression(P, new BinaryConstant("10111111")));
        
        return exprs;
      }
      
    };
    
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyX)) {
      positiveContingents.add(new Assertion(expr));
      negativeContingents.add(new Assertion(new NotExpression(expr)));
    }
    
    boolean positiveResult;
    boolean negativeResult;
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : ListUtils.union(exprs, positiveContingents)) {
        z3.write(expr.toString());
      }
      positiveResult = z3.checkSat();
    }
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : ListUtils.union(exprs, negativeContingents)) {
        z3.write(expr.toString());
      }
      negativeResult = z3.checkSat();
    }
    
    interpretResult(positiveResult, negativeResult);
    
  }
  
}
