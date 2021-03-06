package io.lp0onfire.smtnes.smt2;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ExpressionList extends SExpression {

  private final List<SExpression> exprs;
  public List<SExpression> getExprs() {
    return this.exprs;
  }
  
  public ExpressionList(SExpression... values) {
    if (values == null) {
      throw new NullPointerException("null subexpression in ExpressionList");
    }
    for (SExpression value : values) {
      if (value == null) {
        throw new NullPointerException("null subexpression in ExpressionList");
      }
    }
    exprs = new LinkedList<SExpression>(Arrays.asList(values));
  }
  
  public ExpressionList(List<SExpression> values) {
    if (values == null) {
      throw new NullPointerException("null subexpression in ExpressionList");
    }
    for (SExpression value : values) {
      if (value == null) {
        throw new NullPointerException("null subexpression in ExpressionList");
      }
    }
    exprs = new LinkedList<SExpression>(values);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (SExpression expr : exprs) {
      sb.append(" ");
      sb.append(expr.toString());
    }
    sb.append(" )");
    return sb.toString();
  }
  
}
