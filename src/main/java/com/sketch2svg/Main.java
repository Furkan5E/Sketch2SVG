package com.sketch2svg;

import com.sketch2svg.parser.Sketch;

public class Main {

    public static void main(String[] args) {
        String inputPath = null;
        String outputPath = null;

        // parse command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i", "--input" -> {
                    if (i + 1 < args.length) {
                        inputPath = args[++i];
                    }
                }
                case "-o", "--output" -> {
                    if (i + 1 < args.length) {
                        outputPath = args[++i];
                    }
                }
                case "-h", "--help" -> {
                    printHelp();
                    return;
                }
                default -> {
                    // if a positional argument without flag is provided
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

        // default fallback path
        if (inputPath == null) {
            inputPath = "src/main/resources/sketch.txt";
        }
        if (outputPath == null) {
            outputPath = inputPath.replaceAll("(?i)\\.txt$", "") + ".svg";
            if (outputPath.equals(inputPath)) {
                outputPath = inputPath + ".svg";
            }
        }

        System.out.println("Processing: " + inputPath);
        System.out.println("Destination: " + outputPath);

        Sketch sketch = new Sketch();
        sketch.fromFile(inputPath);
        sketch.exportSVG(outputPath);

        System.out.println("Successfully generated SVG: " + outputPath);
    }

    private static void printHelp() {
        System.out.println("""
            Sketch2SVG - Vector Graphics CLI Generator
            
            Usage:
              java -jar sketch2svg.jar [options]
              java -jar sketch2svg.jar <input.txt> <output.svg>
            
            Options:
              -i, --input <file>     Path to the source sketch .txt file
              -o, --output <file>    Path for the generated .svg file
              -h, --help             Display this help message
            """);
    }
}