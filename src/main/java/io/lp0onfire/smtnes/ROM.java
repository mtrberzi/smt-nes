package io.lp0onfire.smtnes;

public class ROM {

  private final byte[] PRG_ROM;
  public byte[] getPRG_ROM() {
    return PRG_ROM;
  }
  public byte readPRG_ROM(int address) {
    return PRG_ROM[address];
  }
  
  private final int mapperNumber;
  public int getMapperNumber() {
    return mapperNumber;
  }
  
  public ROM(byte[] PRG_ROM, int mapperNumber) {
    this.PRG_ROM = PRG_ROM;
    this.mapperNumber = mapperNumber;
  }
  
}
