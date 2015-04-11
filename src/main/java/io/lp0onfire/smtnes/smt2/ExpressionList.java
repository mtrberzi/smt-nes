package io.lp0onfire.smtnes.smt2;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class ExpressionList extends SExpression {

  private List<SExpression> exprs = new LinkedList<SExpression>();
  public List<SExpression> getExprs() {
    return this.exprs;
  }
  public void setExprs(SExpression... values) {
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
