package io.lp0onfire.smtnes.generators.cpu;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestCPUState {

  @Test
  public void testNonNegative() {
    // Make sure that all CPU states have a non-negative index.
    for (CPUState state : CPUState.values()) {
      if (state.getIndex() < 0) {
        fail("CPU state " + state.toString() + " has negative index");
      }
    }
  }
  
  @Test
  public void testUniqueIndices() {
    // Make sure that all CPU states have a unique index.
    Map<Integer, CPUState> indices = new HashMap<>();
    for (CPUState state : CPUState.values()) {
      if (indices.containsKey(state.getIndex())) {
        fail("CPU states " + state.toString() + " and " + indices.get(state.getIndex()) + " have duplicate index");
      } else {
        indices.put(state.getIndex(), state);
      }
    }
  }
  
}
