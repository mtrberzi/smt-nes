package io.lp0onfire.smtnes.smt2;

import org.apache.commons.lang3.ArrayUtils;

public class XorExpression extends ExpressionList {

  public XorExpression(SExpression... expr) {
    super(ArrayUtils.addAll(
        new SExpression[] { new Symbol("xor") }, expr));
  }
  
}
