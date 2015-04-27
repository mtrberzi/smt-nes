package io.lp0onfire.smtnes;

public class ROMBuilder {

  private byte[] PRG_ROM = new byte[0];
  public void setPRG_ROM(byte[] PRG_ROM) {
    this.PRG_ROM = PRG_ROM;
  }
  
  private int mapperNumber = 0;
  public void setMapperNumber(int mapperNumber) {
    this.mapperNumber = mapperNumber;
  }
  
  public ROM build() {
    return new ROM(PRG_ROM, mapperNumber);
  }
  
}
