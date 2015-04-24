package io.lp0onfire.smtnes;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;

import org.junit.Test;

public class TestSTP {
  
  @Test
  public void testFindSTP() throws FileNotFoundException {
    try(STP stp = new STP()) { }
  }
 
  @Test(timeout=1000)
  public void testTrivialAssertion() throws Exception {
    try(STP stp = new STP()) {
      stp.open();
      assertTrue(stp.checkSat());
    }
  }
  
  @Test(timeout=1000)
  public void testSimpleSMT2() throws Exception {
    
  }
  
}
