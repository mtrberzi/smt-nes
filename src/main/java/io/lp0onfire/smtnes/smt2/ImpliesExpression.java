package io.lp0onfire.smtnes.smt2;

public class ImpliesExpression extends ExpressionList {

  public ImpliesExpression(SExpression premise, SExpression conclusion) {
    super(new Symbol("=>"), premise, conclusion);
  }
  
}
