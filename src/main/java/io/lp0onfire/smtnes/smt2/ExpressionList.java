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
    exprs = new LinkedList<SExpression>(Arrays.asList(values));
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
