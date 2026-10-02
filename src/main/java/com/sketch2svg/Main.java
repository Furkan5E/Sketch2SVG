package com.sketch2svg;

import com.sketch2svg.core.OutputStyle;
import com.sketch2svg.parser.Sketch;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

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
        boolean recursive = false;
        boolean watch = false;
        fit = false;
        grid = false;
        pixelWidth = null;
        style = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i", "--input", "-o", "--output", "-d", "--dir", "--batch", "-s", "--size" -> {
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
                        case "-s", "--size" -> {
                            if (!value.matches("[1-9][0-9]{0,5}")) {
                                System.err.println("Option " + option + " needs a positive whole number of pixels, not " + value);
                                return 1;
                            }
                            pixelWidth = Integer.parseInt(value);
                        }
                        default -> dirPath = value;
                    }
                }
                case "-h", "--help" -> {
                    printHelp();
                    return 0;
                }
                case "-c", "--check" -> check = true;
                case "-r", "--recursive" -> recursive = true;
                case "-w", "--watch" -> watch = true;
                case "-f", "--fit" -> fit = true;
                case "-g", "--grid" -> grid = true;
                case "--pretty", "--minify" -> {
                    OutputStyle chosen = args[i].equals("--pretty") ? OutputStyle.PRETTY : OutputStyle.MINIFIED;
                    if (style != null && style != chosen) {
                        System.err.println("Choose either --pretty or --minify, not both");
                        return 1;
                    }
                    style = chosen;
                }
                case "-v", "--version" -> {
                    System.out.println("Sketch2SVG " + version());
                    return 0;
                }
                default -> {
                    boolean isValue = args[i].equals(STDIO) || !args[i].startsWith("-");
                    if (inputPath == null && isValue) {
                        inputPath = args[i];
                    } else if (outputPath == null && isValue) {
                        outputPath = args[i];
                    } else {
                        System.err.println("Unknown argument: " + args[i]);
                        printHelp();
                        return 1;
                    }
                }
            }
        }

        // Reject paths the file system can't represent (e.g. "a?.svg" on Windows) before anything uses them
        for (String path : new String[]{inputPath, outputPath, dirPath}) {
            if (path == null || path.equals(STDIO)) {
                continue;
            }
            try {
                Path.of(path);
            } catch (InvalidPathException e) {
                System.err.println("Invalid path: " + path + " (" + e.getReason() + ")");
                return 1;
            }
        }

        //batch directory conversion
        if (dirPath != null) {
            if (watch) {
                System.err.println("--watch works on a single file (-i), not with -d");
                return 1;
            }
            if (STDIO.equals(outputPath)) {
                System.err.println("Batch mode writes one file per sketch; -o - (stdout) is not supported with -d");
                return 1;
            }
            log = System.out;
            return convertBatch(dirPath, outputPath, check, recursive) ? 0 : 1;
        }

        //single file conversion
        if (inputPath == null) {
            System.err.println("No input given: use -i <file> or -d <dir>");
            printHelp();
            return 1;
        }
        if (outputPath == null) {
            if (inputPath.equals(STDIO)) {
                outputPath = STDIO; // stdin in, stdout out, for pipelines
            } else {
                outputPath = inputPath.replaceAll("(?i)\\.txt$", "") + ".svg";
                if (outputPath.equals(inputPath)) {
                    outputPath = inputPath + ".svg";
                }
            }
        }
        // When the SVG goes to stdout, status messages must not mix with it
        log = outputPath.equals(STDIO) && !check ? System.err : System.out;

        if (watch) {
            // Watching needs a real file to poll and a real file to rewrite
            if (inputPath.equals(STDIO) || outputPath.equals(STDIO) || check) {
                System.err.println("--watch needs an input file and an output file (not stdin/stdout or --check)");
                return 1;
            }
            return watch(inputPath, outputPath);
        }
        if (check) {
            return checkFile(inputPath) ? 0 : 1;
        }
        return convertSingleFile(inputPath, outputPath) ? 0 : 1;
    }

    // "-" as an input or output path means stdin / stdout
    private static final String STDIO = "-";

    // Where progress messages go (stderr while the SVG itself is written to stdout)
    private static PrintStream log = System.out;

    // Parses a sketch from a file, or from stdin for "-" (includes then resolve against the working directory).
    // Returns null if the input could not be read.
    private static Sketch load(String inputPath) {
        Sketch sketch = new Sketch();
        if (!inputPath.equals(STDIO)) {
            return sketch.fromFile(inputPath) ? sketch : null;
        }
        try {
            sketch.fromString(new String(System.in.readAllBytes(), StandardCharsets.UTF_8), Path.of(""));
            return sketch;
        } catch (IOException e) {
            System.err.println("Could not read stdin: " + e.getMessage());
            return null;
        }
    }

    private static String displayName(String path) {
        return path.equals(STDIO) ? "<stdin>" : path;
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
        log.println("Processing: " + displayName(inputPath));
        Sketch sketch = load(inputPath);
        if (sketch == null) {
            System.err.println("Failed to convert: " + displayName(inputPath));
            return false;
        }
        return write(sketch, inputPath, outputPath);
    }

    private static final long POLL_MILLIS = 300;
    private static final long SETTLE_MILLIS = 100;

    // Converts once, then again whenever the sketch or anything it includes changes; runs until interrupted
    private static int watch(String inputPath, String outputPath) {
        Path main = Path.of(inputPath).toAbsolutePath().normalize();
        FileWatcher watcher = new FileWatcher();
        log.println("Watching " + inputPath + " for changes (Ctrl+C to stop)");
        List<Path> known = List.of(main);
        while (true) {
            // Timestamps from *before* the files are read: a save that lands while this build runs
            // then differs from the snapshot and triggers another build instead of being missed
            Map<Path, FileTime> before = watcher.snapshot(known);

            log.println("Processing: " + inputPath);
            Sketch sketch = load(inputPath);
            List<Path> sources = new ArrayList<>(List.of(main));
            if (sketch != null) {
                write(sketch, inputPath, outputPath);
                sources.addAll(sketch.getSourceFiles()); // includes can change between builds
            }
            watcher.track(sources, before);
            known = sources;

            try {
                do {
                    Thread.sleep(POLL_MILLIS);
                } while (!watcher.changed());
                Thread.sleep(SETTLE_MILLIS); // editors often save in several writes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }
    }

    // Output options from the command line; they override what the script itself sets
    private static boolean fit;
    private static boolean grid;
    private static Integer pixelWidth;
    private static OutputStyle style;

    private static final float FIT_PADDING = 10.f;

    private static void applyOutputOptions(Sketch sketch) {
        if (fit) {
            sketch.setAutoFit(FIT_PADDING);
        }
        if (pixelWidth != null) {
            sketch.setPixelWidth(pixelWidth);
        }
        if (style != null) {
            sketch.setOutputStyle(style);
        }
        if (grid) {
            sketch.setGrid(true);
        }
    }

    // Writes an already-loaded sketch to a file, or to stdout for "-"
    private static boolean write(Sketch sketch, String inputPath, String outputPath) {
        applyOutputOptions(sketch);
        String svg = render(sketch, inputPath);
        if (svg == null) {
            System.err.println("Failed to convert: " + displayName(inputPath));
            return false;
        }
        if (outputPath.equals(STDIO)) {
            System.out.println(svg);
            System.out.flush();
            return true;
        }
        Path output = Path.of(outputPath);
        if (isSourceOf(sketch, output)) {
            System.err.println("Refusing to overwrite the sketch's own source file: " + outputPath + " (choose a different -o)");
            System.err.println("Failed to convert: " + displayName(inputPath));
            return false;
        }
        try {
            Files.writeString(output, svg);
        } catch (IOException e) {
            System.err.println("Could not write SVG file: " + outputPath + " (" + e.getMessage() + ")");
            System.err.println("Failed to convert: " + displayName(inputPath));
            return false;
        }
        log.println("Successfully generated: " + outputPath);
        return true;
    }

    // Whether writing to `output` would replace the sketch itself or a file it includes
    // (the same file under another spelling counts too, e.g. a relative path or different letter case)
    private static boolean isSourceOf(Sketch sketch, Path output) {
        if (!Files.exists(output)) {
            return false; // nothing there to lose
        }
        for (Path source : sketch.getSourceFiles()) {
            try {
                if (Files.exists(source) && Files.isSameFile(output, source)) {
                    return true;
                }
            } catch (IOException e) {
                if (output.toAbsolutePath().normalize().equals(source.toAbsolutePath().normalize())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Builds the whole document in memory first, so a failure never leaves a half-written file.
    // Returns null if a value can't be written, e.g. a coordinate that overflowed to Infinity.
    private static String render(Sketch sketch, String inputPath) {
        try {
            return sketch.toSVGString();
        } catch (IllegalArgumentException e) {
            System.err.println("[Error] " + displayName(inputPath) + ": " + e.getMessage()
                    + " (a size, position or scale is too large)");
            return null;
        }
    }

    // Parses and renders in memory without writing; fails if the script has errors (warnings are reported but allowed)
    private static boolean checkFile(String inputPath) {
        Sketch sketch = load(inputPath);
        if (sketch == null) {
            return false;
        }
        applyOutputOptions(sketch);
        int errors = sketch.getErrorCount() + (render(sketch, inputPath) == null ? 1 : 0);
        log.printf("%s: %d error(s), %d warning(s)%n", displayName(inputPath), errors, sketch.getWarningCount());
        return errors == 0;
    }

    private static boolean convertBatch(String inputDir, String outputDir, boolean check, boolean recursive) {
        Path folder = Path.of(inputDir);
        if (!Files.isDirectory(folder)) {
            System.err.println("Error: Provided path is not a directory: " + inputDir);
            return false;
        }

        // Sorted so runs are repeatable; recursive mode descends into subfolders
        List<Path> files;
        try (Stream<Path> found = recursive ? Files.walk(folder) : Files.list(folder)) {
            files = found.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            System.err.println("Error: Could not read directory: " + inputDir + " (" + e.getMessage() + ")");
            return false;
        }
        if (files.isEmpty()) {
            System.out.println("No .txt files found in directory: " + inputDir);
            return true;
        }

        Path targetDir = Path.of(outputDir != null ? outputDir : inputDir);

        System.out.printf("Batch %s %d file(s)...%n", check ? "checking" : "converting", files.size());
        int failed = 0;
        for (Path file : files) {
            boolean ok;
            if (check) {
                ok = checkFile(file.toString());
            } else {
                // Mirror the input's folder structure under the target directory
                Path relative = folder.relativize(file);
                String outName = relative.getFileName().toString().replaceAll("(?i)\\.txt$", "") + ".svg";
                Path outPath = targetDir.resolve(relative).resolveSibling(outName);
                ok = createParent(outPath) && convertSingleFile(file.toString(), outPath.toString());
            }
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

    private static boolean createParent(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error: Could not create folder for " + file + " (" + e.getMessage() + ")");
            return false;
        }
    }

    private static void printHelp() {
        System.out.println("""
            Sketch2SVG - Vector Graphics CLI Generator
            
            Usage:
              java -cp target/classes com.sketch2svg.Main [options]
              java -jar Sketch2SVG.jar [options]
            
            Options:
              -i, --input <file>       Path to source sketch .txt file ("-" reads stdin)
              -o, --output <file/dir>  Path for output .svg file or destination folder
                                       ("-" writes stdout; the default when reading stdin)
              -d, --batch <dir>        Batch convert all .txt files inside directory
              -r, --recursive          With -d, also convert subfolders (mirrored under -o)
              -w, --watch              Re-convert whenever the input (or an included file) changes
              -f, --fit                Size the canvas to fit the drawing (overrides the script's canvas)
              -s, --size <px>          Set the image width in pixels (height follows the canvas)
              -g, --grid               Draw a coordinate grid and axes over the result (for placing shapes)
                  --pretty             Indent nested elements
                  --minify             Write everything on one line (smallest file)
              -c, --check              Only report errors and warnings; write nothing
                                       (exit code 1 if any script has errors)
              -h, --help               Display this help message
              -v, --version            Display the version
            """);
    }
}