package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestNumeral {

  @Test
  public void testCreation_Zero() {
    new Numeral("0");
  }
  
  @Test
  public void testCreation_NonZero() {
    new Numeral("1");
  }
  
  @Test
  public void testCreation_NonZero_ManyDigits() {
    new Numeral("1234567890");
  }
  
  @Test
  public void testToString() {
    String digits = "1234567890";
    Numeral n = new Numeral(digits);
    assertEquals(digits, n.toString());
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_EmptyDigits_Illegal() {
    new Numeral("");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_LeadingZero_Illegal() {
    new Numeral("01");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_NonDigits_Illegal() {
    new Numeral("123a");
  }
  
}
