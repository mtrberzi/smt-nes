package io.lp0onfire.smtnes.smt2;

public class Symbol extends SExpression implements Identifier {

  private final String name;
  public String getName() {
    return this.name;
  }
  
  public void validateName(String name) {
    // assume simple symbols only
    if (name.length() == 0) {
      throw new IllegalArgumentException("symbol name cannot be empty");
    } else {
      if (Character.isDigit(name.charAt(0))) {
        throw new IllegalArgumentException("symbol name cannot start with a digit");
      }
      for (int i = 0; i < name.length(); ++i) {
        char ch = name.charAt(i);
        if (Character.isAlphabetic(ch)) {
          continue;
        } else if (Character.isDigit(ch)) {
          continue;
        } else {
          // check for special character in the set
          // ~ ! @ $ % ^ & * _ - + = < > . ? /
          String specialCharacters = "~!@$%^&*_-+=<>.?/";
          if (specialCharacters.indexOf(ch) != -1) {
            continue;
          } else {
            throw new IllegalArgumentException("symbol name must contain only letters, digits, and restricted special characters");
          }
        }
      }
    }
  }
  
  public Symbol(String name) { 
    validateName(name);
    this.name = name;
  }
  
  @Override
  public String toString() {
    return this.name;
  }
  
}
