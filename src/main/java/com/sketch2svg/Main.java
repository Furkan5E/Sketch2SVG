package com.sketch2svg;

import com.sketch2svg.parser.Sketch;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        String inputPath = null;
        String outputPath = null;
        String dirPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i", "--input" -> {
                    if (i + 1 < args.length) inputPath = args[++i];
                }
                case "-o", "--output" -> {
                    if (i + 1 < args.length) outputPath = args[++i];
                }
                case "-d", "--dir", "--batch" -> {
                    if (i + 1 < args.length) dirPath = args[++i];
                }
                case "-h", "--help" -> {
                    printHelp();
                    return;
                }
                default -> {
                    if (inputPath == null && !args[i].startsWith("-")) {
                        inputPath = args[i];
                    } else if (outputPath == null && !args[i].startsWith("-")) {
                        outputPath = args[i];
                    } else {
                        System.err.println("Unknown argument: " + args[i]);
                        printHelp();
                        return;
                    }
                }
            }
        }

        //batch directory conversion
        if (dirPath != null) {
            convertBatch(dirPath, outputPath);
            return;
        }

        //single file conversion fallback
        if (inputPath == null) {
            inputPath = "src/main/resources/sketch.txt";
        }
        if (outputPath == null) {
            outputPath = inputPath.replaceAll("(?i)\\.txt$", "") + ".svg";
            if (outputPath.equals(inputPath)) {
                outputPath = inputPath + ".svg";
            }
        }

        convertSingleFile(inputPath, outputPath);
    }

    private static void convertSingleFile(String inputPath, String outputPath) {
        System.out.println("Processing: " + inputPath);
        Sketch sketch = new Sketch();
        sketch.fromFile(inputPath);
        sketch.exportSVG(outputPath);
        System.out.println("Successfully generated: " + outputPath);
    }

    private static void convertBatch(String inputDir, String outputDir) {
        File folder = new File(inputDir);
        if (!folder.isDirectory()) {
            System.err.println("Error: Provided path is not a directory: " + inputDir);
            return;
        }

        File[] files = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("No .txt files found in directory: " + inputDir);
            return;
        }

        String targetDir = outputDir != null ? outputDir : inputDir;
        new File(targetDir).mkdirs();

        System.out.printf("Batch converting %d file(s)...%n", files.length);
        for (File file : files) {
            String outName = file.getName().replaceAll("(?i)\\.txt$", "") + ".svg";
            Path outPath = Paths.get(targetDir, outName);
            convertSingleFile(file.getPath(), outPath.toString());
        }
        System.out.println("Batch conversion complete.");
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