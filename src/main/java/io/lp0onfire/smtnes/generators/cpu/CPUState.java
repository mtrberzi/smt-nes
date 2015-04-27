package io.lp0onfire.smtnes.generators.cpu;

import io.lp0onfire.smtnes.smt2.BinaryConstant;

public enum CPUState {

  Resetting(0), // instead look at ResetSequence
  InstructionFetch(1), // read instruction from DataIn
  ;
  
  private final int index;
  public int getIndex() {
    return this.index;
  }
  
  public BinaryConstant toBinaryConstant() {
    // TODO
     throw new UnsupportedOperationException("not yet implemented");
  }
  
  private static Integer maxIndex = null;
  public static int getMaxIndex() {
    if (maxIndex == null) {
      maxIndex = Integer.MIN_VALUE;
      for (CPUState state : CPUState.values()) {
        if (state.getIndex() > maxIndex) {
          maxIndex = state.getIndex();
        }
      }
    }
    return maxIndex;
  }
  
  public static int getStateWidth() {
    // Get the number of bits needed to represent any CPUState as a bitvector.
    return Integer.toBinaryString(getMaxIndex()).length();
  }
  
  CPUState(int index) {
    this.index = index;
  }
  
}
