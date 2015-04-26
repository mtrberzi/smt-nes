package io.lp0onfire.smtnes.generators.cpu;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.STP;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.BinaryConstant;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

public class TestCPUMemoryControllerFrontHalf {

  // we need to set up CPU_{AddressBus, WriteEnable, DataOut}
  
  class NullPageHandler extends PageHandler {

    @Override
    public String getHandlerPrefix() {
      return "NullPage_";
    }

    @Override
    public Set<String> getCustomStateVariablesRead() {
      return new HashSet<String>();
    }

    @Override
    public Set<String> getCustomStateVariablesWritten() {
      return new HashSet<String>();
    }

    @Override
    public List<SExpression> generateCode(Map<String, Symbol> inputs,
        Map<String, Symbol> outputs) {
      return new LinkedList<SExpression>();
    }
    
  }
  
  class VerifyChipSelectHandler extends PageHandler {

    private final BinaryConstant expectedCS;
    
    public VerifyChipSelectHandler(BinaryConstant expectedChipSelect) {
      this.expectedCS = expectedChipSelect;
    }
    
    @Override
    public String getHandlerPrefix() {
      return "Verify_";
    }

    @Override
    public Set<String> getCustomStateVariablesRead() {
      return new HashSet<String>();
    }

    @Override
    public Set<String> getCustomStateVariablesWritten() {
      return new HashSet<String>();
    }

    @Override
    public List<SExpression> generateCode(Map<String, Symbol> inputs,
        Map<String, Symbol> outputs) {
      List<SExpression> exprs = new LinkedList<>();
      // read chip select and assert that it has the correct value
      Symbol chipSelect = inputs.get(getHandlerPrefix() + "ChipSelect");
      exprs.add(new Assertion(new EqualsExpression(chipSelect, expectedCS)));
      return exprs;
    }
    
  }
  
  @Test(timeout=5000)
  public void testSyntaxOK() throws IOException {
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    ArrayList<PageHandler> handlers = new ArrayList<>();
    for (int i = 0; i < 16; ++i) {
      handlers.add(i, new NullPageHandler());
    }
    CPUMemoryControllerFrontHalf memoryFront = new CPUMemoryControllerFrontHalf(handlers);
    exprs.addAll(reg.apply(new BusDriver(new BinaryConstant("0000000000000000"), new BinaryConstant("0"), new BinaryConstant("00000000"))));
    exprs.addAll(reg.apply(memoryFront));
    
    try(STP stp = new STP()) {
      stp.open();
      for(SExpression expr : exprs) {
        stp.write(expr.toString());
      }
      assertTrue(stp.checkSat());
    }
  }
  
  @Test(timeout=5000)
  public void testRead_ChipSelectAsserted() throws IOException {
    // check that addressing into page 7 ("0111") sets CS high for handler 7
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    ArrayList<PageHandler> handlers = new ArrayList<>();
    for (int i = 0; i < 16; ++i) {
      handlers.add(i, new NullPageHandler());
    }
    PageHandler csHandler = new VerifyChipSelectHandler(new BinaryConstant("1"));
    // override handler 7
    handlers.set(7, csHandler);
    CPUMemoryControllerFrontHalf memoryFront = new CPUMemoryControllerFrontHalf(handlers);
    exprs.addAll(reg.apply(new BusDriver(new BinaryConstant("0111" + "000000000000"), new BinaryConstant("0"), new BinaryConstant("00000000"))));
    exprs.addAll(reg.apply(memoryFront));
    exprs.addAll(reg.apply(csHandler));
    
    try(STP stp = new STP()) {
      stp.open();
      for(SExpression expr : exprs) {
        stp.write(expr.toString());
      }
      assertTrue(stp.checkSat());
    }
  }
 
  @Test(timeout=5000)
  public void testRead_ChipSelectNotAsserted() throws IOException {
    // check that addressing into page 7 ("0111") sets CS low for handler 13
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    ArrayList<PageHandler> handlers = new ArrayList<>();
    for (int i = 0; i < 16; ++i) {
      handlers.add(i, new NullPageHandler());
    }
    PageHandler csHandler = new VerifyChipSelectHandler(new BinaryConstant("0"));
    // override handler 13
    handlers.set(13, csHandler);
    CPUMemoryControllerFrontHalf memoryFront = new CPUMemoryControllerFrontHalf(handlers);
    exprs.addAll(reg.apply(new BusDriver(new BinaryConstant("0111" + "000000000000"), new BinaryConstant("0"), new BinaryConstant("00000000"))));
    exprs.addAll(reg.apply(memoryFront));
    exprs.addAll(reg.apply(csHandler));
    
    try(STP stp = new STP()) {
      stp.open();
      for(SExpression expr : exprs) {
        stp.write(expr.toString());
      }
      assertTrue(stp.checkSat());
    }
  }
  
}
