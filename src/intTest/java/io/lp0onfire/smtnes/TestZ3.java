package io.lp0onfire.smtnes;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.Test;

public class TestZ3 {
  
  @Test
  public void testFindZ3() throws FileNotFoundException {
    try(Z3 z3 = new Z3()) { }
  }
 
  @Test(timeout=1000)
  public void testTrivialAssertion() throws Exception {
    try(Z3 z3 = new Z3()) {
      z3.open();
      assertTrue(z3.checkSat());
    }
  }
  
  @Test(timeout=1000)
  public void testSimpleSMT2() throws Exception {
    try(Z3 z3 = new Z3()) {
      z3.open();
      z3.write("(declare-fun x () (_ BitVec 6))");
      z3.write("(assert (= (bvnot x) x))");
      assertFalse(z3.checkSat());
    }
  }
  
}
