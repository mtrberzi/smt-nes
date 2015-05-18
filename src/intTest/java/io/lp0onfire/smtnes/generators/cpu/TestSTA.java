package io.lp0onfire.smtnes.generators.cpu;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.Marker;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.Z3;
import io.lp0onfire.smtnes.smt2.Assertion;
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

public class TestSTA {

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
  
  @Test
  public void testSTA_ZPG() throws IOException {
    // opcode 85
    
    // run the program
    // $0000: 85 10  STA $10
    // with A = $23
    // and assert that we see a write to $0010 with data = $23
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x85);
    burner.write(0x001, 0x10);
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
    
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_A"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol A = outputs.get("CPU_A");
        exprs.add(new BitVectorDeclaration(A, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(A, new HexConstant("23"))));
        
        return exprs;
      }
    }));
    
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
    // STA zero-page takes 3 cycles
    
    CodeGenerator verifyWrite = new VerifyCPUWrite(new HexConstant("0010"), new HexConstant("23"));
    
    for (int i = 0; i < 3; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      if (i == 1) {
        exprs.add(new Marker("verifyWrite", reg.apply(verifyWrite)));
      }
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    boolean positiveResult;
    boolean negativeResult;
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(extra);
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      positiveResult = z3.checkSat();
    }
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(new NotExpression(extra));
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      negativeResult = z3.checkSat();
    }
    
    interpretResult(positiveResult, negativeResult);
    
  }
  

  @Test
  public void testSTA_ZPX() throws IOException {
    // opcode 95
    
    // run the program
    // $0000: 95 10  STA $10
    // with A = $23, X = $08
    // and assert that we see a write to $0018 with data = $23
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x95);
    burner.write(0x001, 0x10);
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
    
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_A"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol A = outputs.get("CPU_A");
        exprs.add(new BitVectorDeclaration(A, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(A, new HexConstant("23"))));
        
        return exprs;
      }
    }));
    
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_X"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol X = outputs.get("CPU_X");
        exprs.add(new BitVectorDeclaration(X, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(X, new HexConstant("08"))));
        
        return exprs;
      }
    }));
    
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
    // STA zero-page,x takes 4 cycles
    
    CodeGenerator verifyWrite = new VerifyCPUWrite(new HexConstant("0018"), new HexConstant("23"));
    
    for (int i = 0; i < 4; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      if (i == 2) {
        exprs.add(new Marker("verifyWrite", reg.apply(verifyWrite)));
      }
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    boolean positiveResult;
    boolean negativeResult;
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(extra);
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      positiveResult = z3.checkSat();
    }
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(new NotExpression(extra));
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      negativeResult = z3.checkSat();
    }
    
    interpretResult(positiveResult, negativeResult);
    
  }
  

  @Test
  public void testSTA_ABS() throws IOException {
    // opcode 8D
    
    // run the program
    // $0000: 8D 03 2A  STA $2A03
    // with A = $9B
    // and assert that we see a write to $2A03 with data = $9B
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x8D);
    burner.write(0x001, 0x03);
    burner.write(0x002, 0x2A);
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
    
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_A"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol A = outputs.get("CPU_A");
        exprs.add(new BitVectorDeclaration(A, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(A, new HexConstant("9B"))));
        
        return exprs;
      }
    }));
    
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
    // STA absolute takes 4 cycles
    
    CodeGenerator verifyWrite = new VerifyCPUWrite(new HexConstant("2A03"), new HexConstant("9B"));
    
    for (int i = 0; i < 4; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      if (i == 2) {
        exprs.add(new Marker("verifyWrite", reg.apply(verifyWrite)));
      }
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    boolean positiveResult;
    boolean negativeResult;
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(extra);
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      positiveResult = z3.checkSat();
    }
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(new NotExpression(extra));
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      negativeResult = z3.checkSat();
    }
    
    interpretResult(positiveResult, negativeResult);
    
  }
  

  @Test
  public void testSTA_ABX() throws IOException {
    // opcode 9D
    
    // run the program
    // $0000: 9D 90 29  STA $2990,X
    // with A = $9B, X = $73
    // and assert that we see a write to $2A03 with data = $9B
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x9D);
    burner.write(0x001, 0x90);
    burner.write(0x002, 0x29);
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
    
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_A"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol A = outputs.get("CPU_A");
        exprs.add(new BitVectorDeclaration(A, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(A, new HexConstant("9B"))));
        
        return exprs;
      }
    }));
    
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_X"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol X = outputs.get("CPU_X");
        exprs.add(new BitVectorDeclaration(X, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(X, new HexConstant("73"))));
        
        return exprs;
      }
    }));
    
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
    // STA absolute,x takes 5 cycles
    
    CodeGenerator verifyWrite = new VerifyCPUWrite(new HexConstant("2A03"), new HexConstant("9B"));
    
    for (int i = 0; i < 5; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      if (i == 3) {
        exprs.add(new Marker("verifyWrite", reg.apply(verifyWrite)));
      }
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    boolean positiveResult;
    boolean negativeResult;
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(extra);
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      positiveResult = z3.checkSat();
    }
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        if (expr instanceof Marker) {
          Marker marker = (Marker) expr;
          for (SExpression extra : marker.getExprs()) {
            SExpression assertion = new Assertion(new NotExpression(extra));
            z3.write(assertion.toString());
          }
        } else {
          z3.write(expr.toString()); 
        }
      }
      negativeResult = z3.checkSat();
    }
    
    interpretResult(positiveResult, negativeResult);
    
  }
  
  
}
