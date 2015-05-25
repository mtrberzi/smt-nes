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

import org.apache.commons.collections4.ListUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestAND {
  
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
  public void testAND_IMM() throws IOException {
    // opcode 29
    
    // run the program
    // $0000: 29 5A  AND #$5A
    // with A = $0F
    // and expect to see $0A in A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x29);
    burner.write(0x001, 0x5A);
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
            "CPU_A"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol A = outputs.get("CPU_A");
        exprs.add(new BitVectorDeclaration(A, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(A, new HexConstant("0F"))));
        
        return exprs;
      }
    }));
    
    // execute instruction
    // AND immediate takes 2 cycles
    
    for (int i = 0; i < 2; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $0A
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
        exprs.add(new EqualsExpression(A, new HexConstant("0A")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
  
  @Test
  public void testAND_ZPG() throws IOException {
    // opcode A5
    
    // run the program
    // $0000: A5 10  AND $10
    // ...
    // $0010: 2A
    // and expect to see $10 in register A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x25);
    burner.write(0x001, 0x10);
    burner.write(0x010, 0x2A);
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
    // AND zero-page takes 3 cycles
    
    for (int i = 0; i < 3; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $2A
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
        exprs.add(new EqualsExpression(A, new HexConstant("2A")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
  
  @Test
  public void testAND_ZPX() throws IOException {
    // opcode B5
    
    // run the program
    // $0000: B5 11
    // ...
    // $0018: 53
    // with X = $07
    // and expect to see $53 in A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x35);
    burner.write(0x001, 0x11);
    burner.write(0x018, 0x53);
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
    
    // set X = $07
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
        exprs.add(new Assertion(new EqualsExpression(X, new HexConstant("07"))));
        
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
    // AND zeropage,x takes 4 cycles
    
    for (int i = 0; i < 4; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $53
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
        exprs.add(new EqualsExpression(A, new HexConstant("53")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
  
  @Test
  public void testAND_ABS() throws IOException {
    // opcode AD
    
    // run the program
    // $0000: AD 2A 03  AND $032A
    // ...
    // $032A: E2
    // and expect to see $E2 in A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x2D);
    burner.write(0x001, 0x2A);
    burner.write(0x002, 0x03);
    burner.write(0x32A, 0xE2);
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
    // AND absolute takes 4 cycles
    
    for (int i = 0; i < 4; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $E2
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
        exprs.add(new EqualsExpression(A, new HexConstant("E2")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
  
  @Test
  public void testAND_ABX() throws IOException {
    // opcode BD
    
    // run the program
    // $0000: BD 20 03 AND $0320,X
    // ...
    // $032A: 49
    // with X = $0A
    // and expect to see $49 in A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x3D);
    burner.write(0x001, 0x20);
    burner.write(0x002, 0x03);
    burner.write(0x32A, 0x49);
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
    
    // set X = $0A
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
        exprs.add(new Assertion(new EqualsExpression(X, new HexConstant("0A"))));
        
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
    // AND absolute,x takes 4 cycles if the page is not crossed
    
    for (int i = 0; i < 4; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $49
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
        exprs.add(new EqualsExpression(A, new HexConstant("49")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
  
  @Test
  public void testAND_ABY() throws IOException {
    // opcode B9
    
    // run the program
    // $0000: B9 20 03 AND $0320,Y
    // ...
    // $032A: 49
    // with Y = $0A
    // and expect to see $49 in A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x39);
    burner.write(0x001, 0x20);
    burner.write(0x002, 0x03);
    burner.write(0x32A, 0x49);
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
    
    // set Y = $0A
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_Y"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol Y = outputs.get("CPU_Y");
        exprs.add(new BitVectorDeclaration(Y, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(Y, new HexConstant("0A"))));
        
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
    // AND absolute,y takes 4 cycles if the page is not crossed
    
    for (int i = 0; i < 4; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $49
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
        exprs.add(new EqualsExpression(A, new HexConstant("49")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
  
  @Test
  public void testAND_INX() throws IOException {
    // opcode A1
    
    // run the program
    // $0000: A1 20  AND ($20,X)
    // ...
    // $0032: 76 05
    // ...
    // $0576: D4
    // with X = $12
    // and expect to see A = $D4
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x21);
    burner.write(0x001, 0x20);
    burner.write(0x032, 0x76);
    burner.write(0x033, 0x05);
    burner.write(0x576, 0xD4);
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
    
    // set Y = $0A
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
        exprs.add(new Assertion(new EqualsExpression(X, new HexConstant("12"))));
        
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
    // AND indirect,x takes 6 cycles
    
    for (int i = 0; i < 6; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $D4
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
        exprs.add(new EqualsExpression(A, new HexConstant("D4")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
  
  @Test
  public void testAND_INY() throws IOException {
    // opcode B1
    
    // run the program
    // $0000: B1 40  AND ($40),Y
    // ...  
    // $0040: 40 02
    // ...
    // $025A: 3F
    // with Y = $1A
    // and expect to see $3F in A
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    ROMBurner burner = new ROMBurner("TestROM");
    burner.write(0x000, 0x31);
    burner.write(0x001, 0x40);
    burner.write(0x040, 0x40);
    burner.write(0x041, 0x02);
    burner.write(0x25A, 0x3F);
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
    
    // set Y = $1A
    exprs.addAll(reg.apply(new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<String>(Arrays.asList(new String[]{
            "CPU_Y"
        }));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        Symbol Y = outputs.get("CPU_Y");
        exprs.add(new BitVectorDeclaration(Y, new Numeral("8")));
        exprs.add(new Assertion(new EqualsExpression(Y, new HexConstant("1A"))));
        
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
    // AND indirect,y takes 5 cycles if we don't cross the page
    
    for (int i = 0; i < 5; ++i) {
      exprs.addAll(reg.apply(cpuCycle));
      exprs.addAll(reg.apply(memoryControllerFront));
      exprs.addAll(reg.apply(nullPageHandler));
      exprs.addAll(reg.apply(ramPageHandler));
      exprs.addAll(reg.apply(memoryControllerBack));
    }
    
    // check that A = $3F
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
        exprs.add(new EqualsExpression(A, new HexConstant("3F")));
        
        return exprs;
      }
      
    };
    List<SExpression> positiveContingents = new LinkedList<>();
    List<SExpression> negativeContingents = new LinkedList<>();
    for (SExpression expr : reg.apply(verifyA)) {
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
