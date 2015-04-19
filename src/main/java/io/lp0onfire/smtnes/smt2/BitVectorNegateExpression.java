package io.lp0onfire.smtnes.smt2;

public class BitVectorNegateExpression extends ExpressionList {
  public BitVectorNegateExpression(SExpression e) {
    super(new Symbol("bvneg"), e);
  }
}
