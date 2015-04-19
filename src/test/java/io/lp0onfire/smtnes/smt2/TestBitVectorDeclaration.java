package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestBitVectorDeclaration {

  @Test
  public void testCreation() {
    new BitVectorDeclaration(new Symbol("foo"), new Numeral("8"));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCreation_ZeroWidth_Illegal() {
    new BitVectorDeclaration(new Symbol("foo"), new Numeral("0"));
  }
  
  @Test
  public void testGetSymbol() {
    Symbol s = new Symbol("foo");
    Numeral w = new Numeral("8");
    BitVectorDeclaration decl = new BitVectorDeclaration(s, w);
    assertEquals(s, decl.getSymbol());
  }
  
  @Test
  public void testGetWidth() {
    Symbol s = new Symbol("foo");
    Numeral w = new Numeral("8");
    BitVectorDeclaration decl = new BitVectorDeclaration(s, w);
    assertEquals(w, decl.getWidth());
  }
  
}
