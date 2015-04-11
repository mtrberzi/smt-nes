package io.lp0onfire.smtnes.smt2;

import org.apache.commons.lang3.ArrayUtils;

public class IndexedIdentifier extends ExpressionList {
  
  // Indexed identifiers are defined as the application
  // of the reserved word _ to a symbol
  // and one or more indices, given by numerals.
  
  public IndexedIdentifier(Symbol sym, Numeral... numerals) {
    super(ArrayUtils.addAll(new SExpression[]{
        new Symbol("_"),
        sym
    }, numerals));
    if (numerals.length == 0) {
      throw new IllegalArgumentException("indexed identifier must contain at least one index");
    } else {
      
    }
  }
  
}
