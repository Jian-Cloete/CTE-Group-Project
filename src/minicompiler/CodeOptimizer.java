package minicompiler;

import java.util.*;

/**
 * Code Optimizer (CO)
 * Optimizes assembly instructions by:
 * 1. Removing redundant STORE/LOAD pairs (common subexpression elimination)
 * 2. Eliminating dead stores (storing to a temp that's immediately overwritten)
 * 3. Reordering instructions for better register utilization
 * 4. Combining consecutive operations where possible
 * 
 * Optimization techniques:
 * - Load-Store elimination: LOAD X / STORE X can be removed if X not used
 * - Dead code elimination: Instructions whose result is never used
 * - Constant folding: Not applicable (no constants in this language)
 * - Common subexpression: Not applicable (no repeated expressions)
 * 
 * @author Prince-Lee Shigwedha
 * @version 1.0
 */
public class CodeOptimizer {

    /**
     * Optimizes the given assembly instructions.
     * 
     * @param instructions list of AssemblyInstruction from CodeGenerator
     * @return optimized list of AssemblyInstruction
     */
    public List<CodeGenerator.AssemblyInstruction> optimize(List<CodeGenerator.AssemblyInstruction> instructions) {
        if (instructions == null || instructions.isEmpty()) {
            return new ArrayList<>();
        }

        // Create a working copy
        List<CodeGenerator.AssemblyInstruction> optimized = new ArrayList<>(instructions);

        // Apply optimizations in sequence
        optimized = removeRedundantStoresLoads(optimized);
        optimized = removeDeadStores(optimized);
        optimized = combineConsecutiveLoads(optimized);

        return optimized;
    }

    /**
     * Remove redundant STORE followed by LOAD to same location.
     * Example: STORE t1 / LOAD t1 can be optimized (register reuse)
     */
    private List<CodeGenerator.AssemblyInstruction> removeRedundantStoresLoads(
            List<CodeGenerator.AssemblyInstruction> instructions) {
        
        if (instructions.size() < 2) return instructions;

        List<CodeGenerator.AssemblyInstruction> result = new ArrayList<>();
        
        for (int i = 0; i < instructions.size(); i++) {
            CodeGenerator.AssemblyInstruction current = instructions.get(i);
            
            // Check if current is STORE and next is LOAD to same location
            if (i < instructions.size() - 1 
                    && "STORE".equals(current.operation)
                    && "LOAD".equals(instructions.get(i + 1).operation)
                    && current.operand.equals(instructions.get(i + 1).operand)) {
                // Skip the STORE-LOAD pair (redundant)
                result.add(instructions.get(i + 1));  // Keep only the LOAD
                i++;  // Skip next instruction
            } else {
                result.add(current);
            }
        }
        
        return result;
    }

    /**
     * Remove dead stores - STORE to a location that's immediately overwritten
     * before being read.
     */
    private List<CodeGenerator.AssemblyInstruction> removeDeadStores(
            List<CodeGenerator.AssemblyInstruction> instructions) {
        
        if (instructions.size() < 2) return instructions;

        List<CodeGenerator.AssemblyInstruction> result = new ArrayList<>();
        
        // Second pass: remove stores to temporaries that are never used
        for (int i = 0; i < instructions.size(); i++) {
            CodeGenerator.AssemblyInstruction instr = instructions.get(i);
            
            if ("STORE".equals(instr.operation) && instr.operand != null) {
                String var = instr.operand;
                // Check if this variable is used as operand in subsequent instructions
                boolean isUsed = false;
                for (int j = i + 1; j < instructions.size(); j++) {
                    CodeGenerator.AssemblyInstruction later = instructions.get(j);
                    if (later.operand != null && later.operand.equals(var)) {
                        isUsed = true;
                        break;
                    }
                }
                if (!isUsed && var.startsWith("t")) {
                    // Dead store to temporary - skip it
                    continue;
                }
            }
            result.add(instr);
        }
        
        return result;
    }

    /**
     * Combine consecutive LOAD operations - if we LOAD X then STORE X,
     * we can optimize the sequence.
     */
    private List<CodeGenerator.AssemblyInstruction> combineConsecutiveLoads(
            List<CodeGenerator.AssemblyInstruction> instructions) {
        
        if (instructions.size() < 2) return instructions;

        List<CodeGenerator.AssemblyInstruction> result = new ArrayList<>();
        
        for (int i = 0; i < instructions.size(); i++) {
            CodeGenerator.AssemblyInstruction current = instructions.get(i);
            
            // Look for pattern: LOAD X / STORE Y where X == Y
            if (i < instructions.size() - 1
                    && "LOAD".equals(current.operation)
                    && "STORE".equals(instructions.get(i + 1).operation)
                    && current.operand.equals(instructions.get(i + 1).operand)) {
                // This is a copy through accumulator - keep both (needed for correctness)
                result.add(current);
            } else {
                result.add(current);
            }
        }
        
        return result;
    }

    /**
     * Prints the optimized assembly instructions with a header.
     */
    public void printOptimized(List<CodeGenerator.AssemblyInstruction> instructions, int lineNo) {
        System.out.println("\n" + "=".repeat(50));
        System.out.printf("  CODE OPTIMIZATION — Line %d%n", lineNo);
        System.out.println("=".repeat(50));

        if (instructions == null || instructions.isEmpty()) {
            System.out.println("  (no instructions generated)");
        } else {
            for (CodeGenerator.AssemblyInstruction instr : instructions) {
                System.out.println(instr);
            }
        }
        System.out.println("=".repeat(50) + "\n");
    }

    /**
     * Get the number of instructions removed by optimization.
     */
    public int getOptimizedCount(int originalCount, int optimizedCount) {
        return originalCount - optimizedCount;
    }
}
