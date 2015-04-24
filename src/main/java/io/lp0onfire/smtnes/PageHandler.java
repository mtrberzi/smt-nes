package io.lp0onfire.smtnes;

public interface PageHandler extends CodeGenerator {

  // Every page handler must respond to the following prefixed signals:
  // ChipSelect, Address, WriteEnable, DataIn, DataOut
  
  // Return a unique prefix ending with an underscore for variables belonging to this handler.
  String getHandlerPrefix();
  
}
