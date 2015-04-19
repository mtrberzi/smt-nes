package io.lp0onfire.smtnes.smt2;

public class BitVectorConcatExpression extends ExpressionList {

  public BitVectorConcatExpression(SExpression e1, SExpression e2) {
    super(new Symbol("concat"), e1, e2);
  }
  
}
