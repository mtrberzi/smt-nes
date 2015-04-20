package io.lp0onfire.smtnes.smt2;

public class Assertion extends ExpressionList {

  public Assertion(SExpression expr) {
    super(new Symbol("assert"), expr);
  }
  
}
