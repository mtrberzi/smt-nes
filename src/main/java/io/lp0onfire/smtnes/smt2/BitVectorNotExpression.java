package io.lp0onfire.smtnes.smt2;

public class BitVectorNotExpression extends ExpressionList {
  public BitVectorNotExpression(SExpression e) {
    super(new Symbol("bvnot"), e);
  }
}
