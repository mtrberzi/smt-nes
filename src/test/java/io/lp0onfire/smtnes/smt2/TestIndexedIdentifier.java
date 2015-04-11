package io.lp0onfire.smtnes.smt2;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestIndexedIdentifier {

  @Test
  public void testCreation_OneIndex() {
    new IndexedIdentifier(new Symbol("foo"), new Numeral("0"));
  }
  
  @Test
  public void testCreation_MultipleIndices() {
    new IndexedIdentifier(new Symbol("foo"), new Numeral("0"), new Numeral("0"));
  }
  
  @Test
  public void testGetSymbol() {
    IndexedIdentifier i = new IndexedIdentifier(new Symbol("foo"), new Numeral("0"));
    assertEquals("foo", i.getSymbol().getName());
  }
  
  @Test
  public void testGetIndices_OneIndex() {
    IndexedIdentifier i = new IndexedIdentifier(new Symbol("foo"), new Numeral("0"));
    assertEquals(1, i.getIndices().size());
  }
  
  @Test
  public void testGetIndices_MultipleIndices() {
    IndexedIdentifier i = new IndexedIdentifier(new Symbol("foo"), new Numeral("0"), new Numeral("1"));
    assertEquals(2, i.getIndices().size());
    assertEquals("0", i.getIndices().get(0).getDigits());
    assertEquals("1", i.getIndices().get(1).getDigits());
  }
  
  @Test(expected=java.lang.IllegalArgumentException.class)
  public void testCreation_NoIndices_Illegal() {
    new IndexedIdentifier(new Symbol("bogus"));
  }
  
}
