package minicompiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final List<String> DEFAULT_PROGRAM = Arrays.asList(
            "BEGIN",
            "INTEGER A, B, C, E, M, N, G, H, I, a, c",
            "INPUT A, B, C",
            "LET B = A */ M",
            "LET G = a + c",
            "temp = <s%**h - j / w +d +*$&;",
            "M = A/B+C",
            "N = G/H-I+a*B/c",
            "WRITE M",
            "WRITEE F;",
            "END"
    );

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean keepRunning = true;
        boolean colorEnabled = isColorEnabled(args, scanner);

        System.out.println("===============================================");
        System.out.println(" MINI COMPILER (CTE711S Group Assignment)");
        System.out.println("===============================================");

        while (keepRunning) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    List<String> programLines = chooseProgramInput(scanner, true);
                    Compiler compiler = new Compiler(colorEnabled);
                    compiler.processLineByLine(programLines);
                }
                case "2" -> {
                    List<String> programLines = chooseProgramInput(scanner, false);
                    Compiler compiler = new Compiler(colorEnabled);
                    compiler.processAllAtOnce(programLines);
                }
                case "3" -> {
                    keepRunning = false;
                    System.out.println("Exiting mini compiler. Goodbye.");
                }
                default -> System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\nChoose compilation mode:");
        System.out.println("1. Process line-by-line");
        System.out.println("2. Process all-at-once");
        System.out.println("3. Exit");
        System.out.print("Enter option: ");
    }

    private static boolean isColorEnabled(String[] args, Scanner scanner) {
        if (args != null) {
            for (String arg : args) {
                if ("--no-color".equalsIgnoreCase(arg)) {
                    System.out.println("Color output disabled via --no-color.");
                    return false;
                }
            }
        }

        System.out.print("Enable color output? (Y/n): ");
        String answer = scanner.nextLine().trim();
        return answer.isEmpty() || answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    private static List<String> chooseProgramInput(Scanner scanner, boolean lineByLineMode) {
        while (true) {
            System.out.println("\nSelect input source:");
            System.out.println("1. Use assignment sample program");
            System.out.println("2. Enter custom program");
            System.out.print("Enter option: ");
            String inputChoice = scanner.nextLine().trim();

            if ("1".equals(inputChoice)) {
                return new ArrayList<>(DEFAULT_PROGRAM);
            }

            if ("2".equals(inputChoice)) {
                return readCustomProgram(scanner, lineByLineMode);
            }

            System.out.println("Invalid choice. Please select 1 or 2.");
        }
    }

    private static List<String> readCustomProgram(Scanner scanner, boolean lineByLineMode) {
        List<String> lines = new ArrayList<>();
        if (lineByLineMode) {
            System.out.println("\nEnter your program line by line.");
        } else {
            System.out.println("\nPaste your full program.");
        }
        System.out.println("Type DONE on its own line to finish input.\n");

        while (true) {
            String line = scanner.nextLine();
            if ("DONE".equalsIgnoreCase(line.trim())) {
                break;
            }
            lines.add(line);
        }

        if (lines.isEmpty()) {
            System.out.println("No lines entered. Falling back to assignment sample program.");
            return new ArrayList<>(DEFAULT_PROGRAM);
        }

        return lines;
    }
}
