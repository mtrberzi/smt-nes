package io.lp0onfire.smtnes.smt2;

public class BitVectorLogicalRightShiftExpression extends ExpressionList {
  public BitVectorLogicalRightShiftExpression(SExpression shiftedValue, SExpression shiftAmount) {
    super(new Symbol("bvlshr"), shiftedValue, shiftAmount);
  }
}
