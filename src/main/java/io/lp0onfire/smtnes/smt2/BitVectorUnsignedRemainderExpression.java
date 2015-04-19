package io.lp0onfire.smtnes.smt2;

public class BitVectorUnsignedRemainderExpression extends ExpressionList {
  public BitVectorUnsignedRemainderExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvurem"), e1, e2);
  }
}
