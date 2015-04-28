package io.lp0onfire.smtnes.smt2;

public class Implication extends ExpressionList {

  public Implication(SExpression premise, SExpression conclusion) {
    super(new Symbol("=>"), premise, conclusion);
  }
  
}
