package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestSymbol {

  @Test
  public void testCreation_Letters() {
    new Symbol("abc");
  }
  
  @Test
  public void testCreation_LettersDigits() {
    new Symbol("abc123");
  }
  
  @Test
  public void testCreation_LettersDigitsSpecials() {
    new Symbol("abc123~!@$%^&*_-+=<>.?/");
  }
  
  @Test
  public void testGetName() {
    String name = "foo123";
    Symbol symbol = new Symbol(name);
    assertEquals(name, symbol.getName());
  }
  
  @Test
  public void testToString() {
    String name = "foo123";
    Symbol symbol = new Symbol(name);
    assertEquals(name, symbol.toString());
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_EmptyName_Illegal() {
    new Symbol("");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_StartsWithDigit_Illegal() {
    new Symbol("1abc");
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_UnknownSpecial_Illegal() {
    new Symbol("#");
  }
  
}
