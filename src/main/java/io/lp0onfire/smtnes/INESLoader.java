package io.lp0onfire.smtnes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class INESLoader {

  public INESLoader() {

  }

  public ROM loadImage(File f) throws IOException, FileNotFoundException {
    try (FileInputStream in = new FileInputStream(f)) {
      byte[] header = new byte[16];
      in.read(header, 0, 16);
      // check id: header[0..3] == N E S 0x1a
      if (header[0] != 'N' || header[1] != 'E' || header[2] != 'S' || header[3] != 0x1a) {
        throw new IllegalArgumentException("File is not an iNES image.");
      }
      // check for non-standard header pieces
      if ((header[7] & 0x0c) == 0x04) {
        throw new IllegalArgumentException("Header is corrupted by 'DiskDude!'.");
      }
      if ((header[7] & 0x0c) == 0x0c) {
        throw new IllegalArgumentException("Header format not recognized.");
      }
      int PRGsize = header[4]; // in 4K pages
      int CHRsize = header[5]; // in 2K pages
      int iNESMapper = ((header[6] & 0xF0) >> 4) | (header[7] & 0xF0);
      int iNESFlags = (header[6] & 0x0f) | ((header[7] & 0x0f) << 4);
      int iNESVersion;
      if ((header[7] & 0x0c) == 0x08) {
        throw new IllegalArgumentException("Cannot load NES 2.0 images.");
      } else {
        // iNES 1.0
        iNESVersion = 1;
        for (int i = 8; i < 0x10; ++i) {
          if (header[i] != 0) {
            throw new IllegalArgumentException("Unrecognized data found in header at offset " + i + ".");
          }
        }
      }
      if ((iNESFlags & 0x04) != 0) {
        throw new IllegalArgumentException("Cannot load trained ROMs.");
      }
      
      byte[] PRG_ROM = new byte[PRGsize * 0x4000];
      in.read(PRG_ROM, 0, PRGsize * 0x4000);
      byte[] CHR_ROM = new byte[CHRsize * 0x2000];
      in.read(CHR_ROM, 0, CHRsize * 0x2000);

      // construct a ROM
      ROMBuilder romBuilder = new ROMBuilder();
      romBuilder.setPRG_ROM(PRG_ROM);
      romBuilder.setMapperNumber(iNESMapper);
      return romBuilder.build();
    }
  }

}
