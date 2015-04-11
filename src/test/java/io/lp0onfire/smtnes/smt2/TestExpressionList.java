package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class TestExpressionList {

  @Test
  public void testCreation_FromArray() {
    SExpression[] exprs = new SExpression[]{};
    new ExpressionList(exprs);
  }
  
  @Test
  public void testCreation_FromList() {
    List<SExpression> exprs = new LinkedList<SExpression>();
    new ExpressionList(exprs);
  }
  
  @Test
  public void testGetExprs_Empty() {
    List<SExpression> exprs = new LinkedList<SExpression>();
    ExpressionList l = new ExpressionList(exprs);
    assertEquals(0, l.getExprs().size());
  }
  
  @Test
  public void testGetExprs_NonEmpty() {
    List<SExpression> exprs = new LinkedList<SExpression>();
    exprs.add(new Numeral("1"));
    ExpressionList l = new ExpressionList(exprs);
    assertEquals(1, l.getExprs().size());
  }

  @Test
  public void testToString_Empty() {
    List<SExpression> exprs = new LinkedList<SExpression>();
    ExpressionList l = new ExpressionList(exprs);
    assertEquals("( )", l.toString());
  }
  
  @Test
  public void testToString_NonEmpty() {
    List<SExpression> exprs = new LinkedList<SExpression>();
    exprs.add(new Numeral("1"));
    ExpressionList l = new ExpressionList(exprs);
    assertEquals("( 1 )", l.toString());
  }
  
}
