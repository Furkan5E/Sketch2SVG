package com.sketch2svg;

import com.sketch2svg.parser.Sketch;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    // Returns the process exit code: 0 on success, 1 on any failure
    static int run(String[] args) {
        String inputPath = null;
        String outputPath = null;
        String dirPath = null;
        boolean check = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i", "--input", "-o", "--output", "-d", "--dir", "--batch" -> {
                    String option = args[i];
                    String value = optionValue(args, i);
                    if (value == null) {
                        System.err.println("Option " + option + " needs a value");
                        printHelp();
                        return 1;
                    }
                    i++;
                    switch (option) {
                        case "-i", "--input" -> inputPath = value;
                        case "-o", "--output" -> outputPath = value;
                        default -> dirPath = value;
                    }
                }
                case "-h", "--help" -> {
                    printHelp();
                    return 0;
                }
                case "-c", "--check" -> check = true;
                case "-v", "--version" -> {
                    System.out.println("Sketch2SVG " + version());
                    return 0;
                }
                default -> {
                    if (inputPath == null && !args[i].startsWith("-")) {
                        inputPath = args[i];
                    } else if (outputPath == null && !args[i].startsWith("-")) {
                        outputPath = args[i];
                    } else {
                        System.err.println("Unknown argument: " + args[i]);
                        printHelp();
                        return 1;
                    }
                }
            }
        }

        //batch directory conversion
        if (dirPath != null) {
            return convertBatch(dirPath, outputPath, check) ? 0 : 1;
        }

        //single file conversion
        if (inputPath == null) {
            System.err.println("No input given: use -i <file> or -d <dir>");
            printHelp();
            return 1;
        }
        if (check) {
            return checkFile(inputPath) ? 0 : 1;
        }
        if (outputPath == null) {
            outputPath = inputPath.replaceAll("(?i)\\.txt$", "") + ".svg";
            if (outputPath.equals(inputPath)) {
                outputPath = inputPath + ".svg";
            }
        }

        return convertSingleFile(inputPath, outputPath) ? 0 : 1;
    }

    // Project version from the jar manifest, or "dev" when running from compiled classes
    static String version() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }

    // The argument after an option, or null if it is missing or is another option ("-" alone is a value)
    private static String optionValue(String[] args, int i) {
        if (i + 1 >= args.length) {
            return null;
        }
        String next = args[i + 1];
        return next.equals("-") || !next.startsWith("-") ? next : null;
    }

    private static boolean convertSingleFile(String inputPath, String outputPath) {
        System.out.println("Processing: " + inputPath);
        Sketch sketch = new Sketch();
        if (!sketch.fromFile(inputPath) || !sketch.exportSVG(outputPath)) {
            System.err.println("Failed to convert: " + inputPath);
            return false;
        }
        System.out.println("Successfully generated: " + outputPath);
        return true;
    }

    // Parses without writing anything; fails if the script has errors (warnings are reported but allowed)
    private static boolean checkFile(String inputPath) {
        Sketch sketch = new Sketch();
        if (!sketch.fromFile(inputPath)) {
            return false;
        }
        System.out.printf("%s: %d error(s), %d warning(s)%n",
                inputPath, sketch.getErrorCount(), sketch.getWarningCount());
        return sketch.getErrorCount() == 0;
    }

    private static boolean convertBatch(String inputDir, String outputDir, boolean check) {
        File folder = new File(inputDir);
        if (!folder.isDirectory()) {
            System.err.println("Error: Provided path is not a directory: " + inputDir);
            return false;
        }

        File[] files = folder.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("No .txt files found in directory: " + inputDir);
            return true;
        }

        String targetDir = outputDir != null ? outputDir : inputDir;
        if (!check) {
            new File(targetDir).mkdirs();
        }

        System.out.printf("Batch %s %d file(s)...%n", check ? "checking" : "converting", files.length);
        int failed = 0;
        for (File file : files) {
            String outName = file.getName().replaceAll("(?i)\\.txt$", "") + ".svg";
            Path outPath = Paths.get(targetDir, outName);
            boolean ok = check ? checkFile(file.getPath()) : convertSingleFile(file.getPath(), outPath.toString());
            if (!ok) {
                failed++;
            }
        }

        if (failed > 0) {
            System.err.printf("Batch %s finished with %d failure(s).%n", check ? "check" : "conversion", failed);
            return false;
        }
        System.out.printf("Batch %s complete.%n", check ? "check" : "conversion");
        return true;
    }

    private static void printHelp() {
        System.out.println("""
            Sketch2SVG - Vector Graphics CLI Generator
            
            Usage:
              java -cp target/classes com.sketch2svg.Main [options]
              java -jar Sketch2SVG.jar [options]
            
            Options:
              -i, --input <file>       Path to source sketch .txt file
              -o, --output <file/dir>  Path for output .svg file or destination folder
              -d, --batch <dir>        Batch convert all .txt files inside directory
              -c, --check              Only report errors and warnings; write nothing
                                       (exit code 1 if any script has errors)
              -h, --help               Display this help message
              -v, --version            Display the version
            """);
    }
}