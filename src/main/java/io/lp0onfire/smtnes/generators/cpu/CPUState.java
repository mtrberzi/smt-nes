package io.lp0onfire.smtnes.generators.cpu;

public enum CPUState {

  Resetting(0);
  
  private final int index;
  public int getIndex() {
    return this.index;
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
