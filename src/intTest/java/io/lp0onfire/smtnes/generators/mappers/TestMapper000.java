package io.lp0onfire.smtnes.generators.mappers;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.Mapper;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.ROM;
import io.lp0onfire.smtnes.ROMBuilder;
import io.lp0onfire.smtnes.StateVariableRegistry;
import io.lp0onfire.smtnes.Z3;
import io.lp0onfire.smtnes.generators.cpu.BusDriver;
import io.lp0onfire.smtnes.generators.cpu.ChipSelectDriver;
import io.lp0onfire.smtnes.generators.cpu.VerifyPageHandlerDataOut;
import io.lp0onfire.smtnes.smt2.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestMapper000 {

  // initializing a 32K PRG ROM takes Z3 49.84 seconds on an i7 at 3.20 GHz
  // so we should be decently generous with these timeouts
  
  @Test(timeout=90 * 1000)
  public void testInitialization() throws IOException {
    // initialize NROM with 32K of PRG_ROM
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    // create a 32K PRG ROM and initialize it
    
    byte[] PRG_ROM = new byte[32768];
    for (int i = 0; i < 32768; ++i) {
      PRG_ROM[i] = (byte)0x00;
    }
    ROMBuilder builder = new ROMBuilder();
    builder.setPRG_ROM(PRG_ROM);
    builder.setMapperNumber(0);
    ROM rom = builder.build();
    
    Mapper nrom = new Mapper000(rom);
    CodeGenerator mapperInit = nrom.getMapperInitializer();
    
    exprs.addAll(reg.apply(mapperInit));

    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
  }
  
  @Test(timeout=90 * 1000)
  public void testReadPRG_32K() throws IOException {
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    // create a 32K PRG ROM and initialize it
    
    int targetIndex = 42;
    
    byte[] PRG_ROM = new byte[32768];
    for (int i = 0; i < 32768; ++i) {
      PRG_ROM[i] = (byte)0xBD;
    }
    PRG_ROM[targetIndex] = (byte)0xA5;
    ROMBuilder builder = new ROMBuilder();
    builder.setPRG_ROM(PRG_ROM);
    builder.setMapperNumber(0);
    ROM rom = builder.build();
    
    Mapper nrom = new Mapper000(rom);
    CodeGenerator mapperInit = nrom.getMapperInitializer();
    // read $802A
    CodeGenerator busDriver = new BusDriver(
        new BinaryConstant("1" + "000000000101010"), new BinaryConstant("0"), new BinaryConstant("00000000"));
    PageHandler mapperHandler = nrom.getCPUPageHandler(8);
    CodeGenerator csDrive = new ChipSelectDriver(mapperHandler.getHandlerPrefix(), new BinaryConstant("0"));
    CodeGenerator dataVerifier = new VerifyPageHandlerDataOut(mapperHandler.getHandlerPrefix(), new BinaryConstant("10100101"));
    
    exprs.addAll(reg.apply(mapperInit));
    exprs.addAll(reg.apply(busDriver));
    exprs.addAll(reg.apply(csDrive));
    exprs.addAll(reg.apply(mapperHandler));
    exprs.addAll(reg.apply(dataVerifier));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }
  }
  
  @Test(timeout=90 * 1000)
  public void testReadPRG_16K_Mirrors() throws IOException {
    // create a 16K PRG ROM and initialize it
    // read at target address but in the high page ($C000 - $FFFF)
    
    List<SExpression> exprs = new LinkedList<>();
    StateVariableRegistry reg = new StateVariableRegistry();
    
    int targetIndex = 42;
    
    byte[] PRG_ROM = new byte[16384];
    for (int i = 0; i < 16384; ++i) {
      PRG_ROM[i] = (byte)0xBD;
    }
    PRG_ROM[targetIndex] = (byte)0xA5;
    ROMBuilder builder = new ROMBuilder();
    builder.setPRG_ROM(PRG_ROM);
    builder.setMapperNumber(0);
    ROM rom = builder.build();
    
    Mapper nrom = new Mapper000(rom);
    CodeGenerator mapperInit = nrom.getMapperInitializer();
    // read $802A
    CodeGenerator busDriver = new BusDriver(
        new BinaryConstant("1" + "100000000101010"), new BinaryConstant("0"), new BinaryConstant("00000000"));
    PageHandler mapperHandler = nrom.getCPUPageHandler(8);
    CodeGenerator csDrive = new ChipSelectDriver(mapperHandler.getHandlerPrefix(), new BinaryConstant("0"));
    CodeGenerator dataVerifier = new VerifyPageHandlerDataOut(mapperHandler.getHandlerPrefix(), new BinaryConstant("10100101"));
    
    exprs.addAll(reg.apply(mapperInit));
    exprs.addAll(reg.apply(busDriver));
    exprs.addAll(reg.apply(csDrive));
    exprs.addAll(reg.apply(mapperHandler));
    exprs.addAll(reg.apply(dataVerifier));
    
    try(Z3 z3 = new Z3()) {
      z3.open();
      for(SExpression expr : exprs) {
        z3.write(expr.toString());
      }
      assertTrue(z3.checkSat());
    }

  }
  
}
