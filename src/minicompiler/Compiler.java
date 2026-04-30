package minicompiler;

import java.util.ArrayList;
import java.util.List;

public class Compiler {

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String FORBIDDEN_SEMANTIC_SYMBOLS = "%$&<>";

    private final LexicalAnalyzer lexicalAnalyzer;
    private final SyntaxAnalyzer syntaxAnalyzer;
    private final SemanticAnalyzer semanticAnalyzer;
    private final IntermediateCode intermediateCode;
    private final CodeGenerator codeGenerator;
    private final CodeOptimizer codeOptimizer;
    private final TargetCode targetCode;
    private final boolean useColors;

    private CompileStats stats;

    private static class CompileStats {
        int totalLines;
        int lexicalErrors;
        int syntaxErrors;
        int semanticErrors;
        int translatedLines;
    }

    private static class ValidLine {
        int lineNo;
        String rawLine;
        List<LexicalAnalyzer.Token> tokens;

        ValidLine(int lineNo, String rawLine, List<LexicalAnalyzer.Token> tokens) {
            this.lineNo = lineNo;
            this.rawLine = rawLine;
            this.tokens = tokens;
        }
    }

    public Compiler(boolean useColors) {
        this.lexicalAnalyzer = new LexicalAnalyzer();
        this.syntaxAnalyzer = new SyntaxAnalyzer();
        this.semanticAnalyzer = new SemanticAnalyzer();
        this.intermediateCode = new IntermediateCode();
        this.codeGenerator = new CodeGenerator();
        this.codeOptimizer = new CodeOptimizer();
        this.targetCode = new TargetCode();
        this.useColors = useColors;
        this.stats = new CompileStats();
    }

