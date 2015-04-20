package io.lp0onfire.smtnes;

import io.lp0onfire.smtnes.smt2.IndexedIdentifier;
import io.lp0onfire.smtnes.smt2.SExpression;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestStateVariableRegistry {
  
  @Test
  public void testWriteOnly() {
    StateVariableRegistry reg = new StateVariableRegistry();
    CodeGenerator gen  = new CodeGenerator(){
      @Override
      public List<String> getStateVariablesRead() {
        return new LinkedList<>();
      }

      @Override
      public List<String> getStateVariablesWritten() {
        return Arrays.asList("foo");
      }

      @Override
      public List<SExpression> generateCode(Map<String, IndexedIdentifier> inputs,
          Map<String, IndexedIdentifier> outputs) {
        // we expect to receive no inputs
        assertTrue("received unexpected input", inputs.isEmpty());
        // we expect to receive one output, called "foo"
        assertTrue("received wrong number of outputs", outputs.size() == 1);
        // check that we got (_ foo 0)
        IndexedIdentifier foo = outputs.get("foo");
        assertEquals("output 'foo' has wrong symbol name", "foo", foo.getSymbol().getName());
        assertEquals("output 'foo' has wrong index", "0", foo.getIndices().get(0).getDigits());
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
      public List<String> getStateVariablesRead() {
        return Arrays.asList("foo");
      }

      @Override
      public List<String> getStateVariablesWritten() {
        return new LinkedList<>();
      }

      @Override
      public List<SExpression> generateCode(Map<String, IndexedIdentifier> inputs,
          Map<String, IndexedIdentifier> outputs) {
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
      public List<String> getStateVariablesRead() {
        return new LinkedList<>();
      }

      @Override
      public List<String> getStateVariablesWritten() {
        return Arrays.asList("foo");
      }

      @Override
      public List<SExpression> generateCode(
          Map<String, IndexedIdentifier> inputs,
          Map<String, IndexedIdentifier> outputs) {
        return new LinkedList<>();
      }
      
    };
    CodeGenerator update = new CodeGenerator() {

      @Override
      public List<String> getStateVariablesRead() {
        return Arrays.asList("foo");
      }

      @Override
      public List<String> getStateVariablesWritten() {
        return Arrays.asList("foo");
      }

      @Override
      public List<SExpression> generateCode(
          Map<String, IndexedIdentifier> inputs,
          Map<String, IndexedIdentifier> outputs) {
        // we should see input: (_ foo 0) and output: (_ foo 1)
        
        assertTrue(inputs.containsKey("foo"));
        IndexedIdentifier foo_in = inputs.get("foo");
        assertEquals("input 'foo' has wrong symbol name", "foo", foo_in.getSymbol().getName());
        assertEquals("input 'foo' has wrong index", "0", foo_in.getIndices().get(0).getDigits());
        
        assertTrue(outputs.containsKey("foo"));
        IndexedIdentifier foo_out = outputs.get("foo");
        assertEquals("output 'foo' has wrong symbol name", "foo", foo_out.getSymbol().getName());
        assertEquals("output 'foo' has wrong index", "1", foo_out.getIndices().get(0).getDigits());
        
        return new LinkedList<>();
      }
      
    };
    reg.apply(init);
    reg.apply(update);
  }
  
}
