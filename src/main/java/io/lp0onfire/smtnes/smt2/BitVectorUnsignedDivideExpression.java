package io.lp0onfire.smtnes.smt2;

public class BitVectorUnsignedDivideExpression extends ExpressionList {
  public BitVectorUnsignedDivideExpression(SExpression e1, SExpression e2) {
    super(new Symbol("bvudiv"), e1, e2);
  }
}
