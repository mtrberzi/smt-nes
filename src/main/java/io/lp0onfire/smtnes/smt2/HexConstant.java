package io.lp0onfire.smtnes.smt2;

public class HexConstant extends SExpression {

  private final String digits;
  public String getDigits() {
    return this.digits;
  }
  
  public void verifyDigits(String digits) {
    if (digits.length() == 0) {
      throw new IllegalArgumentException("hex constant cannot be empty");
    } else {
      for (int i = 0; i < digits.length(); ++i) {
        char c = digits.charAt(i);
        if (Character.isDigit(c)) {
          continue;
        } else if (Character.isAlphabetic(c)) {
          char cl = Character.toLowerCase(c);
          if (cl == 'a' || cl == 'b' || cl == 'c' || cl == 'd' || cl == 'e' || cl == 'f') {
            continue;
          } else {
            throw new IllegalArgumentException("hex constant must consist only of digits 0-9a-fA-F");
          }
        } else {
          throw new IllegalArgumentException("hex constant must consist only of digits 0-9a-fA-F");
        }
      }
    }
  }
  
  public HexConstant(String digits) {
    verifyDigits(digits);
    this.digits = digits;
  }
  
  @Override
  public String toString() {
    return "#x" + digits;
  }
  
}
