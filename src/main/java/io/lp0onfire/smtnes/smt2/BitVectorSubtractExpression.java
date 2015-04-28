package io.lp0onfire.smtnes.smt2;

public class BitVectorSubtractExpression extends ExpressionList {
  public BitVectorSubtractExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvsub"), e1, e2);
  }
}
