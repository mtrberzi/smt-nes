package io.lp0onfire.smtnes.generators.cpu;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.PageHandler;
import io.lp0onfire.smtnes.smt2.ArrayDeclaration;
import io.lp0onfire.smtnes.smt2.ArrayReadExpression;
import io.lp0onfire.smtnes.smt2.Assertion;
import io.lp0onfire.smtnes.smt2.BinaryConstant;
import io.lp0onfire.smtnes.smt2.BitVectorDeclaration;
import io.lp0onfire.smtnes.smt2.BitVectorExtractExpression;
import io.lp0onfire.smtnes.smt2.EqualsExpression;
import io.lp0onfire.smtnes.smt2.Numeral;
import io.lp0onfire.smtnes.smt2.SExpression;
import io.lp0onfire.smtnes.smt2.Symbol;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

// Burns a 1-page (256 bytes) ROM and provides initialization and page handlers.
public class ROMBurner {
  
  private final String uniquePrefix;
  private byte[] romData = new byte[0x1000];
  
  public void write(int address, int data) {
    romData[address] = (byte)data;
  }
  
  public ROMBurner(String uPrefix) {
    this.uniquePrefix = uPrefix;
    
    // the safest thing to do is burn NOP (opcode EA) at every address
    for (int i = 0; i < 0x1000; ++i) {
      write(i, 0xEA);
    }
    
    // construct mapper initializer
    mapperInitializer = new CodeGenerator() {

      @Override
      public Set<String> getStateVariablesRead() {
        return new HashSet<>();
      }

      @Override
      public Set<String> getStateVariablesWritten() {
        String prefix = uniquePrefix;
        return new HashSet<>(Arrays.asList(prefix + "PRG_ROM"));
      }

      @Override
      public List<SExpression> generateCode(Map<String, Symbol> inputs,
          Map<String, Symbol> outputs) {
        List<SExpression> exprs = new LinkedList<>();
        
        String prefix = uniquePrefix;
        // declare PRG_ROM array
        Symbol PRG_ROM = outputs.get(prefix + "PRG_ROM");
        // figure out how many bits we need for the address
        Integer PRG_ROM_address_bits = 12;
        exprs.add(new ArrayDeclaration(PRG_ROM, new Numeral(PRG_ROM_address_bits.toString()), new Numeral("8")));
        // fill the contents of PRG_ROM
        for (int addr = 0; addr < 0x1000; ++addr) {
          // calculate N-bit address
          String bits = Integer.toBinaryString(addr);
          // zero-pad on the left
          int zeroCount = PRG_ROM_address_bits - bits.length();
          BinaryConstant index = new BinaryConstant(StringUtils.repeat('0', zeroCount) + bits);
          byte data = romData[addr];
          exprs.add(new Assertion(new EqualsExpression(
              new ArrayReadExpression(PRG_ROM, index), new BinaryConstant(data))));
        }
        
        return exprs;
      }
      
    };
    
    PRG_ROM_PageHandler = new PageHandler() {

      @Override
      public String getHandlerPrefix() {
        return uniquePrefix + "PRG_ROM_Handler";
      }

      @Override
      public Set<String> getCustomStateVariablesRead() {
        String prefix = uniquePrefix;
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
        Symbol PRG_ROM = inputs.get(uniquePrefix + "PRG_ROM");
        
        // figure out how many bits we need for the address
        Integer PRG_ROM_address_bits = 12;
        
        SExpression romAddress = new BitVectorExtractExpression(Address, 
            new Numeral(Integer.toString(PRG_ROM_address_bits - 1)), new Numeral("0"));
        
        exprs.add(new Assertion(new EqualsExpression(DataOut, new ArrayReadExpression(PRG_ROM, romAddress))));
        
        return exprs;
      }
      
    };
    
  }

  private final CodeGenerator mapperInitializer;
  
  public CodeGenerator getInitializer() {
    return mapperInitializer;
  }

  private final PageHandler PRG_ROM_PageHandler;
  
  public PageHandler getCPUPageHandler() {
    return PRG_ROM_PageHandler;
  }
}
