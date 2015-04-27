package io.lp0onfire.smtnes.generators.cpu;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.Z3;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.smt2.ArrayDeclaration;
import io.lp0onfire.smtnes.smt2.ArrayReadExpression;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.BinaryConstant;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.Numeral;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

public class TestCPURAMHandler {

  // set up (prefix)ChipSelect, CPU_{RAM, AddressBus, WriteEnable, DataOut}
  // use InitCPURAM to get CPU_RAM, BusDriver to get CPU_{addr,we,do},
  // ChipSelectDriver to get (prefix)ChipSelect
  
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
  
  class VerifyRAMContents implements CodeGenerator {

    private final BinaryConstant readAddr;
    private final BinaryConstant expectedValue;
    
    public VerifyRAMContents(BinaryConstant readAddr, BinaryConstant expectedValue) {
      this.readAddr = readAddr;
      this.expectedValue = expectedValue;
    }
    
    @Override
    public Set<String> getStateVariablesRead() {
      return new HashSet<String>(Arrays.asList(new String[]{
          "CPU_RAM"
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
      
      Symbol RAM = inputs.get("CPU_RAM");
      exprs.add(new Assertion(new EqualsExpression(new ArrayReadExpression(RAM, readAddr), expectedValue)));
      
      return exprs;
    }
    
  }
  
  @Test(timeout=5000)
  public void testSyntaxOK() throws IOException {
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    PageHandler ramHandler = new CPURAMHandler();
    CodeGenerator ramInit = new InitCPURAM();
    CodeGenerator csDrive = new ChipSelectDriver(ramHandler.getHandlerPrefix(), new BinaryConstant("0"));
    CodeGenerator busDrive = new BusDriver(
        new BinaryConstant("0000" + "0" + "00000000000"), new BinaryConstant("0"), new BinaryConstant("00000000"));
    
    exprs.addAll(reg.apply(ramInit));
    exprs.addAll(reg.apply(csDrive));
    exprs.addAll(reg.apply(busDrive));
    exprs.addAll(reg.apply(ramHandler));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
  }
  
  @Test(timeout=5000)
  public void testCSLow_MemoryUnchanged() throws IOException {
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    String addrLowBits = "00000000000";
    
    BinaryConstant targetAddr = new BinaryConstant("0000" + "0" + addrLowBits);
    BinaryConstant ramAddr = new BinaryConstant(addrLowBits);
    
    PageHandler ramHandler = new CPURAMHandler();
    CodeGenerator ramInit = new InitCPURAM();
    CodeGenerator csDrive = new ChipSelectDriver(ramHandler.getHandlerPrefix(), new BinaryConstant("0"));
    CodeGenerator busDrive = new BusDriver(
        targetAddr, new BinaryConstant("1"), new BinaryConstant("11111111"));
    CodeGenerator verifier = new VerifyRAMContents(ramAddr, ramInitialValue);
    
    // If CS is low, the memory should not change.
    
    exprs.addAll(reg.apply(ramInit));
    exprs.addAll(reg.apply(csDrive));
    exprs.addAll(reg.apply(busDrive));
    exprs.addAll(reg.apply(verifier));
    exprs.addAll(reg.apply(ramHandler));
    exprs.addAll(reg.apply(verifier));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
  }
  
  @Test(timeout=5000)
  public void testReadMemory() throws IOException {
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    String addrLowBits = "00000000000";
    
    BinaryConstant targetAddr = new BinaryConstant("0000" + "0" + addrLowBits);
    BinaryConstant ramAddr = new BinaryConstant(addrLowBits);
    
    PageHandler ramHandler = new CPURAMHandler();
    CodeGenerator ramInit = new InitCPURAM();
    CodeGenerator csDrive = new ChipSelectDriver(ramHandler.getHandlerPrefix(), new BinaryConstant("1"));
    CodeGenerator busDrive = new BusDriver(
        targetAddr, new BinaryConstant("0"), new BinaryConstant("11111111"));
    CodeGenerator memoryVerifier = new VerifyRAMContents(ramAddr, ramInitialValue);
    CodeGenerator dataVerifier = new VerifyPageHandlerDataOut(ramHandler.getHandlerPrefix(), ramInitialValue);
    
    // If CS is high and WE is low, the memory should not change
    // and the value of DataOut should be the value in memory at that address.
    
    exprs.addAll(reg.apply(ramInit));
    exprs.addAll(reg.apply(csDrive));
    exprs.addAll(reg.apply(busDrive));
    exprs.addAll(reg.apply(memoryVerifier));
    exprs.addAll(reg.apply(ramHandler));
    exprs.addAll(reg.apply(memoryVerifier));
    exprs.addAll(reg.apply(dataVerifier));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
  }
  
  @Test(timeout=5000)
  public void testWriteMemory() throws IOException {
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    String addrLowBits = "00000000000";
    
    BinaryConstant targetAddr = new BinaryConstant("0000" + "0" + addrLowBits);
    BinaryConstant ramAddr = new BinaryConstant(addrLowBits);
    
    BinaryConstant targetValue = new BinaryConstant("11111111");
    
    PageHandler ramHandler = new CPURAMHandler();
    CodeGenerator ramInit = new InitCPURAM();
    CodeGenerator csDrive = new ChipSelectDriver(ramHandler.getHandlerPrefix(), new BinaryConstant("1"));
    CodeGenerator busDrive = new BusDriver(
        targetAddr, new BinaryConstant("1"), targetValue);
    CodeGenerator memoryVerifier1 = new VerifyRAMContents(ramAddr, ramInitialValue);
    CodeGenerator memoryVerifier2 = new VerifyRAMContents(ramAddr, targetValue);
    
    // If CS is high and WE is 1, the memory should be updated to reflect the written value.
    
    exprs.addAll(reg.apply(ramInit));
    exprs.addAll(reg.apply(csDrive));
    exprs.addAll(reg.apply(busDrive));
    exprs.addAll(reg.apply(memoryVerifier1));
    exprs.addAll(reg.apply(ramHandler));
    exprs.addAll(reg.apply(memoryVerifier2));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
  }
  
}
