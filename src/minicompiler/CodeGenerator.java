package minicompiler;

import java.util.*;

/**
 * Code Generator (CG)
 * Converts Intermediate Code Representation (TAC) into assembly-like instructions.
 * 
 * Supported instructions:
 * - LOAD X    → Load value from variable X into accumulator
 * - STORE X  → Store accumulator value into variable X
 * - ADD X    → Add value from variable X to accumulator
 * - SUB X    → Subtract value from variable X from accumulator
 * - MUL X    → Multiply accumulator by value from variable X
 * - DIV X    → Divide accumulator by value from variable X
 * 
 * Example transformations:
 *   t1 = A / B    →  LOAD A
 *                    DIV B
 *                    STORE t1
 *   
 *   M = t1 + C    →  LOAD t1
 *                    ADD C
 *                    STORE M
 *
 * @author Prince-Lee Shigwedha
 * @version 1.0
 */
public class CodeGenerator {

    /** One assembly-like instruction */
    public static class AssemblyInstruction {
        public final String operation;
        public final String operand;
        public final int icrLineNumber;  // Reference to original TAC instruction

        public AssemblyInstruction(String operation, String operand, int icrLineNumber) {
            this.operation = operation;
            this.operand = operand;
            this.icrLineNumber = icrLineNumber;
        }

        @Override
        public String toString() {
            if (operand == null || operand.isEmpty()) {
                return String.format("  %-6s", operation);
            }
            return String.format("  %-6s %s", operation, operand);
        }
    }

    /**
     * Generates assembly-like instructions from TAC instructions.
     * 
     * @param tacInstructions list of TACInstruction from IntermediateCode
     * @return list of AssemblyInstruction
     */
    public List<AssemblyInstruction> generate(List<IntermediateCode.TACInstruction> tacInstructions) {
        List<AssemblyInstruction> assembly = new ArrayList<>();

        if (tacInstructions == null || tacInstructions.isEmpty()) {
            return assembly;
        }

        int icrLineNum = 1;
        for (IntermediateCode.TACInstruction tac : tacInstructions) {
            List<AssemblyInstruction> converted = convertTACToAssembly(tac, icrLineNum);
            assembly.addAll(converted);
            icrLineNum++;
        }

        return assembly;
    }

    /**
     * Convert a single TAC instruction to assembly instructions.
     * Uses accumulator-based architecture.
     */
    private List<AssemblyInstruction> convertTACToAssembly(IntermediateCode.TACInstruction tac, int icrLineNum) {
        List<AssemblyInstruction> instructions = new ArrayList<>();
        String op = tac.operator;

        // Handle simple copy (result = operand1 with operator "=")
        if (op.equals("=") && (tac.operand2 == null || tac.operand2.isEmpty())) {
            instructions.add(new AssemblyInstruction("LOAD", tac.operand1, icrLineNum));
            instructions.add(new AssemblyInstruction("STORE", tac.result, icrLineNum));
            return instructions;
        }

        // Handle binary operations
        // Step 1: Load first operand into accumulator
        instructions.add(new AssemblyInstruction("LOAD", tac.operand1, icrLineNum));

        // Step 2: Perform operation with second operand
        switch (op) {
            case "+":
                instructions.add(new AssemblyInstruction("ADD", tac.operand2, icrLineNum));
                break;
            case "-":
                instructions.add(new AssemblyInstruction("SUB", tac.operand2, icrLineNum));
                break;
            case "*":
                instructions.add(new AssemblyInstruction("MUL", tac.operand2, icrLineNum));
                break;
            case "/":
                instructions.add(new AssemblyInstruction("DIV", tac.operand2, icrLineNum));
                break;
            default:
                // Unknown operator - skip
                break;
        }

        // Step 3: Store result
        instructions.add(new AssemblyInstruction("STORE", tac.result, icrLineNum));

        return instructions;
    }

    /**
     * Prints the assembly instructions with a header.
     */
    public void printAssembly(List<AssemblyInstruction> instructions, int lineNo) {
        System.out.println("\n" + "=".repeat(50));
        System.out.printf("  CODE GENERATION — Line %d%n", lineNo);
        System.out.println("=".repeat(50));

        if (instructions == null || instructions.isEmpty()) {
            System.out.println("  (no instructions generated)");
        } else {
            for (AssemblyInstruction instr : instructions) {
                System.out.println(instr);
            }
        }
        System.out.println("=".repeat(50) + "\n");
    }

    /**
     * Get the total number of assembly instructions.
     */
    public int getInstructionCount(List<AssemblyInstruction> instructions) {
        return instructions != null ? instructions.size() : 0;
    }
}

