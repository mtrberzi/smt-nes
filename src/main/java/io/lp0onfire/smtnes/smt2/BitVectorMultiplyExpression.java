package io.lp0onfire.smtnes.smt2;

public class BitVectorMultiplyExpression extends ExpressionList {
  public BitVectorMultiplyExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvmul"), e1, e2);
  }
}
