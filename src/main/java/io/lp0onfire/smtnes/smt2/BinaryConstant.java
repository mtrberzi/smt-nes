package io.lp0onfire.smtnes.smt2;

public class BinaryConstant extends SExpression {

  private final String bits;
  public String getBits() {
    return this.bits;
  }
  
  public void verifyBits(String bits) {
    if (bits.length() == 0) {
      throw new IllegalArgumentException("binary constant cannot be empty");
    } else {
      for (int i = 0; i < bits.length(); ++i) {
        char c = bits.charAt(i);
        if (c == '0' || c == '1') {
          continue;
        } else {
          throw new IllegalArgumentException("binary constant must consist of only the characters 0 and 1");
        }
      }
    }
  }
  
  public BinaryConstant(String bits) {
    verifyBits(bits);
    this.bits = bits;
  }
  
  @Override
  public String toString() {
    return "#b" + bits;
  }
  
}
