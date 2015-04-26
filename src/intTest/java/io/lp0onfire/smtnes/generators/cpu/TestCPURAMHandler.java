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
import io.lp0onfire.smtnes.STP;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.smt2.ArrayDeclaration;
import io.lp0onfire.smtnes.smt2.ArrayReadExpression;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.BinaryConstant;
import io.lp0onfire.smtnes.smt2.BitVectorDeclaration;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.Numeral;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

public class TestCPURAMHandler {

  // set up (prefix)ChipSelect, CPU_{RAM, AddressBus, WriteEnable, DataOut}
  // use InitCPURAM to get CPU_RAM, BusDriver to get CPU_{addr,we,do},
  // ChipSelectDriver to get (prefix)ChipSelect
  
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
            new ArrayReadExpression(RAM, index), new BinaryConstant("10100101"))));
      }
      return exprs;
    }
    
  }
  
  class ChipSelectDriver implements CodeGenerator {

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
    
    try(STP stp = new STP()) {
      stp.open();
      for(SExpression expr : exprs) {
        stp.write(expr.toString());
      }
      assertTrue(stp.checkSat());
    }
  }
  
}
