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
  
}
