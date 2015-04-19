package io.lp0onfire.smtnes.smt2;

public class BitVectorUnsignedLessThanExpression extends ExpressionList {

  public BitVectorUnsignedLessThanExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvult"), e1, e2);
  }
  
}
