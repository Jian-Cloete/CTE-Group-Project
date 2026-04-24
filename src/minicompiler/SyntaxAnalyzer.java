package minicompiler;



import java.util.*;

/**
 * Syntax Analyzer (Parser)
 * Checks tokens against grammar rules and catches:
 * - Numbers (0-9) not allowed
 * - Combined operators e.g. star-slash, plus-star
 * - Semicolon at end of line
 * - Expression must follow grammar rules R1-R4
 *
 * @author Alicia Sasamba
 * @version 1.0
 */
public class SyntaxAnalyzer {

    /**
     * Main method - runs all syntax checks on a line
     * Returns null if valid, returns error message if invalid
     */
    public String analyze(String line, List<LexicalAnalyzer.Token> tokens, int lineNo) {

        // Check 1: Numbers not allowed
        String numberError = checkForNumbers(line, lineNo);
        if (numberError != null) return numberError;

        // Check 2: Semicolon at end of line not allowed
        String semicolonError = checkForSemicolon(tokens, lineNo);
        if (semicolonError != null) return semicolonError;

        // Check 3: Combined operators not allowed
        String combinedOpError = checkCombinedOperators(tokens, lineNo);
        if (combinedOpError != null) return combinedOpError;

        // Check 4: Grammar rules R1-R4
        // Skip for declaration/IO lines - they are not expressions
        String firstToken = tokens.get(0).tokenName;
        boolean isNonExpressionLine = firstToken.equals("INTEGER") || firstToken.equals("INPUT")
                || firstToken.equals("BEGIN") || firstToken.equals("END") || firstToken.equals("WRITE");
        if (!isNonExpressionLine) {
            String grammarError = checkGrammar(tokens, lineNo);
            if (grammarError != null) return grammarError;
        }

        return null;
    }

    /**
     * Check 1: Numbers 0-9 are not allowed
     */
    private String checkForNumbers(String line, int lineNo) {
        for (char c : line.toCharArray()) {
            if (Character.isDigit(c)) {
                return String.format("Syntax Error (Line %d): Numbers are not allowed - found digit '%c'", lineNo, c);
            }
        }
        return null;
    }

    /**
     * Check 2: Semicolon at end of line is not allowed
     */
    private String checkForSemicolon(List<LexicalAnalyzer.Token> tokens, int lineNo) {
        if (tokens == null || tokens.isEmpty()) return null;
        LexicalAnalyzer.Token last = tokens.get(tokens.size() - 1);
        if (last.tokenName.equals(";")) {
            return String.format("Syntax Error (Line %d): Semicolon at end of line is not allowed", lineNo);
        }
        return null;
    }

    /**
     * Check 3: Two operators next to each other not allowed
     */
    private String checkCombinedOperators(List<LexicalAnalyzer.Token> tokens, int lineNo) {
        if (tokens == null || tokens.isEmpty()) return null;
        for (int i = 0; i < tokens.size() - 1; i++) {
            LexicalAnalyzer.Token current = tokens.get(i);
            LexicalAnalyzer.Token next = tokens.get(i + 1);
            if (current.tokenAttribute.equals("Operator") && next.tokenAttribute.equals("Operator")) {
                return String.format("Syntax Error (Line %d): Combined operators '%s%s' are not allowed",
                        lineNo, current.tokenName, next.tokenName);
            }
        }
        return null;
    }

    /**
     * Check 4: Grammar rules R1-R4
     * Valid pattern: Identifier (Operator Identifier)*
     */
    private String checkGrammar(List<LexicalAnalyzer.Token> tokens, int lineNo) {
        if (tokens == null || tokens.isEmpty()) return null;

        List<LexicalAnalyzer.Token> expr = extractExpression(tokens);
        if (expr.isEmpty()) return null;

        // Must start with identifier
        if (!expr.get(0).tokenAttribute.equals("Identifier")) {
            return String.format("Syntax Error (Line %d): Expression must start with an identifier, found '%s'",
                    lineNo, expr.get(0).tokenName);
        }

        // Must end with identifier
        if (!expr.get(expr.size() - 1).tokenAttribute.equals("Identifier")) {
            return String.format("Syntax Error (Line %d): Expression must end with an identifier, found '%s'",
                    lineNo, expr.get(expr.size() - 1).tokenName);
        }

        // Pattern: Identifier (Operator Identifier)*
        for (int i = 0; i < expr.size(); i++) {
            if (i % 2 == 0) {
                if (!expr.get(i).tokenAttribute.equals("Identifier")) {
                    return String.format("Syntax Error (Line %d): Expected identifier at position %d, found '%s'",
                            lineNo, i + 1, expr.get(i).tokenName);
                }
            } else {
                if (!expr.get(i).tokenAttribute.equals("Operator")) {
                    return String.format("Syntax Error (Line %d): Expected operator at position %d, found '%s'",
                            lineNo, i + 1, expr.get(i).tokenName);
                }
            }
        }

        return null;
    }

    /**
     * Strips keywords and assignment parts to get just the expression
     * e.g. "LET G = a + c" becomes tokens for "a + c"
     */
    private List<LexicalAnalyzer.Token> extractExpression(List<LexicalAnalyzer.Token> tokens) {
        List<LexicalAnalyzer.Token> result = new ArrayList<>(tokens);

        // Remove leading keyword
        if (!result.isEmpty() && result.get(0).tokenAttribute.equals("Keyword")) {
            result.remove(0);
        }

        // Remove "X =" assignment at start
        if (result.size() >= 2
                && result.get(0).tokenAttribute.equals("Identifier")
                && result.get(1).tokenName.equals("=")) {
            result.remove(0);
            result.remove(0);
        }

        return result;
    }
}