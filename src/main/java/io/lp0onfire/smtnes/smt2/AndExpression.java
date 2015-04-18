package io.lp0onfire.smtnes.smt2;

import org.apache.commons.lang3.ArrayUtils;

public class AndExpression extends ExpressionList {

  public AndExpression(SExpression... expr) {
    super(ArrayUtils.addAll(
        new SExpression[] { new Symbol("and") }, expr));
  }
  
}