    /**
     * Assignment mode 1:
     * Translate the program iteratively, line by line.
     */
    public void processLineByLine(List<String> lines) {
        resetForNewCompilation(lines);
        printHeader("MODE: LINE-BY-LINE");

        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i);
            processSingleLine(line, lineNo, true, null);
        }

        printSummary();
    }

    /**
     * Assignment mode 2:
     * Translate the program iteratively, all at once.
     * Pass 1 validates all lines, then Pass 2 translates valid expression lines.
     */
    public void processAllAtOnce(List<String> lines) {
        resetForNewCompilation(lines);
        printHeader("MODE: ALL-AT-ONCE");

        List<ValidLine> validExpressionLines = new ArrayList<>();

        printInfo("Pass 1/2: Checking all lines for lexical, syntax and semantic errors...");
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String line = lines.get(i);
            processSingleLine(line, lineNo, false, validExpressionLines);
        }

        printInfo("Pass 2/2: Running stages 4-7 for valid expression lines...");
        for (ValidLine valid : validExpressionLines) {
            printLineBanner(valid.lineNo, valid.rawLine);
            runTranslationStages(valid.tokens, valid.lineNo);
        }

        printSummary();
    }

    private void processSingleLine(
            String rawLine,
            int lineNo,
            boolean runTranslationImmediately,
            List<ValidLine> allAtOnceQueue) {

        stats.totalLines++;
        printLineBanner(lineNo, rawLine);

        if (rawLine == null || rawLine.trim().isEmpty()) {
            printWarn("Empty line skipped.");
            return;
        }

        Character forbiddenSymbol = findForbiddenSemanticSymbol(rawLine);
        if (forbiddenSymbol != null) {
            stats.semanticErrors++;
            printError(String.format(
                    "Semantic Error (Line %d): Forbidden symbol '%c' is not allowed in this language",
                    lineNo, forbiddenSymbol));
            return;
        }

        // Stage 1: Lexical analysis
        List<LexicalAnalyzer.Token> tokens = lexicalAnalyzer.scanLine(rawLine, lineNo);
        if (tokens == null) {
            stats.lexicalErrors++;
            printError("Compilation stopped for this line (Lexical Error).");
            return;
        }
        printSuccess("Lexical Analysis: PASS");

        // Stage 2: Syntax analysis
        String syntaxError = syntaxAnalyzer.analyze(rawLine, tokens, lineNo);
        if (syntaxError != null) {
            stats.syntaxErrors++;
            printError(syntaxError);
            return;
        }
        printSuccess("Syntax Analysis: PASS");

        // Register declarations before semantic checks on subsequent lines.
        if (startsWithKeyword(tokens, "INTEGER")) {
            semanticAnalyzer.registerDeclarations(tokens);
        }

        // Stage 3: Semantic analysis
        String semanticError = semanticAnalyzer.analyze(rawLine, tokens, lineNo);
        if (semanticError != null) {
            stats.semanticErrors++;
            printError(semanticError);
            return;
        }
        printSuccess("Semantic Analysis: PASS");

        // Only expression assignment lines should continue through stages 4-7.
        if (!isExpressionAssignmentLine(tokens)) {
            printInfo("No translation needed for this valid non-expression line.");
            return;
        }

        if (runTranslationImmediately) {
            runTranslationStages(tokens, lineNo);
        } else if (allAtOnceQueue != null) {
            allAtOnceQueue.add(new ValidLine(lineNo, rawLine, tokens));
            printInfo("Queued for stages 4-7 in pass 2.");
        }
    }

    private void runTranslationStages(List<LexicalAnalyzer.Token> tokens, int lineNo) {
        // Stage 4: Intermediate code
        List<IntermediateCode.TACInstruction> tac = intermediateCode.generate(tokens, lineNo);
        intermediateCode.printICR(tac, lineNo);

        // Stage 5: Code generation
        List<CodeGenerator.AssemblyInstruction> assembly = codeGenerator.generate(tac);
        codeGenerator.printAssembly(assembly, lineNo);

        // Stage 6: Code optimization
        List<CodeGenerator.AssemblyInstruction> optimized = codeOptimizer.optimize(assembly);
        codeOptimizer.printOptimized(optimized, lineNo);

        // Stage 7: Target machine code
        List<TargetCode.BinaryInstruction> binary = targetCode.generate(optimized);
        targetCode.printBinary(binary, lineNo);

        stats.translatedLines++;
    }

    private Character findForbiddenSemanticSymbol(String rawLine) {
        for (int i = 0; i < rawLine.length(); i++) {
            char c = rawLine.charAt(i);
            if (FORBIDDEN_SEMANTIC_SYMBOLS.indexOf(c) >= 0) {
                return c;
            }
        }
        return null;
    }

    private boolean startsWithKeyword(List<LexicalAnalyzer.Token> tokens, String keyword) {
        return tokens != null
                && !tokens.isEmpty()
                && keyword.equals(tokens.get(0).tokenName);
    }

    private boolean isExpressionAssignmentLine(List<LexicalAnalyzer.Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }

        int startIdx = 0;
        if ("LET".equals(tokens.get(0).tokenName)) {
            startIdx = 1;
        }

        // Need: Identifier '=' Identifier Operator Identifier ...
        if (tokens.size() < startIdx + 5) {
            return false;
        }

        if (!"Identifier".equals(tokens.get(startIdx).tokenAttribute)) {
            return false;
        }
        if (!"=".equals(tokens.get(startIdx + 1).tokenName)) {
            return false;
        }
        if (!"Identifier".equals(tokens.get(startIdx + 2).tokenAttribute)) {
            return false;
        }
        if (!"Operator".equals(tokens.get(startIdx + 3).tokenAttribute)) {
            return false;
        }
        return "Identifier".equals(tokens.get(startIdx + 4).tokenAttribute);
    }

    private void resetForNewCompilation(List<String> lines) {
        lexicalAnalyzer.resetTokenCounter();
        semanticAnalyzer.reset();
        intermediateCode.reset();
        stats = new CompileStats();
        if (lines == null) {
            throw new IllegalArgumentException("Program lines cannot be null.");
        }
    }

    private void printHeader(String mode) {
        String title = "MINI COMPILER - " + mode;
        System.out.println("\n" + "=".repeat(80));
        System.out.println(colorize(title, CYAN));
        System.out.println("=".repeat(80));
    }

    private void printLineBanner(int lineNo, String line) {
        System.out.println("\n" + "-".repeat(80));
        System.out.println(colorize("Line " + lineNo + ": " + line, YELLOW));
        System.out.println("-".repeat(80));
    }

    private void printSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(colorize("COMPILATION SUMMARY", CYAN));
        System.out.println("=".repeat(80));
        System.out.printf("Total lines processed: %d%n", stats.totalLines);
        System.out.printf("Lexical errors:       %d%n", stats.lexicalErrors);
        System.out.printf("Syntax errors:        %d%n", stats.syntaxErrors);
        System.out.printf("Semantic errors:      %d%n", stats.semanticErrors);
        System.out.printf("Fully translated:     %d%n", stats.translatedLines);
        System.out.println("=".repeat(80) + "\n");
    }

    private void printInfo(String message) {
        System.out.println(colorize("[INFO] " + message, CYAN));
    }

    private void printWarn(String message) {
        System.out.println(colorize("[WARN] " + message, YELLOW));
    }

    private void printSuccess(String message) {
        System.out.println(colorize("[OK]   " + message, GREEN));
    }

    private void printError(String message) {
        System.out.println(colorize("[ERR]  " + message, RED));
    }

    private String colorize(String text, String color) {
        if (!useColors) {
            return text;
        }
        return color + text + RESET;
    }
}
