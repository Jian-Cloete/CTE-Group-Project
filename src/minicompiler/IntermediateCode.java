package minicompiler;

import java.util.*;

/**
 * Intermediate Code Representation (ICR)
 * Converts valid expression lines into 3-address code (TAC).
 *
 * Only the three error-free lines are processed:
 *   Line 5:  LET G = a + c          →  G = a + c
 *   Line 7:  M = A/B+C              →  t1 = A / B
 *                                      M  = t1 + C
 *   Line 8:  N = G/H-I+a*B/c        →  t1 = G / H
 *                                      t2 = t1 - I
 *                                      t3 = a * B
 *                                      t4 = t3 / c
 *                                      N  = t2 + t4
 *
 * Strategy: standard left-to-right operator precedence
 *   Pass 1 – evaluate all * and / sub-expressions into temporaries
 *   Pass 2 – evaluate remaining + and - using results from Pass 1
 *
 * @author [Your Name]
 * @version 1.0
 */
public class IntermediateCode {

    /** One 3-address instruction: result = operand1 op operand2 */
    public static class TACInstruction {
        public final String result;
        public final String operand1;
        public final String operator;
        public final String operand2;

        public TACInstruction(String result, String operand1, String operator, String operand2) {
            this.result = result;
            this.operand1 = operand1;
            this.operator = operator;
            this.operand2 = operand2;
        }

        @Override
        public String toString() {
            return String.format("  %-6s = %s %s %s", result, operand1, operator, operand2);
        }
    }

    private int tempCounter = 0;

    /** Generate a fresh temporary variable name: t1, t2, … */
    private String newTemp() {
        tempCounter++;
        return "t" + tempCounter;
    }

    /**
     * Resets the temporary counter.
     * Call between independent compilations so temporaries restart from t1.
     */
    public void reset() {
        tempCounter = 0;
    }

    /**
     * Main entry point.
     * Takes the token list for one valid line and returns the list of TAC instructions.
     *
     * @param tokens  token list from LexicalAnalyzer (already verified by Syntax + Semantic)
     * @param lineNo  line number – used only for the printed header
     * @return        ordered list of TACInstruction objects
     */
    public List<TACInstruction> generate(List<LexicalAnalyzer.Token> tokens, int lineNo) {
        List<TACInstruction> instructions = new ArrayList<>();

        if (tokens == null || tokens.isEmpty()) return instructions;

        // --- Extract assignment target and RHS expression tokens ---
        String assignTarget = null;
        List<LexicalAnalyzer.Token> exprTokens = extractExpression(tokens);

        // Find the assignment target (the identifier before '=')
        assignTarget = findAssignTarget(tokens);

        if (exprTokens.isEmpty() || assignTarget == null) return instructions;

        // Build a mutable list of "value" strings (identifiers or temps)
        // interleaved with operators, mirroring the expression token stream.
        // We work on parallel lists: operands and operators.
        List<String> operands = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        for (LexicalAnalyzer.Token t : exprTokens) {
            if (t.tokenAttribute.equals("Identifier")) {
                operands.add(t.tokenName);
            } else if (t.tokenAttribute.equals("Operator")) {
                operators.add(t.tokenName);
            }
        }

        // --- Pass 1: Resolve * and / (higher precedence) left to right ---
        int i = 0;
        while (i < operators.size()) {
            String op = operators.get(i);
            if (op.equals("*") || op.equals("/")) {
                String temp = newTemp();
                TACInstruction instr = new TACInstruction(
                        temp, operands.get(i), op, operands.get(i + 1));
                instructions.add(instr);

                // Replace the two operands and operator with the new temp
                operands.set(i, temp);
                operands.remove(i + 1);
                operators.remove(i);
                // Do NOT advance i — re-examine same position
            } else {
                i++;
            }
        }

        // --- Pass 2: Resolve + and - left to right ---
        // At this point operands.size() == operators.size() + 1
        if (operators.isEmpty()) {
            // Simple assignment: TARGET = operand  (e.g. LET G = a  — no operators)
            // Emit a copy instruction represented as operand + "+" + "0" would be odd;
            // instead we emit it as a special copy: result = op1 (operator field = "=")
            instructions.add(new TACInstruction(assignTarget, operands.get(0), "=", ""));
        } else {
            i = 0;
            while (operators.size() > 1) {
                String op = operators.get(0);
                String temp = newTemp();
                TACInstruction instr = new TACInstruction(
                        temp, operands.get(0), op, operands.get(1));
                instructions.add(instr);
                operands.set(0, temp);
                operands.remove(1);
                operators.remove(0);
            }
            // Final instruction uses the actual assignment target
            instructions.add(new TACInstruction(
                    assignTarget, operands.get(0), operators.get(0), operands.get(1)));
        }

        return instructions;
    }

    /**
     * Strips keywords and the "X =" prefix so we are left with only
     * the right-hand side expression tokens.
     */
    private List<LexicalAnalyzer.Token> extractExpression(List<LexicalAnalyzer.Token> tokens) {
        List<LexicalAnalyzer.Token> result = new ArrayList<>(tokens);

        // Remove leading keyword (LET, WRITE, etc.)
        if (!result.isEmpty() && result.get(0).tokenAttribute.equals("Keyword")) {
            result.remove(0);
        }

        // Remove "X =" at the start
        if (result.size() >= 2
                && result.get(0).tokenAttribute.equals("Identifier")
                && result.get(1).tokenName.equals("=")) {
            result.remove(0); // remove identifier
            result.remove(0); // remove '='
        }

        return result;
    }

    /**
     * Finds the identifier that is the assignment target (left of '=').
     * For "LET G = ..."  or  "M = ..."  this returns "G" / "M".
     */
    private String findAssignTarget(List<LexicalAnalyzer.Token> tokens) {
        List<LexicalAnalyzer.Token> working = new ArrayList<>(tokens);

        // Skip leading keyword
        if (!working.isEmpty() && working.get(0).tokenAttribute.equals("Keyword")) {
            working.remove(0);
        }

        // Next should be: Identifier '='
        if (working.size() >= 2
                && working.get(0).tokenAttribute.equals("Identifier")
                && working.get(1).tokenName.equals("=")) {
            return working.get(0).tokenName;
        }

        return null;
    }

    /**
     * Pretty-prints the TAC instruction list to stdout with a header.
     */
    public void printICR(List<TACInstruction> instructions, int lineNo) {
        System.out.println("\n" + "=".repeat(50));
        System.out.printf("  INTERMEDIATE CODE REPRESENTATION — Line %d%n", lineNo);
        System.out.println("=".repeat(50));

        if (instructions == null || instructions.isEmpty()) {
            System.out.println("  (no instructions generated)");
        } else {
            for (TACInstruction instr : instructions) {
                // Pretty-print copy instructions
                if (instr.operator.equals("=") && instr.operand2.isEmpty()) {
                    System.out.printf("  %-6s = %s%n", instr.result, instr.operand1);
                } else {
                    System.out.println(instr);
                }
            }
        }
        System.out.println("=".repeat(50) + "\n");
    }
}