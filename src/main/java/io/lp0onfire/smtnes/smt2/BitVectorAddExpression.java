package io.lp0onfire.smtnes.smt2;

public class BitVectorAddExpression extends ExpressionList {
  public BitVectorAddExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvadd"), e1, e2);
  }
}
