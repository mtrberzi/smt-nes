package io.lp0onfire.smtnes.smt2;

public class BitVectorExtractExpression extends ExpressionList {

  public BitVectorExtractExpression(SExpression bitvector, Numeral upperIndex, Numeral lowerIndex) {
    super(new IndexedIdentifier(new Symbol("extract"), upperIndex, lowerIndex), bitvector);
  }
  
}
