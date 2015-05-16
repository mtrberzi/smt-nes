package io.lp0onfire.smtnes.generators.cpu;

import org.apache.commons.lang3.StringUtils;

import io.lp0onfire.smtnes.smt2.BinaryConstant;

public enum CPUState {

  Resetting(0), // instead look at ResetSequence
  InstructionFetch(1), // read instruction from DataIn
  LDA_IMM_Cycle1(2),
  LDA_ZPG_Cycle1(3),
  LDA_ZPG_Cycle2(4),
  LDA_ABS_Cycle1(5),
  LDA_ABS_Cycle2(6),
  LDA_ABS_Cycle3(7),
  LDA_ZPX_Cycle1(8),
  LDA_ZPX_Cycle2(9),
  LDA_ZPX_Cycle3(10),
  LDA_ABX_Cycle1(11),
  LDA_ABX_Cycle2(12),
  LDA_ABX_Cycle2x(13),
  LDA_ABX_Cycle3(14),
  LDA_ABY_Cycle1(15),
  LDA_ABY_Cycle2(16),
  LDA_ABY_Cycle2x(17),
  LDA_ABY_Cycle3(18),
  LDA_INX_Cycle1(19),
  LDA_INX_Cycle2(20),
  LDA_INX_Cycle3(21),
  LDA_INX_Cycle4(22),
  LDA_INX_Cycle5(23),
  LDA_INY_Cycle1(24),
  LDA_INY_Cycle2(25),
  LDA_INY_Cycle3(26),
  LDA_INY_Cycle3x(27),
  LDA_INY_Cycle4(28), 
  LDX_IMM_Cycle1(29), 
  LDX_ZPG_Cycle1(30), 
  LDX_ZPG_Cycle2(31), 
  LDX_ZPY_Cycle1(32), 
  LDX_ZPY_Cycle2(33), 
  LDX_ZPY_Cycle3(34), 
  LDX_ABS_Cycle1(35), 
  LDX_ABS_Cycle2(36), 
  LDX_ABS_Cycle3(37), 
  LDX_ABY_Cycle1(38), 
  LDX_ABY_Cycle2(39), 
  LDX_ABY_Cycle2x(40), 
  LDX_ABY_Cycle3(41),
  STA_ZPG_Cycle1(42),
  STA_ZPG_Cycle2(43),
  ;
  
  private final int index;
  public int getIndex() {
    return this.index;
  }
  
  public BinaryConstant toBinaryConstant() {
    String bits = Integer.toBinaryString(getIndex());
    // zero-pad on the left
    int zeroCount = getStateWidth() - bits.length();
    return new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
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
