package io.lp0onfire.smtnes.smt2;

import org.apache.commons.lang3.ArrayUtils;

public class OrExpression extends ExpressionList {

  public OrExpression(SExpression... expr) {
    super(ArrayUtils.addAll(
        new SExpression[] { new Symbol("or") }, expr));
  }
  
}
