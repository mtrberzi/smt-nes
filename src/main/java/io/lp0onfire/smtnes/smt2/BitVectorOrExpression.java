package io.lp0onfire.smtnes.smt2;

public class BitVectorOrExpression extends ExpressionList {
  public BitVectorOrExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvor"), e1, e2);
  }
}
