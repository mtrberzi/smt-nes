package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestBinaryConstant {

  @Test
  public void testCreation() {
    new BinaryConstant("0110");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_Empty_Illegal() {
    new BinaryConstant("");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_NonBinaryDigit_Illegal() {
    new BinaryConstant("01210");
  }
  
  @Test
  public void testGetBits() {
    String bits = "0110";
    BinaryConstant b = new BinaryConstant(bits);
    assertEquals(bits, b.getBits());
  }
  
  @Test
  public void testToString() {
    String bits = "0110";
    BinaryConstant b = new BinaryConstant(bits);
    assertEquals("#b" + bits, b.toString());
  }
  
}
