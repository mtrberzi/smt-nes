package io.lp0onfire.smtnes.smt2;

public class ArrayReadExpression extends ExpressionList {

  public ArrayReadExpression(SExpression array, SExpression index) {
    super(new Symbol("select"), array, index);
  }
  
}
