package io.lp0onfire.smtnes.smt2;

public class BitVectorLeftShiftExpression extends ExpressionList {
  public BitVectorLeftShiftExpression(SExpression shiftedValue, SExpression shiftAmount) {
    super(new Symbol("bvshl"), shiftedValue, shiftAmount);
  }
}
