package io.lp0onfire.smtnes.smt2;

public class ArrayWriteExpression extends ExpressionList {

  public ArrayWriteExpression(SExpression array, SExpression index, SExpression value) {
    super(new Symbol("store"), array, index, value);
  }
  
}
