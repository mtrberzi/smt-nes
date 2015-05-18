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
  private EqualsExpression preserveA() {
    return new EqualsExpression(A_current, A_next);
  }
  
  private EqualsExpression preserveX() {
    return new EqualsExpression(X_current, X_next);
  }
  
  private EqualsExpression preserveY() {
    return new EqualsExpression(Y_current, Y_next);
  }
  
  private EqualsExpression preserveSP() {
    return new EqualsExpression(SP_current, SP_next);
  }
  
  private EqualsExpression preserveP() {
    return new EqualsExpression(P_current, P_next);
  }
  
  private EqualsExpression preservePC() {
    return new EqualsExpression(PC_current, PC_next);
  }
  
  // Set up a read from the address pointed to by the current program counter.
  private AndExpression fetchPC() {
    return new AndExpression(
        new EqualsExpression(AddressBus_next, PC_current),
        new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
        new EqualsExpression(DataOut_next, new BinaryConstant("00000000"))
        );
  }
  
  private EqualsExpression incrementPC() {
    return new EqualsExpression(PC_next, new BitVectorAddExpression(PC_current, new BinaryConstant("0000000000000001")));
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
    exprs.addAll(instruction_LDA());
    exprs.addAll(instruction_LDX());
    exprs.addAll(instruction_STA());
    
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
    // note that since we simulate an instruction fetch here, we also increment PC
    SExpression nextProgramCounter = new BitVectorConcatExpression(
        DataIn_current,
        new BitVectorExtractExpression(PC_current, new Numeral("7"), new Numeral("0")));
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.Resetting.toBinaryConstant()),
            new EqualsExpression(ResetSequence_current, new BinaryConstant("111"))),
            new AndExpression(
                preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
                new EqualsExpression(PC_next, new BitVectorAddExpression(nextProgramCounter, new BinaryConstant("0000000000000001"))),
                new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant()),
                new EqualsExpression(AddressBus_next, nextProgramCounter),
                new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                new EqualsExpression(ResetSequence_next, new BinaryConstant("000"))
        ))));
    
    return exprs;
  }
  
  private List<SExpression> instruction_LDA() {
    List<SExpression> exprs = new LinkedList<>();
    
    // opcodes that use LDA:
    // A1 (INX), B1 (INY), A9 (IMM), B9 (ABY), A5 (ZPG), B5 (ZPX), AD (ABS), BD (ABX)
    
    // operation of LDA:
    // * A = MemGet(CalcAddr)
    // * P[Z] = (A == 0)
    // * P[N] = (A[7] == 1)
    
    // opcode A9: LDA immediate
    // cycle 0: read [PC], increment PC
    // cycle 1: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("A9"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_IMM_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_IMM_Cycle1.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode A5: LDA zeropage
    // cycle 0: read [PC], increment PC
    // cycle 1: read [$0000 | DataIn] (zero-page read)
    // cycle 2: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("A5"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ZPG_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(new EqualsExpression(State_current, CPUState.LDA_ZPG_Cycle1.toBinaryConstant()),
					    new AndExpression(preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
							      new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
							      new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
							      new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
							      new EqualsExpression(State_next, CPUState.LDA_ZPG_Cycle2.toBinaryConstant())))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ZPG_Cycle2.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));

    // opcode B5: LDA zeropage,x
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [CalcAddr]
    // cycle 2: ignore DataIn, CalcAddr[7:0] += X (discarding overflow), read [CalcAddr]
    // cycle 3: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("B5"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ZPX_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ZPX_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ZPX_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ZPX_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                new BitVectorAddExpression(X_current, new BitVectorExtractExpression(
                    CalcAddr_current, new Numeral("7"), new Numeral("0"))))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ZPX_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ZPX_Cycle3.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode AD: LDA absolute
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [PC], increment PC
    // cycle 2: CalcAddr[15:8] = DataIn, read [CalcAddr]
    // cycle 3: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("AD"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ABS_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABS_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ABS_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABS_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(DataIn_current, new BitVectorExtractExpression(
                CalcAddr_current, new Numeral("7"), new Numeral("0")))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ABS_Cycle3.toBinaryConstant())
        ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABS_Cycle3.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode BD: LDA absolute,x
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [PC], increment PC
    // cycle 2: CalcAddr = (DataIn ++ CalcAddr[7:0]) + (0x00 ++ X)
    // if (CalcAddr[7:0] + X >= 0x100) then read [DataIn ++ (CalcAddr[7:0] + X)] (CalcAddr without carry into high byte) and do cycle 2x; 
    // otherwise read [CalcAddr] and do cycle 3
    // cycle 2x: discard DataIn, read [CalcAddr]
    // cycle 3: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("BD"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ABX_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABX_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ABX_Cycle2.toBinaryConstant())
            ))));
    SExpression LDA_ABX_CalcAddr_Cycle2 = new BitVectorAddExpression(
        new BitVectorConcatExpression(DataIn_current, new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
        new BitVectorConcatExpression(new BinaryConstant("00000000"), X_current)
        );
    SExpression LDA_ABX_Overflow_Cycle2 = new BitVectorUnsignedGreaterEqualExpression(
        new BitVectorAddExpression(
            new BitVectorConcatExpression(new BinaryConstant("0"), new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
            new BitVectorConcatExpression(new BinaryConstant("0"), X_current)),
        new BinaryConstant("100000000")
        );
    // this happens in cycle 2 no matter what
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABX_Cycle2.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, LDA_ABX_CalcAddr_Cycle2)
            ))));
    // now the conditional for overflow
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDA_ABX_Cycle2.toBinaryConstant()),
        LDA_ABX_Overflow_Cycle2
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(DataIn_current, new BitVectorAddExpression(
                new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0")), X_current))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ABX_Cycle2x.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDA_ABX_Cycle2.toBinaryConstant()),
        new NotExpression(LDA_ABX_Overflow_Cycle2)
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, LDA_ABX_CalcAddr_Cycle2),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ABX_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABX_Cycle2x.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, CalcAddr_current),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ABX_Cycle3.toBinaryConstant())
        ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABX_Cycle3.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode B9: LDA absolute,y
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [PC], increment PC
    // cycle 2: CalcAddr = (DataIn ++ CalcAddr[7:0]) + (0x00 ++ Y)
    // if (CalcAddr[7:0] + Y >= 0x100) then read [DataIn ++ (CalcAddr[7:0] + Y)] (CalcAddr without carry into high byte) and do cycle 2x; 
    // otherwise read [CalcAddr] and do cycle 3
    // cycle 2x: discard DataIn, read [CalcAddr]
    // cycle 3: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("B9"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ABY_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABY_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_ABY_Cycle2.toBinaryConstant())
            ))));
    SExpression LDA_ABY_CalcAddr_Cycle2 = new BitVectorAddExpression(
        new BitVectorConcatExpression(DataIn_current, new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
        new BitVectorConcatExpression(new BinaryConstant("00000000"), Y_current)
        );
    SExpression LDA_ABY_Overflow_Cycle2 = new BitVectorUnsignedGreaterEqualExpression(
        new BitVectorAddExpression(
            new BitVectorConcatExpression(new BinaryConstant("0"), new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
            new BitVectorConcatExpression(new BinaryConstant("0"), Y_current)),
        new BinaryConstant("100000000")
        );
    // this happens in cycle 2 no matter what
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABY_Cycle2.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, LDA_ABY_CalcAddr_Cycle2)
            ))));
    // now the conditional for overflow
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDA_ABY_Cycle2.toBinaryConstant()),
        LDA_ABY_Overflow_Cycle2
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(DataIn_current, new BitVectorAddExpression(
                new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0")), Y_current))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ABY_Cycle2x.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDA_ABY_Cycle2.toBinaryConstant()),
        new NotExpression(LDA_ABY_Overflow_Cycle2)
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, LDA_ABY_CalcAddr_Cycle2),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ABY_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABY_Cycle2x.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, CalcAddr_current),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_ABY_Cycle3.toBinaryConstant())
        ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_ABY_Cycle3.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode A1: LDA indirect,x
    // cycle 0: read [PC], increment PC
    // cycle 1: TmpAddr = $00 ++ DataIn, read [$00 ++ DataIn]
    // cycle 2: discard DataIn, TmpAddr[7:0] += X (no overflow), read [TmpAddr_next]
    // cycle 3: CalcAddr[7:0] = DataIn, TmpAddr[7:0] += 1 (no overflow), read [TmpAddr_next]
    // cycle 4: CalcAddr[15:8] = DataIn, read [CalcAddr]
    // cycle 5: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("A1"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_INX_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INX_Cycle1.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(TmpAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INX_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INX_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(TmpAddr_next, 
                new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                    new BitVectorAddExpression(
                        new BitVectorExtractExpression(TmpAddr_current, new Numeral("7"), new Numeral("0")),
                        X_current))
            ),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                    new BitVectorAddExpression(
                        new BitVectorExtractExpression(TmpAddr_current, new Numeral("7"), new Numeral("0")),
                        X_current))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INX_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INX_Cycle3.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(TmpAddr_next, 
                new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                    new BitVectorAddExpression(
                        new BitVectorExtractExpression(TmpAddr_current, new Numeral("7"), new Numeral("0")),
                        new BinaryConstant("00000001")))
            ),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                    new BitVectorAddExpression(
                        new BitVectorExtractExpression(TmpAddr_current, new Numeral("7"), new Numeral("0")),
                        new BinaryConstant("00000001")))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INX_Cycle4.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INX_Cycle4.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(
                DataIn_current, new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0")))),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(
                DataIn_current, new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0")))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INX_Cycle5.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INX_Cycle5.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode B1: LDA indirect,y
    // cycle 0: read [PC], increment PC
    // cycle 1: TmpAddr = $00 ++ DataIn, read [TmpAddr]
    // cycle 2: CalcAddr[7:0] = DataIn, read [TmpAddr+1] (discarding overflow)
    // cycle 3: CalcAddr[15:8] = DataIn, CalcAddr[7:0] += Y, read [CalcAddr_next]
    // if (CalcAddr[7:0] + Y >= 0x100) then go to cycle 3x;  go to cycle 4
    // cycle 3x: discard DataIn, read [CalcAddr]
    // cycle 4: set A, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("B1"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDA_INY_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INY_Cycle1.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(TmpAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INY_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INY_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                new BitVectorAddExpression(
                    new BitVectorExtractExpression(TmpAddr_current, new Numeral("7"), new Numeral("0")), 
                    new BinaryConstant("00000001")))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INY_Cycle3.toBinaryConstant())
            ))));
    SExpression LDA_INY_CalcAddr_Cycle3 = new BitVectorAddExpression(
        new BitVectorConcatExpression(DataIn_current, new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
        new BitVectorConcatExpression(new BinaryConstant("00000000"), Y_current)
        );
    SExpression LDA_INY_Overflow_Cycle3 = new BitVectorUnsignedGreaterEqualExpression(
        new BitVectorAddExpression(
            new BitVectorConcatExpression(new BinaryConstant("0"), new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
            new BitVectorConcatExpression(new BinaryConstant("0"), Y_current)),
        new BinaryConstant("100000000")
        );
    // this happens in cycle 3 no matter what
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INY_Cycle3.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, LDA_INY_CalcAddr_Cycle3)
            ))));
    // now the conditional for overflow
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDA_INY_Cycle3.toBinaryConstant()),
        LDA_INY_Overflow_Cycle3
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(DataIn_current, new BitVectorAddExpression(
                new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0")), Y_current))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INY_Cycle3x.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDA_INY_Cycle3.toBinaryConstant()),
        new NotExpression(LDA_INY_Overflow_Cycle3)
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, LDA_INY_CalcAddr_Cycle3),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INY_Cycle4.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INY_Cycle3x.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, CalcAddr_current),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDA_INY_Cycle4.toBinaryConstant())
        ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDA_INY_Cycle4.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(A_next, DataIn_current),
            preserveX(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    return exprs;
  }
 
  private List<SExpression> instruction_LDX() {
    List<SExpression> exprs = new LinkedList<>();
    
    // opcodes that use LDX:
    // A2 (IMM), A6 (ZPG), B6 (ZPY), AE (ABS), BE (ABY)
    
    // operation of LDX:
    // * X = MemGet(CalcAddr)
    // * P[Z] = (X == 0)
    // * P[N] = (X[7] == 1)
    
    // opcode A2: LDX immediate
    // cycle 0: read [PC], increment PC
    // cycle 1: set X, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("A2"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDX_IMM_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_IMM_Cycle1.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(X_next, DataIn_current),
            preserveA(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode A6: LDX zeropage
    // cycle 0: read [PC], increment PC
    // cycle 1: read [$0000 | DataIn] (zero-page read)
    // cycle 2: set X, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("A6"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDX_ZPG_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(new EqualsExpression(State_current, CPUState.LDX_ZPG_Cycle1.toBinaryConstant()),
              new AndExpression(preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
                    new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
                    new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
                    new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
                    new EqualsExpression(State_next, CPUState.LDX_ZPG_Cycle2.toBinaryConstant())))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ZPG_Cycle2.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(X_next, DataIn_current),
            preserveA(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode B6: LDX zeropage,y
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [CalcAddr]
    // cycle 2: ignore DataIn, CalcAddr[7:0] += Y (discarding overflow), read [CalcAddr]
    // cycle 3: set X, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("B6"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDX_ZPY_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ZPY_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDX_ZPY_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ZPY_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                new BitVectorAddExpression(Y_current, new BitVectorExtractExpression(
                    CalcAddr_current, new Numeral("7"), new Numeral("0"))))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDX_ZPY_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ZPY_Cycle3.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(X_next, DataIn_current),
            preserveA(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode AE: LDX absolute
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [PC], increment PC
    // cycle 2: CalcAddr[15:8] = DataIn, read [CalcAddr]
    // cycle 3: set X, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("AE"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDX_ABS_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ABS_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDX_ABS_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ABS_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(DataIn_current, new BitVectorExtractExpression(
                CalcAddr_current, new Numeral("7"), new Numeral("0")))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDX_ABS_Cycle3.toBinaryConstant())
        ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ABS_Cycle3.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(X_next, DataIn_current),
            preserveA(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode BE: LDX absolute,y
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [PC], increment PC
    // cycle 2: CalcAddr = (DataIn ++ CalcAddr[7:0]) + (0x00 ++ Y)
    // if (CalcAddr[7:0] + Y >= 0x100) then read [DataIn ++ (CalcAddr[7:0] + Y)] (CalcAddr without carry into high byte) and do cycle 2x; 
    // otherwise read [CalcAddr] and do cycle 3
    // cycle 2x: discard DataIn, read [CalcAddr]
    // cycle 3: set X, set P, instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("BE"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDX_ABY_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ABY_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.LDX_ABY_Cycle2.toBinaryConstant())
            ))));
    SExpression LDA_ABY_CalcAddr_Cycle2 = new BitVectorAddExpression(
        new BitVectorConcatExpression(DataIn_current, new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
        new BitVectorConcatExpression(new BinaryConstant("00000000"), Y_current)
        );
    SExpression LDA_ABY_Overflow_Cycle2 = new BitVectorUnsignedGreaterEqualExpression(
        new BitVectorAddExpression(
            new BitVectorConcatExpression(new BinaryConstant("0"), new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
            new BitVectorConcatExpression(new BinaryConstant("0"), Y_current)),
        new BinaryConstant("100000000")
        );
    // this happens in cycle 2 no matter what
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ABY_Cycle2.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, LDA_ABY_CalcAddr_Cycle2)
            ))));
    // now the conditional for overflow
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDX_ABY_Cycle2.toBinaryConstant()),
        LDA_ABY_Overflow_Cycle2
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(DataIn_current, new BitVectorAddExpression(
                new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0")), Y_current))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDX_ABY_Cycle2x.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(new AndExpression(
        new EqualsExpression(State_current, CPUState.LDX_ABY_Cycle2.toBinaryConstant()),
        new NotExpression(LDA_ABY_Overflow_Cycle2)
        ), new AndExpression(
            new EqualsExpression(AddressBus_next, LDA_ABY_CalcAddr_Cycle2),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDX_ABY_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ABY_Cycle2x.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, CalcAddr_current),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.LDX_ABY_Cycle3.toBinaryConstant())
        ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.LDX_ABY_Cycle3.toBinaryConstant()),
        new AndExpression(
            new EqualsExpression(X_next, DataIn_current),
            preserveA(), preserveY(), preserveSP(), 
            new EqualsExpression(P_next, new BitVectorConcatExpression(
                new BitVectorConcatExpression(
                // 7 P[Z]
                    new ConditionalExpression(
                        new EqualsExpression(DataIn_current, new BinaryConstant("00000000")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 6 downto 2
                    new BitVectorExtractExpression(P_current, new Numeral("6"), new Numeral("2"))
                    ),
                new BitVectorConcatExpression(
                // 1 P[N]
                    new ConditionalExpression(
                        new EqualsExpression(new BitVectorExtractExpression(DataIn_current, new Numeral("7"), new Numeral("7")), 
                            new BinaryConstant("1")), 
                        new BinaryConstant("1"), new BinaryConstant("0")),
                // 0
                    new BitVectorExtractExpression(P_current, new Numeral("0"), new Numeral("0"))
                    )
                )),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    return exprs;
  }
  
  private List<SExpression> instruction_STA() {
    List<SExpression> exprs = new LinkedList<>();
    
    // opcodes that use STA:
    // 81 (INX), 91 (INYW), 99 (ABYW), 85 (ZPG), 95 (ZPX), 8D (ABS), 9D (ABXW)
    
    // operation of STA:
    // * MemSet(CalcAddr, A)
    
    // opcode 85: STA zeropage
    // cycle 0: read [PC], increment PC
    // cycle 1: write A to [$00 ++ PC] 
    // cycle 2: instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("85"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.STA_ZPG_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(
        new Implication(new EqualsExpression(State_current, CPUState.STA_ZPG_Cycle1.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("1")),
            new EqualsExpression(DataOut_next, A_current),
            new EqualsExpression(State_next, CPUState.STA_ZPG_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ZPG_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode 95: STA zeropage,x
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr = $00 ++ DataIn, read [CalcAddr]
    // cycle 2: ignore DataIn, CalcAddr[7:0] += X, write A to [CalcAddr]
    // cycle 3: instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("95"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.STA_ZPX_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ZPX_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.STA_ZPX_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ZPX_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), 
                new BitVectorAddExpression(X_current, new BitVectorExtractExpression(
                    CalcAddr_current, new Numeral("7"), new Numeral("0"))))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("1")),
            new EqualsExpression(DataOut_next, A_current),
            new EqualsExpression(State_next, CPUState.STA_ZPX_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ZPX_Cycle3.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode 8D: STA absolute
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [PC], increment PC
    // cycle 2: CalcAddr[15:8] = DataIn, write A to [CalcAddr]
    // cycle 3: instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("8D"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.STA_ABS_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ABS_Cycle1.toBinaryConstant()), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(CalcAddr_next, new BitVectorConcatExpression(new BinaryConstant("00000000"), DataIn_current)),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.STA_ABS_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ABS_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(DataIn_current, new BitVectorExtractExpression(
                CalcAddr_current, new Numeral("7"), new Numeral("0")))),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("1")),
            new EqualsExpression(DataOut_next, A_current),
            new EqualsExpression(State_next, CPUState.STA_ABS_Cycle3.toBinaryConstant())
        ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ABS_Cycle3.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode 9D: STA absolute,x(w)
    // cycle 0: read [PC], increment PC
    // cycle 1: CalcAddr[7:0] = DataIn, read [PC], increment PC
    // cycle 2: read [DataIn ++ (CalcAddr[7:0] + X)], CalcAddr = (DataIn ++ CalcAddr[7:0]) + X
    // cycle 3: write A to [CalcAddr]
    // cycle 4: instruction fetch
    exprs.add(new Assertion(new Implication(
        new AndExpression(new EqualsExpression(State_current, CPUState.InstructionFetch.toBinaryConstant()),
            new EqualsExpression(DataIn_current, new HexConstant("9D"))), 
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.STA_ABX_Cycle1.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ABX_Cycle1.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            new EqualsExpression(new BitVectorExtractExpression(
                CalcAddr_next, new Numeral("7"), new Numeral("0")), DataIn_current),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.STA_ABX_Cycle2.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ABX_Cycle2.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, new BitVectorConcatExpression(
                DataIn_current, new BitVectorAddExpression(
                    new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0")), 
                    X_current)
                )),
            new EqualsExpression(CalcAddr_next, new BitVectorAddExpression(
                new BitVectorConcatExpression(
                    DataIn_current, 
                    new BitVectorExtractExpression(CalcAddr_current, new Numeral("7"), new Numeral("0"))),
                new BitVectorConcatExpression(new BinaryConstant("00000000"), X_current)
                )),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("0")),
            new EqualsExpression(DataOut_next, new BinaryConstant("00000000")),
            new EqualsExpression(State_next, CPUState.STA_ABX_Cycle3.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ABX_Cycle3.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(), preservePC(),
            new EqualsExpression(AddressBus_next, CalcAddr_current),
            new EqualsExpression(WriteEnable_next, new BinaryConstant("1")),
            new EqualsExpression(DataOut_next, A_current),
            new EqualsExpression(State_next, CPUState.STA_ABX_Cycle4.toBinaryConstant())
            ))));
    exprs.add(new Assertion(new Implication(
        new EqualsExpression(State_current, CPUState.STA_ABX_Cycle4.toBinaryConstant()),
        new AndExpression(
            preserveA(), preserveX(), preserveY(), preserveSP(), preserveP(),
            fetchPC(), incrementPC(),
            new EqualsExpression(State_next, CPUState.InstructionFetch.toBinaryConstant())
            ))));
    
    // opcode 99: STA absolute,y(w)
    // opcode 81: STA indirect,x
    // opcode 91: STA indirect,y(w)
    
    return exprs;
  }
  
}
