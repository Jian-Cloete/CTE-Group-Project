package minicompiler;

import java.util.*;

/**
 * Target Machine Code (TMC)
 * Converts optimized assembly instructions into binary machine code.
 * 
 * Instruction Set Architecture (8-bit opcodes):
 * - 00000001: LOAD  (load accumulator from memory)
 * - 00000010: STORE (store accumulator to memory)
 * - 00000011: ADD   (add memory to accumulator)
 * - 00000100: SUB   (subtract memory from accumulator)
 * - 00000101: MUL   (multiply accumulator by memory)
 * - 00000110: DIV   (divide accumulator by memory)
 * - 00000111: HALT  (halt execution)
 * 
 * Binary format (16-bit instructions):
 *   [8-bit opcode][8-bit operand address]
 * 
 * Variable mapping (operand addresses):
 *   A=00000001, B=00000010, C=00000011, E=00000100, 
 *   G=00000101, H=00000110, I=00000111, M=00001000,
 *   N=00001001, a=00001010, c=00001011, t1=00001100,
 *   t2=00001101, t3=00001110, t4=00001111
 * 
 * @author Prince-Lee Shigwedha
 * @version 1.0
 */
public class TargetCode {

    // Opcode definitions
    private static final Map<String, String> OPCODES = new HashMap<>();
    static {
        OPCODES.put("LOAD",  "00000001");
        OPCODES.put("STORE", "00000010");
        OPCODES.put("ADD",   "00000011");
        OPCODES.put("SUB",   "00000100");
        OPCODES.put("MUL",   "00000101");
        OPCODES.put("DIV",   "00000110");
        OPCODES.put("HALT",  "00000111");
    }

    // Variable to binary address mapping
    private static final Map<String, String> VARIABLE_ADDRESSES = new HashMap<>();
    static {
        VARIABLE_ADDRESSES.put("A", "00000001");
        VARIABLE_ADDRESSES.put("B", "00000010");
        VARIABLE_ADDRESSES.put("C", "00000011");
        VARIABLE_ADDRESSES.put("E", "00000100");
        VARIABLE_ADDRESSES.put("G", "00000101");
        VARIABLE_ADDRESSES.put("H", "00000110");
        VARIABLE_ADDRESSES.put("I", "00000111");
        VARIABLE_ADDRESSES.put("M", "00001000");
        VARIABLE_ADDRESSES.put("N", "00001001");
        VARIABLE_ADDRESSES.put("a", "00001010");
        VARIABLE_ADDRESSES.put("c", "00001011");
        VARIABLE_ADDRESSES.put("t1", "00001100");
        VARIABLE_ADDRESSES.put("t2", "00001101");
        VARIABLE_ADDRESSES.put("t3", "00001110");
        VARIABLE_ADDRESSES.put("t4", "00001111");
    }

    /**
     * One binary machine instruction
     */
    public static class BinaryInstruction {
        public final String opcode;
        public final String operandAddress;
        public final String binary;  // Full 16-bit instruction
        public final String operation;
        public final String operand;

        public BinaryInstruction(String opcode, String operandAddress, String operation, String operand) {
            this.opcode = opcode;
            this.operandAddress = operandAddress;
            this.operation = operation;
            this.operand = operand;
            this.binary = opcode + operandAddress;
        }

        @Override
        public String toString() {
            return String.format("  %s  %s  |  %s %s", binary, operation, operand);
        }
    }

    /**
     * Converts assembly instructions to binary machine code.
     * 
     * @param instructions list of AssemblyInstruction from CodeOptimizer
     * @return list of BinaryInstruction
     */
    public List<BinaryInstruction> generate(List<CodeGenerator.AssemblyInstruction> instructions) {
        List<BinaryInstruction> binary = new ArrayList<>();

        if (instructions == null || instructions.isEmpty()) {
            return binary;
        }

        for (CodeGenerator.AssemblyInstruction instr : instructions) {
            BinaryInstruction binInstr = convertToBinary(instr);
            if (binInstr != null) {
                binary.add(binInstr);
            }
        }

        // Add HALT instruction at the end
        binary.add(new BinaryInstruction(
            OPCODES.get("HALT"),
            "00000000",
            "HALT",
            ""
        ));

        return binary;
    }

    /**
     * Convert a single assembly instruction to binary.
     */
    private BinaryInstruction convertToBinary(CodeGenerator.AssemblyInstruction instr) {
        String opcode = OPCODES.get(instr.operation);
        if (opcode == null) {
            return null;  // Unknown operation
        }

        String operandAddr = "00000000";  // Default: no operand
        if (instr.operand != null && !instr.operand.isEmpty()) {
            operandAddr = VARIABLE_ADDRESSES.get(instr.operand);
            if (operandAddr == null) {
                operandAddr = "00000000";  // Unknown variable
            }
        }

        return new BinaryInstruction(opcode, operandAddr, instr.operation, instr.operand);
    }

    /**
     * Prints the binary machine code with a header.
     */
    public void printBinary(List<BinaryInstruction> instructions, int lineNo) {
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("  TARGET MACHINE CODE (BINARY) — Line %d%n", lineNo);
        System.out.println("=".repeat(60));
        System.out.printf("  %-16s  %-8s  |  %s%n", "BINARY", "OPCODE", "INSTRUCTION");
        System.out.println("=".repeat(60));

        if (instructions == null || instructions.isEmpty()) {
            System.out.println("  (no instructions generated)");
        } else {
            for (BinaryInstruction instr : instructions) {
                System.out.println(instr);
            }
        }
        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * Get total binary instruction count.
     */
    public int getInstructionCount(List<BinaryInstruction> instructions) {
        return instructions != null ? instructions.size() : 0;
    }

    /**
     * Get opcode for display purposes.
     */
    public String getOpcode(String operation) {
        return OPCODES.get(operation);
    }

    /**
     * Get variable address for display purposes.
     */
    public String getVariableAddress(String variable) {
        return VARIABLE_ADDRESSES.get(variable);
    }
}

