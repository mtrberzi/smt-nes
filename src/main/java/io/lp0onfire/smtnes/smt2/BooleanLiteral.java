package io.lp0onfire.smtnes.smt2;

public class BooleanLiteral extends Symbol {

  private final boolean literal;
  public boolean getLiteral() {
    return this.literal;
  }
  
  public BooleanLiteral(boolean lit) {
    super(lit ? "true" : "false");
    this.literal = lit;
  }
  
}
