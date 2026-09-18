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
            return convertBatch(dirPath, outputPath) ? 0 : 1;
        }

        //single file conversion
        if (inputPath == null) {
            System.err.println("No input given: use -i <file> or -d <dir>");
            printHelp();
            return 1;
        }
        if (outputPath == null) {
            outputPath = inputPath.replaceAll("(?i)\\.txt$", "") + ".svg";
            if (outputPath.equals(inputPath)) {
                outputPath = inputPath + ".svg";
            }
        }

        return convertSingleFile(inputPath, outputPath) ? 0 : 1;
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

    private static boolean convertBatch(String inputDir, String outputDir) {
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
        new File(targetDir).mkdirs();

        System.out.printf("Batch converting %d file(s)...%n", files.length);
        int failed = 0;
        for (File file : files) {
            String outName = file.getName().replaceAll("(?i)\\.txt$", "") + ".svg";
            Path outPath = Paths.get(targetDir, outName);
            if (!convertSingleFile(file.getPath(), outPath.toString())) {
                failed++;
            }
        }

        if (failed > 0) {
            System.err.printf("Batch conversion finished with %d failure(s).%n", failed);
            return false;
        }
        System.out.println("Batch conversion complete.");
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
              -h, --help               Display this help message
            """);
    }
}