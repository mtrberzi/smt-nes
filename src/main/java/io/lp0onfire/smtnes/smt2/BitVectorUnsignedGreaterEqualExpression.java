package io.lp0onfire.smtnes.smt2;

public class BitVectorUnsignedGreaterEqualExpression extends ExpressionList {

  public BitVectorUnsignedGreaterEqualExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvuge"), e1, e2);
  }
  
}
