package io.lp0onfire.smtnes.smt2;

public class ConditionalExpression extends ExpressionList {

  public ConditionalExpression(SExpression condition, SExpression iftrue, SExpression iffalse) {
    super(new Symbol("ite"), condition, iftrue, iffalse);
  }
  
}
