package io.lp0onfire.smtnes;

import io.lp0onfire.smtnes.smt2.Symbol;
import io.lp0onfire.smtnes.smt2.SExpression;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestStateVariableRegistry {
  
  @Test
  public void testWriteOnly() {
    StateVariableRegistry reg = new StateVariableRegistry();
    CodeGenerator gen  = new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<>(Arrays.asList("foo"));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        // we expect to receive no inputs
        assertTrue("received unexpected input", inputs.isEmpty());
        // we expect to receive one output, called "foo"
        assertTrue("received wrong number of outputs", outputs.size() == 1);
        // check that we got (_ foo 0)
        Symbol foo = outputs.get("foo");
        assertEquals("output 'foo' has wrong symbol name", "foo_0", foo.getName());
        return new LinkedList<>();
      }
    };
    reg.apply(gen);
  }
  
  @Test(expected=IllegalStateException.class)
  public void testReadBeforeWrite_Illegal() {
    StateVariableRegistry reg = new StateVariableRegistry();
    CodeGenerator gen  = new CodeGenerator(){
      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>(Arrays.asList("foo"));
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<>();
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        return new LinkedList<>();
      }
    };
    reg.apply(gen);
  }
  
  @Test
  public void testReadAfterWrite() {
    StateVariableRegistry reg = new StateVariableRegistry();
    CodeGenerator init = new CodeGenerator() {

      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<>(Arrays.asList("foo"));
      }

      @Override
      public List<SExpression> generateCode(
          Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        return new LinkedList<>();
      }
      
    };
    CodeGenerator update = new CodeGenerator() {

      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>(Arrays.asList("foo"));
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        return new HashSet<>(Arrays.asList("foo"));
      }

      @Override
      public List<SExpression> generateCode(
          Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        // we should see input: foo_0 and output: foo_1
        
        assertTrue(inputs.containsKey("foo"));
        Symbol foo_in = inputs.get("foo");
        assertEquals("input 'foo' has wrong symbol name", "foo_0", foo_in.getName());
        
        assertTrue(outputs.containsKey("foo"));
        Symbol foo_out = outputs.get("foo");
        assertEquals("output 'foo' has wrong symbol name", "foo_1", foo_out.getName());
        
        return new LinkedList<>();
      }
      
    };
    reg.apply(init);
    reg.apply(update);
  }
  
}
