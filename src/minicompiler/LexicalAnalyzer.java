package minicompiler;


import java.util.*;

/**
 * Lexical Analyzer (Scanner)
 * Breaks source code into tokens and detects lexical errors
 * Includes error recovery - reports ALL errors in a line
 *
 * @author Alicia Sasamba
 * @version 1.0
 */
public class LexicalAnalyzer {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "BEGIN", "INTEGER", "LET", "INPUT", "WRITE", "END"
    ));

    private static final Set<Character> OPERATORS = new HashSet<>(Arrays.asList('+', '-', '*', '/'));
    private static final Set<Character> SYMBOLS = new HashSet<>(Arrays.asList('=', ';'));

    private int globalTokenNumber = 0;

    public static class Token {
        public int tokenNumber;
        public String tokenName;
        public String tokenAttribute;
        public int lineNo;
        public String errorMessage;

        public Token(int tokenNumber, String tokenName, String tokenAttribute, int lineNo) {
            this.tokenNumber = tokenNumber;
            this.tokenName = tokenName;
            this.tokenAttribute = tokenAttribute;
            this.lineNo = lineNo;
            this.errorMessage = null;
        }

        @Override
        public String toString() {
            if (errorMessage != null) {
                return String.format("%-15s %-20s %-15s", "ERROR", tokenName, errorMessage);
            }
            return String.format("%-15d %-20s %-15s", tokenNumber, tokenName, tokenAttribute);
        }
    }

    public void resetTokenCounter() {
        globalTokenNumber = 0;
    }

    public List<Token> scanLine(String line, int lineNo) {
        List<Token> tokens = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (line == null || line.trim().isEmpty()) {
            return tokens;
        }

        StringBuilder currentToken = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // Skip spaces
            if (c == ' ') {
                if (currentToken.length() > 0) {
                    processToken(currentToken.toString(), tokens, errors, lineNo);
                    currentToken = new StringBuilder();
                }
                continue;
            }

            // Comma is a separator (used in INTEGER A, B, C) - skip it
            if (c == ',') {
                if (currentToken.length() > 0) {
                    processToken(currentToken.toString(), tokens, errors, lineNo);
                    currentToken = new StringBuilder();
                }
                continue;
            }

            // Delimiter (operator or symbol) - process as its own token
            if (isDelimiter(c)) {
                if (currentToken.length() > 0) {
                    processToken(currentToken.toString(), tokens, errors, lineNo);
                    currentToken = new StringBuilder();
                }
                processToken(String.valueOf(c), tokens, errors, lineNo);
                continue;
            }

            currentToken.append(c);
        }

        if (currentToken.length() > 0) {
            processToken(currentToken.toString(), tokens, errors, lineNo);
        }

        if (!errors.isEmpty()) {
            for (String err : errors) {
                System.out.println(err);
            }
            return null;
        }

        return tokens;
    }

    private boolean isDelimiter(char c) {
        return OPERATORS.contains(c) || SYMBOLS.contains(c);
    }

    private void processToken(String part, List<Token> tokens, List<String> errors, int lineNo) {
        if (part == null || part.isEmpty()) return;

        // Check for invalid characters (not letter, digit, operator, symbol)
        if (!part.matches("[A-Za-z0-9=+\\-*/;]+")) {
            errors.add(String.format("Lexical Error (Line %d): Invalid character '%s'", lineNo, part));
            return;
        }

        // 1. Keyword check first
        if (KEYWORDS.contains(part)) {
            globalTokenNumber++;
            tokens.add(new Token(globalTokenNumber, part, "Keyword", lineNo));
            return;
        }

        // 2. Misspelled keyword - all uppercase but not a valid keyword (e.g. WRITEE)
        //    Must come BEFORE multi-character identifier check
        if (part.length() > 1 && part.matches("[A-Z]+")) {
            errors.add(String.format("Lexical Error (Line %d): Misspelled keyword '%s' - valid keywords: BEGIN, INTEGER, LET, INPUT, WRITE, END", lineNo, part));
            return;
        }

        // 3. Single letter identifier (A-Z or a-z)
        if (part.matches("[A-Za-z]")) {
            globalTokenNumber++;
            tokens.add(new Token(globalTokenNumber, part, "Identifier", lineNo));
            return;
        }

        // 4. Multi-character mixed/lowercase identifier (e.g. "temp") - not allowed
        if (part.length() > 1 && part.matches("[A-Za-z]+")) {
            errors.add(String.format("Lexical Error (Line %d): Invalid identifier '%s' - only single letters allowed", lineNo, part));
            return;
        }

        // 5. Operator
        if (part.length() == 1 && OPERATORS.contains(part.charAt(0))) {
            globalTokenNumber++;
            tokens.add(new Token(globalTokenNumber, part, "Operator", lineNo));
            return;
        }

        // 6. Symbol
        if (part.length() == 1 && SYMBOLS.contains(part.charAt(0))) {
            globalTokenNumber++;
            tokens.add(new Token(globalTokenNumber, part, "Symbol", lineNo));
            return;
        }

        // Unknown token
        errors.add(String.format("Lexical Error (Line %d): Unknown token '%s'", lineNo, part));
    }

    public void printTokenTableHeader() {
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("%-15s %-20s %-15s%n", "TOKEN NUMBER", "TOKEN NAME", "TOKEN ATTRIBUTE");
        System.out.println("=".repeat(60));
    }

    public void printTokenTableFooter() {
        System.out.println("=".repeat(60));
        System.out.printf("Total Tokens: %d%n", globalTokenNumber);
        System.out.println("=".repeat(60) + "\n");
    }

    public void printTokenTable(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) return;
        printTokenTableHeader();
        for (Token t : tokens) {
            System.out.println(t);
        }
        printTokenTableFooter();
    }

    public int getTotalTokenCount() {
        return globalTokenNumber;
    }
}