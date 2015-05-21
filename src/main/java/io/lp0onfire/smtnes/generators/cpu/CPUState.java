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
  STA_ZPX_Cycle1(44),
  STA_ZPX_Cycle2(45),
  STA_ZPX_Cycle3(46),
  STA_ABS_Cycle1(47),
  STA_ABS_Cycle2(48),
  STA_ABS_Cycle3(49),
  STA_ABX_Cycle1(50),
  STA_ABX_Cycle2(51),
  STA_ABX_Cycle3(52),
  STA_ABX_Cycle4(53),
  STA_ABY_Cycle1(54),
  STA_ABY_Cycle2(55),
  STA_ABY_Cycle3(56),
  STA_ABY_Cycle4(57),
  STA_INX_Cycle1(58),
  STA_INX_Cycle2(59),
  STA_INX_Cycle3(60),
  STA_INX_Cycle4(61),
  STA_INX_Cycle5(62),
  STA_INY_Cycle1(63),
  STA_INY_Cycle2(64),
  STA_INY_Cycle3(65),
  STA_INY_Cycle4(66),
  STA_INY_Cycle5(67), 
  STX_ZPG_Cycle1(68), 
  STX_ZPG_Cycle2(69), 
  STX_ZPY_Cycle1(70), 
  STX_ZPY_Cycle2(71), 
  STX_ZPY_Cycle3(72), 
  STX_ABS_Cycle1(73), 
  STX_ABS_Cycle2(74), 
  STX_ABS_Cycle3(75),
  STY_ZPG_Cycle1(76), 
  STY_ZPG_Cycle2(77), 
  STY_ZPX_Cycle1(78), 
  STY_ZPX_Cycle2(79), 
  STY_ZPX_Cycle3(80), 
  STY_ABS_Cycle1(81), 
  STY_ABS_Cycle2(82), 
  STY_ABS_Cycle3(83),
  TAX_IMP_Cycle1(84),
  TAY_IMP_Cycle1(85),
  TSX_IMP_Cycle1(86),
  TXA_IMP_Cycle1(87),
  TXS_IMP_Cycle1(88),
  TYA_IMP_Cycle1(89),
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
