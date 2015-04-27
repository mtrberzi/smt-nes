package io.lp0onfire.smtnes.generators.cpu;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.lp0onfire.smtnes.CodeGenerator;
import io.lp0onfire.smtnes.smt2.*;

/**
 * Execute one CPU cycle, beginning at the conclusion of the previous
 * memory cycle (or at the start of a reset) and ending the next time the CPU accesses memory.
 */

public class CPUCycle implements CodeGenerator {

  @Override
  public Set<String> getStateVariablesRead() {
    return new HashSet<>(Arrays.asList(
        "CPU_A", "CPU_X", "CPU_Y", "CPU_PC", "CPU_SP", "CPU_P", 
        "CPU_CalcAddr", "CPU_TmpAddr", "CPU_BranchOffset",
        "CPU_DataIn", "CPU_ResetSequence", "CPU_State"));
  }

  @Override
  public Set<String> getStateVariablesWritten() {
    return new HashSet<>(Arrays.asList(
        "CPU_A", "CPU_X", "CPU_Y", "CPU_PC", "CPU_SP", "CPU_P", 
        "CPU_CalcAddr", "CPU_TmpAddr", "CPU_BranchOffset",
        "CPU_AddressBus", "CPU_WriteEnable", "CPU_DataOut", "CPU_ResetSequence", "CPU_State"));
  }

  private Symbol A_current;
  private Symbol X_current;
  private Symbol Y_current;
  private Symbol SP_current;
  private Symbol P_current;
  private Symbol PC_current;
  private Symbol CalcAddr_current;
  private Symbol TmpAddr_current;
  private Symbol BranchOffset_current;
  private Symbol AddressBus_current;
  private Symbol WriteEnable_current;
  private Symbol DataIn_current;
  private Symbol ResetSequence_current;
  private Symbol State_current;
  
  private Symbol A_next;
  private Symbol X_next;
  private Symbol Y_next;
  private Symbol SP_next;
  private Symbol P_next;
  private Symbol PC_next;
  private Symbol CalcAddr_next;
  private Symbol TmpAddr_next;
  private Symbol BranchOffset_next;
  private Symbol AddressBus_next;
  private Symbol WriteEnable_next;
  private Symbol DataOut_next;
  private Symbol ResetSequence_next;
  private Symbol State_next;

  // helpers for subexpressions
  private Assertion preserveA() {
    return new Assertion(new EqualsExpression(A_current, A_next));
  }
  
  private Assertion preserveX() {
    return new Assertion(new EqualsExpression(X_current, X_next));
  }
  
  private Assertion preserveY() {
    return new Assertion(new EqualsExpression(Y_current, Y_next));
  }
  
  private Assertion preserveSP() {
    return new Assertion(new EqualsExpression(SP_current, SP_next));
  }
  
  private Assertion preserveP() {
    return new Assertion(new EqualsExpression(P_current, P_next));
  }
  
  private Assertion preservePC() {
    return new Assertion(new EqualsExpression(PC_current, PC_next));
  }
  
