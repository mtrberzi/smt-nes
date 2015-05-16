package io.lp0onfire.smtnes;

import java.util.LinkedList;
import java.util.List;

import io.lp0onfire.smtnes.smt2.SExpression;

// Used to mark a replacement point in an expression list.
public class Marker extends SExpression {

  private final String id;
  public String getID() {
    return this.id;
  }
  
  private final List<SExpression> exprs;
  public List<SExpression> getExprs() {
    return this.exprs;
  }
  
  public Marker(String id){
    this.id = id;
    this.exprs = new LinkedList<>();
  }
  
  public Marker(String id, List<SExpression> exprs) {
    this.id = id;
    this.exprs = exprs;
  }
  
  @Override
  public String toString() {
    return "*** MARKER " + id + " ***";
  }
  
}
