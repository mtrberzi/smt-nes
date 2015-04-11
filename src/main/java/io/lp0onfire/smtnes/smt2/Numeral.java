package io.lp0onfire.smtnes.smt2;


public class Numeral extends SExpression {

  private final String digits;
  public String getDigits() {
    return this.digits;
  }
  
  public void verifyDigits(String digits) {
    if (digits.length() == 0) {
      throw new IllegalArgumentException("digits must be non-empty");
    } else {
      if (digits.length() == 1 && digits.charAt(0) == '0') {
        // A numeral is the digit 0
        return;
      } else if (digits.charAt(0) == '0') {
        // A numeral is a non-empty sequence of digits not starting with 0
        throw new IllegalArgumentException("digits cannot have leading zero");
      } else {
        for (int i = 0; i < digits.length(); ++i) {
          if (Character.isDigit(digits.charAt(i))) {
            continue;
          } else {
            throw new IllegalArgumentException("numeral must contain only digits");
          }
        }
      }
    }
  }
  
  public Numeral(String digits) {
    verifyDigits(digits);
    this.digits = digits;
  }
  
  @Override
  public String toString() {
    return this.digits;
  }
  
}
