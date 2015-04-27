package io.lp0onfire.smtnes;

import org.apache.commons.lang3.StringUtils;

public abstract class Mapper {

  private final ROM rom;
  public ROM getROM() {
    return this.rom;
  }
  
  public Mapper(ROM rom) {
    this.rom = rom;
  }
  
  public abstract Integer getMapperNumber();
  
  public String getMapperPrefix() {
    // zero-pad mapper number to 3 digits
    String mapperNumber = getMapperNumber().toString();
    int zeroCount = 3 - mapperNumber.length();
    return "Mapper" + StringUtils.repeat('0', zeroCount) + mapperNumber + "_";
  }
  
  // code generator for mapper initial state, e.g. ROM contents, bank latches, etc.
  public abstract CodeGenerator getMapperInitializer();
  // indicator function for which CPU pages a mapper selects
  public abstract boolean decodesCPUPage(int pageNumber);
  // page handler for mapper RAM access, as a function of page number
  public PageHandler getCPUPageHandler(int pageNumber) {
    if (decodesCPUPage(pageNumber)) {
      return _getCPUPageHandler(pageNumber);
    } else {
      throw new IllegalArgumentException("mapper has no handler at page " + pageNumber);
    }
  }
  protected abstract PageHandler _getCPUPageHandler(int pageNumber);
  
}
