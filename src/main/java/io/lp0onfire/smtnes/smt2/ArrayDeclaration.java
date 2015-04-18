package io.lp0onfire.smtnes.smt2;

public class ArrayDeclaration extends ExpressionList {

  private final Symbol symbol;
  public Symbol getSymbol() {
    return this.symbol;
  }
  private final Numeral addressWidth;
  public Numeral getAddressWidth() {
    return this.addressWidth;
  }
  private final Numeral dataWidth;
  public Numeral getDataWidth() {
    return this.dataWidth;
  }
  
  // (declare-fun [symbol] () (Array (_ BitVec [addressWidth]) (_ BitVec [dataWidth])))
  public ArrayDeclaration(Symbol symbol, Numeral addressWidth, Numeral dataWidth) {
    super(new Symbol("declare-fun"), symbol, new ExpressionList(),
        new ExpressionList(new Symbol("Array"),
            new IndexedIdentifier(new Symbol("BitVec"), addressWidth),
            new IndexedIdentifier(new Symbol("BitVec"), dataWidth)));
    // it isn't an official rule that array widths must be >0, but
    // semantically having width=0 is nonsense since bitvectors
    // of width 0 are not allowed
    if (addressWidth.getDigits().equals("0")) {
      throw new IllegalArgumentException("address width must be greater than 0");
    }
    if (dataWidth.getDigits().equals("0")) {
      throw new IllegalArgumentException("data width must be greater than 0");
    }
    this.symbol = symbol;
    this.addressWidth = addressWidth;
    this.dataWidth = dataWidth;
  }
  
}