  @Override
  public List<SExpression> generateCode(Map<String, Symbol> inputs,
      Map<String, Symbol> outputs) {
    List<SExpression> exprs = new LinkedList<>();
    
    // read input variables
    A_current = inputs.get("CPU_A");
    X_current = inputs.get("CPU_X");
    Y_current = inputs.get("CPU_Y");
    SP_current = inputs.get("CPU_SP");
    P_current = inputs.get("CPU_P");
    PC_current = inputs.get("CPU_PC");
    CalcAddr_current = inputs.get("CPU_CalcAddr");
    TmpAddr_current = inputs.get("CPU_TmpAddr");
    BranchOffset_current = inputs.get("CPU_BranchOffset");
    AddressBus_current = inputs.get("CPU_AddressBus");
    WriteEnable_current = inputs.get("CPU_WriteEnable");
    DataIn_current = inputs.get("CPU_DataIn");
    ResetSequence_current = inputs.get("CPU_ResetSequence");
    State_current = inputs.get("CPU_State");
    
    // declare output variables
    A_next = outputs.get("CPU_A");
    exprs.add(new BitVectorDeclaration(A_next, new Numeral("8")));
    
    X_next = outputs.get("CPU_X");
    exprs.add(new BitVectorDeclaration(X_next, new Numeral("8")));
    
    Y_next = outputs.get("CPU_Y");
    exprs.add(new BitVectorDeclaration(Y_next, new Numeral("8")));
    
    SP_next = outputs.get("CPU_SP");
    exprs.add(new BitVectorDeclaration(SP_next, new Numeral("8")));
    
    P_next = outputs.get("CPU_P");
    exprs.add(new BitVectorDeclaration(P_next, new Numeral("8")));
    
    PC_next = outputs.get("CPU_PC");
    exprs.add(new BitVectorDeclaration(PC_next, new Numeral("16")));
    
    CalcAddr_next = outputs.get("CPU_CalcAddr");
    exprs.add(new BitVectorDeclaration(CalcAddr_next, new Numeral("16")));
    
    TmpAddr_next = outputs.get("CPU_TmpAddr");
    exprs.add(new BitVectorDeclaration(TmpAddr_next, new Numeral("16")));
    
    BranchOffset_next = outputs.get("CPU_BranchOffset");
    exprs.add(new BitVectorDeclaration(BranchOffset_next, new Numeral("8")));

    AddressBus_next = outputs.get("CPU_AddressBus");
    exprs.add(new BitVectorDeclaration(AddressBus_next, new Numeral("16")));
    
    WriteEnable_next = outputs.get("CPU_WriteEnable");
    exprs.add(new BitVectorDeclaration(WriteEnable_next, new Numeral("1")));
    
    DataOut_next = outputs.get("CPU_DataOut");
    exprs.add(new BitVectorDeclaration(DataOut_next, new Numeral("8")));

    ResetSequence_next = outputs.get("CPU_ResetSequence");
    exprs.add(new BitVectorDeclaration(ResetSequence_next, new Numeral("3")));
    
    State_next = outputs.get("CPU_State");
    exprs.add(new BitVectorDeclaration(State_next, new Numeral(Integer.toString(CPUState.getStateWidth()))));
    
    exprs.addAll(handleReset());
    
    return exprs;
  }

  private List<SExpression> handleReset() {
    List<SExpression> exprs = new LinkedList<>();
    
    // check for things that happen when CPU_State = Resetting
    // this corresponds to CPU::Reset()
    
    // phase 0: read memory at PC
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("000"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
                new EqualsExpression(State_next, State_current),
                new EqualsExpression(AddressBus_next, PC_current),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("001"))
        ))));
    // phase 1: read memory at PC
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("001"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
                new EqualsExpression(State_next, State_current),
                new EqualsExpression(AddressBus_next, PC_current),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("010"))
        ))));
    // phase 2: read and decrement SP
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("010"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveP(), preservePC(),
                new EqualsExpression(SP_next, new BitVectorSubtractExpression(SP_current, new BinaryConstant("00000001"))),
                new EqualsExpression(State_next, State_current),
                new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000001"), SP_current)),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("011"))
        ))));
    // phase 3: read and decrement SP
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("011"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveP(), preservePC(),
                new EqualsExpression(SP_next, new BitVectorSubtractExpression(SP_current, new BinaryConstant("00000001"))),
                new EqualsExpression(State_next, State_current),
                new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000001"), SP_current)),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("100"))
        ))));
    // phase 4: read and decrement SP
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("100"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveP(), preservePC(),
                new EqualsExpression(SP_next, new BitVectorSubtractExpression(SP_current, new BinaryConstant("00000001"))),
                new EqualsExpression(State_next, State_current),
                new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000001"), SP_current)),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("101"))
        ))));
    // phase 5: set P[FI] = 1, read 0xFFFC
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("101"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveSP(), preservePC(),
                new EqualsExpression(P_next, new BitVectorOrExpression(P_current, new BinaryConstant("00000100"))),
                new EqualsExpression(State_next, State_current),
                new EqualsExpression(AddressBus_next, new HexConstant("FFFC")),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("110"))
        ))));
    // phase 6: set PC_low = DataIn, read 0xFFFD
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("110"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
                new EqualsExpression(PC_next, new BitVectorConcatExpression(
                    new BitVectorExtractExpression(PC_current, new Numeral("15"), new Numeral("8")), DataIn_current)),
                new EqualsExpression(State_next, State_current),
                new EqualsExpression(AddressBus_next, new HexConstant("FFFD")),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("111"))
        ))));
    // phase 7: set PC_high = DataIn, set up instruction fetch
    SExpression nextProgramCounter = new BitVectorConcatExpression(
        DataIn_current,
        new BitVectorExtractExpression(PC_current, new Numeral("7"), new Numeral("0")));
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("110"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
                new EqualsExpression(PC_next, nextProgramCounter),
                new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant()),
                new EqualsExpression(AddressBus_next, nextProgramCounter),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("000"))
        ))));
    
    return exprs;
  }
  
}
