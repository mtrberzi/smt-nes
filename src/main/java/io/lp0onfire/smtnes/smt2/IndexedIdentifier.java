package io.lp0onfire.smtnes.smt2;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public class IndexedIdentifier extends ExpressionList implements Identifier {
  
  // Indexed identifiers are defined as the application
  // of the reserved word _ to a symbol
  // and one or more indices, given by numerals.
  
  private final Symbol symbol;
  public Symbol getSymbol() {
    return this.symbol;
  }
  
  private final List<Numeral> indices;
  public List<Numeral> getIndices() {
    return this.indices;
  }
  
  public IndexedIdentifier(Symbol sym, Numeral... numerals) {
    super(ArrayUtils.addAll(new SExpression[]{
        new Symbol("_"),
        sym
    }, numerals));
    if (numerals.length == 0) {
      throw new IllegalArgumentException("indexed identifier must contain at least one index");
    } else {
      this.symbol = sym;
      this.indices = new LinkedList<Numeral>(Arrays.asList(numerals));
    }
  }
  
}
