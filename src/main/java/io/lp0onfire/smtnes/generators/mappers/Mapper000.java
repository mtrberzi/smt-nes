package io.lp0onfire.smtnes.generators.mappers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.Mapper;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.ROM;
import io.lp0onfire.smtnes.smt2.*;

// NROM mapper, iNES #000.
// PRG ROM size: 16 KiB / 32 KiB
// PRG ROM bank size: not bank-switched
// PRG RAM: (not supported) 2 or 4 KiB, not bank-switched
// CHR capacity: (not supported) 8 KiB ROM
// CHR bank size: not bank-switched
// Nametable mirroring: (not supported) hard-wired horizontal/vertical mirroring

// Memory map:
// CPU $6000 - $7FFF: (not supported) PRG RAM, mirrored as necessary to fill an 8 KiB window
// CPU $8000 - $BFFF: first 16KB of PRG ROM
// CPU $C000 - $FFFF: last 16KB of rom (NROM-256) or mirror of $8000 - $BFFF (NROM-128)

public class Mapper000 extends Mapper {
  
  private final boolean use32K_PRG_ROM;
  
  public Mapper000(ROM rom) {
    super(rom);
    // check the size of PRG_ROM
    int PRG_ROM_size = rom.getPRG_ROM().length;
    if (PRG_ROM_size == 32768) {
      // spans $8000 - $FFFF
      use32K_PRG_ROM = true;
    } else if (PRG_ROM_size == 16384) {
      // spans $8000 - $BFFF
      use32K_PRG_ROM = false;
    } else {
      throw new IllegalArgumentException("incorrect PRG ROM size " + PRG_ROM_size + ", expected 16384 or 32768 bytes of PRG ROM");
    }
    
    // construct mapper initializer
    mapperInitializer = new CodeGenerator() {

      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        String prefix = getMapperPrefix();
        return new HashSet<>(Arrays.asList(prefix + "PRG_ROM"));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        int PRG_ROM_size = getROM().getPRG_ROM().length;
        String prefix = getMapperPrefix();
        // declare PRG_ROM array
        Symbol PRG_ROM = outputs.get(prefix + "PRG_ROM");
        // figure out how many bits we need for the address
        Integer PRG_ROM_address_bits;
        if (use32K_PRG_ROM) {
          PRG_ROM_address_bits = 15;
        } else {
          PRG_ROM_address_bits = 14;
        }
        exprs.add(new ArrayDeclaration(PRG_ROM, new Numeral(PRG_ROM_address_bits.toString()), new Numeral("8")));
        // fill the contents of PRG_ROM
        for (int addr = 0; addr < PRG_ROM_size; ++addr) {
          // calculate N-bit address
          String bits = Integer.toBinaryString(addr);
          // zero-pad on the left
          int zeroCount = PRG_ROM_address_bits - bits.length();
          BinaryConstant index = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
          byte data = getROM().readPRG_ROM(addr);
          exprs.add(new Assertion(new EqualsExpression(
              new ArrayReadExpression(PRG_ROM, index), new BinaryConstant(data))));
        }
        
        return exprs;
      }
      
    };
    
    PRG_ROM_PageHandler = new PageHandler() {

      @Override
      public String getHandlerPrefix() {
        return getMapperPrefix() + "PRG_ROM_Handler";
      }

      @Override
      public Set<String> getCustomStateVariablesRead() {
        String prefix = getMapperPrefix();
        return new HashSet<>(Arrays.asList(prefix + "PRG_ROM"));
      }

      @Override
      public Set<String> getCustomStateVariablesWritten() {
        return new HashSet<>();
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        // declare ROM DataOut
        Symbol DataOut = outputs.get(getHandlerPrefix() + "DataOut");
        exprs.add(new BitVectorDeclaration(DataOut, new Numeral("8")));
        
        // we can basically ignore CS and WE because PRG_ROM is never actually written
        Symbol Address = inputs.get("CPU_AddressBus");
        Symbol PRG_ROM = inputs.get(getMapperPrefix() + "PRG_ROM");
        
        // figure out how many bits we need for the address
        Integer PRG_ROM_address_bits;
        if (use32K_PRG_ROM) {
          PRG_ROM_address_bits = 15;
        } else {
          PRG_ROM_address_bits = 14;
        }
        
        SExpression romAddress = new BitVectorExtractExpression(Address, 
            new Numeral(Integer.toString(PRG_ROM_address_bits - 1)), new Numeral("0"));
        
        exprs.add(new Assertion(new EqualsExpression(DataOut, new ArrayReadExpression(PRG_ROM, romAddress))));
        
        return exprs;
      }
      
    };
    
  }
  
  public Integer getMapperNumber() { return 0; }

  private final CodeGenerator mapperInitializer;
  
  @Override
  public CodeGenerator getMapperInitializer() {
    return mapperInitializer;
  }

  @Override
  public boolean decodesCPUPage(int pageNumber) {
    // assume no PRG RAM
    return (pageNumber >= 8 && pageNumber <= 15);
  }

  private final PageHandler PRG_ROM_PageHandler;
  
  @Override
  protected PageHandler _getCPUPageHandler(int pageNumber) {
    if (pageNumber >= 8 && pageNumber <= 15) {
      return PRG_ROM_PageHandler;
    } else {
      return null;
    }
  }
  
}
