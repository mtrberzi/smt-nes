package io.lp0onfire.smtnes.smt2;

import org.apache.commons.lang3.ArrayUtils;

public class EqualsExpression extends ExpressionList {

  public EqualsExpression(SExpression... expr) {
    super(ArrayUtils.addAll(
        new SExpression[] { new Symbol("=") }, expr));
  }
  
}
