package io.lp0onfire.smtnes.smt2;

import org.apache.commons.lang3.ArrayUtils;

public class DistinctExpression extends ExpressionList {

  public DistinctExpression(SExpression... expr) {
    super(ArrayUtils.addAll(
        new SExpression[] { new Symbol("distinct") }, expr));
  }
  
}
