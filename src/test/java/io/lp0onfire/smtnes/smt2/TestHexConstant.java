package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestHexConstant {

  @Test
  public void testCreation() {
    new HexConstant("abcdefABCDEF0123456789");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_Empty_Illegal() {
    new HexConstant("");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_NonHexDigit_Illegal() {
    new HexConstant("baddudes");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_Symbol_Illegal() {
    new HexConstant("f00?");
  }
  
  @Test
  public void testGetDigits() {
    String digits = "f00f";
    HexConstant b = new HexConstant(digits);
    assertEquals(digits, b.getDigits());
  }
  
  @Test
  public void testToString() {
    String digits = "f00f";
    HexConstant b = new HexConstant(digits);
    assertEquals("#x" + digits, b.toString());
  }
  
}
