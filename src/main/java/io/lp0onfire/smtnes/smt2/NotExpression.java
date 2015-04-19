package io.lp0onfire.smtnes.smt2;

public class NotExpression extends ExpressionList {

  public NotExpression(SExpression subexpression) {
    super(new Symbol("not"), subexpression);
  }
  
}
