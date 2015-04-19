package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestArrayDeclaration {

  @Test
  public void testCreation() {
    new ArrayDeclaration(new Symbol("foo"), new Numeral("8"), new Numeral("16"));
  }
 
  @Test(expected=IllegalArgumentException.class)
  public void testCreation_ZeroAddressWidth_Illegal() {
    new ArrayDeclaration(new Symbol("bogus"), new Numeral("0"), new Numeral("16"));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCreation_ZeroDataWidth_Illegal() {
    new ArrayDeclaration(new Symbol("bogus"), new Numeral("8"), new Numeral("0"));
  }
  
  @Test
  public void testGetSymbol() {
    Symbol s = new Symbol("foo");
    Numeral addr = new Numeral("8");
    Numeral data = new Numeral("16");
    ArrayDeclaration decl = new ArrayDeclaration(s, addr, data);
    assertEquals(s, decl.getSymbol());
  }
  
  @Test
  public void testGetAddressWidth() {
    Symbol s = new Symbol("foo");
    Numeral addr = new Numeral("8");
    Numeral data = new Numeral("16");
    ArrayDeclaration decl = new ArrayDeclaration(s, addr, data);
    assertEquals(addr, decl.getAddressWidth());
  }
  
  @Test
  public void testGetDataWidth() {
    Symbol s = new Symbol("foo");
    Numeral addr = new Numeral("8");
    Numeral data = new Numeral("16");
    ArrayDeclaration decl = new ArrayDeclaration(s, addr, data);
    assertEquals(data, decl.getDataWidth());
  }
  
}
