package io.lp0onfire.smtnes.smt2;

public class BitVectorDeclaration extends ExpressionList {

  private final Symbol symbol;
  public Symbol getSymbol() {
    return this.symbol;
  }
  
  private final Numeral width;
  public Numeral getWidth() {
    return this.width;
  }
  
  // (declare-fun [symbol] () (_ BitVec [width]))
  public BitVectorDeclaration(Symbol symbol, Numeral width) {
    super(new Symbol("declare-fun"), symbol, new ExpressionList(),
        new IndexedIdentifier(new Symbol("BitVec"), width));
    if (width.getDigits().equals("0")) {
      throw new IllegalArgumentException("bitvector width must be greater than 0");
    }
    this.symbol = symbol;
    this.width = width;
  }
  
}
