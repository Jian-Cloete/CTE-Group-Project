package minicompiler;

import java.util.*;

/**
 * Semantic Analyzer
 * Checks meaning and context of valid tokens:
 * - Detects invalid/forbidden symbols: %, $, &, <, >, ;
 * - Checks that variables used in expressions were declared via INTEGER
 * - Checks that variables on the left-hand side of assignments are declared
 *
 * Runs ONLY on lines that passed Lexical and Syntax analysis.
 *
 * @author [Your Name]
 * @version 1.0
 */
public class SemanticAnalyzer {

    // Symbols that are explicitly forbidden per the assignment spec
    private static final Set<Character> FORBIDDEN_SYMBOLS = new HashSet<>(Arrays.asList(
            '%', '$', '&', '<', '>', ';'
    ));

    // Tracks which identifiers have been declared (populated from INTEGER lines)
    private final Set<String> declaredVariables = new HashSet<>();

    /**
     * Register declared variables from an INTEGER declaration line.
     * Call this when you encounter a line like: INTEGER A, B, C, E, M, N, G, H, I, a, c
     *
     * @param tokens token list from the INTEGER line (already lexed)
     */
    public void registerDeclarations(List<LexicalAnalyzer.Token> tokens) {
        if (tokens == null) return;
        for (LexicalAnalyzer.Token t : tokens) {
            if (t.tokenAttribute.equals("Identifier")) {
                declaredVariables.add(t.tokenName);
            }
        }
    }

    /**
     * Main semantic analysis method.
     * Returns null if the line is semantically valid.
     * Returns an error message string if a semantic error is found.
     *
     * @param rawLine  the original source line (used for forbidden symbol scan)
     * @param tokens   token list produced by LexicalAnalyzer
     * @param lineNo   line number for error reporting
     * @return null if valid, error message string if invalid
     */
    public String analyze(String rawLine, List<LexicalAnalyzer.Token> tokens, int lineNo) {

        // Check 1: Scan raw line for forbidden symbols
        String forbiddenError = checkForbiddenSymbols(rawLine, lineNo);
        if (forbiddenError != null) return forbiddenError;

        // Check 2: All identifiers used in expressions must be declared
        String undeclaredError = checkUndeclaredVariables(tokens, lineNo);
        if (undeclaredError != null) return undeclaredError;

        return null; // No semantic errors
    }

    /**
     * Check 1: Scan the raw source line character by character for forbidden symbols.
     * Per spec: %, $, &, <, >, ; are not allowed → Semantic Error
     *
     * Note: The LexicalAnalyzer may not tokenise these (it may flag them first),
     * but we defensively re-check here on the raw line so the Semantic stage is
     * self-contained and reports the correct error type when called directly.
     */
    private String checkForbiddenSymbols(String rawLine, int lineNo) {
        if (rawLine == null) return null;
        for (int i = 0; i < rawLine.length(); i++) {
            char c = rawLine.charAt(i);
            if (FORBIDDEN_SYMBOLS.contains(c)) {
                return String.format(
                        "Semantic Error (Line %d): Forbidden symbol '%c' is not allowed in this language",
                        lineNo, c);
            }
        }
        return null;
    }

    /**
     * Check 2: Every identifier that appears in an expression must have been
     * declared in an INTEGER statement earlier in the program.
     *
     * We skip the declaration line itself (first token = "INTEGER") and
     * INPUT lines (variables being read in don't need prior assignment).
     * For all other lines we check every identifier token.
     */
    private String checkUndeclaredVariables(List<LexicalAnalyzer.Token> tokens, int lineNo) {
        if (tokens == null || tokens.isEmpty()) return null;

        String firstTokenName = tokens.get(0).tokenName;

        // Skip lines that don't reference expressions
        if (firstTokenName.equals("INTEGER") || firstTokenName.equals("BEGIN")
                || firstTokenName.equals("END") || firstTokenName.equals("INPUT")) {
            return null;
        }

        // For LET / assignment / WRITE lines - check all identifiers
        for (LexicalAnalyzer.Token t : tokens) {
            if (t.tokenAttribute.equals("Identifier")) {
                if (!declaredVariables.contains(t.tokenName)) {
                    return String.format(
                            "Semantic Error (Line %d): Undeclared variable '%s' - must be declared in INTEGER statement",
                            lineNo, t.tokenName);
                }
            }
        }

        return null;
    }

    /**
     * Returns a read-only view of all currently declared variables.
     * Useful for debugging or for IntermediateCode to know the symbol table.
     */
    public Set<String> getDeclaredVariables() {
        return Collections.unmodifiableSet(declaredVariables);
    }

    /**
     * Resets the declared variable table.
     * Call this between independent compilations (e.g. line-by-line mode restart).
     */
    public void reset() {
        declaredVariables.clear();
    }
}

