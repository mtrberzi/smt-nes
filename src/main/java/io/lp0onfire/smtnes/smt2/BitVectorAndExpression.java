package io.lp0onfire.smtnes.smt2;

public class BitVectorAndExpression extends ExpressionList {
  public BitVectorAndExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvand"), e1, e2);
  }
}
