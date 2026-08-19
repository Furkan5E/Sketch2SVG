# Sketch2SVG

[![Java CI with Maven](https://github.com/Furkan5E/Sketch2SVG/actions/workflows/maven.yml/badge.svg)](https://github.com/Furkan5E/Sketch2SVG/actions/workflows/maven.yml)

A lightweight, zero dependency Java vector graphics engine and CLI tool that converts geometric sketch scripts (`.txt`) into standards-compliant Scalable Vector Graphics (`.svg`).

---

## Features

- **Zero External Runtime Dependencies:** Pure Java implementation utilising native SVG DOM serialization.
- **Rich Geometry Engine:** Supports `Circle`, `Rect`, `Square`, `Line`, `Arc`, `Star`, `RegPolygon`, `Trapezoid`, `Arrow`, and `Text`.
- **Fluent API & Chaining:** Programmatic shape configuration with intuitive builders (`.at()`, `.fill()`, `.stroke()`, `.rotate()`, `.scale()`).
- **Fault-Tolerant Parser:** Robust syntax error diagnostics with line-number reporting.
- **Dynamic CLI:** Flag parsing (`-i`, `-o`, `-d`/`--batch`, `-h`) for terminal automation and batch conversions.
- **Automated CI/CD:** JUnit 5 test suite integrated with GitHub Actions.

---
## Build Instructions
```bash
mvn clean package
```
---

## CLI Usage

### Basic Conversion
```bash
java -cp target/classes com.sketch2svg.Main -i src/main/resources/sketch.txt -o output.svg
```

### Batch Directory Conversion
```bash
java -cp target/classes com.sketch2svg.Main -d ./sketches -o ./dist
```
### Options
```bash
Options:
  -i, --input <file>       Path to source sketch .txt file
  -o, --output <file/dir>  Path for output .svg file or destination folder
  -d, --batch <dir>        Batch convert all .txt files inside directory
  -h, --help               Display this help message
```
---
## Shape	Syntax
```text
Circle	circle <radius> <cx> <cy> [strokeWidth] [strokeRGBA] [fillRGBA]
Rectangle	rect <w> <h> <cx> <cy> [strokeWidth] [strokeRGBA] [fillRGBA]
Square	square <size> <cx> <cy> [strokeWidth] [strokeRGBA] [fillRGBA]
Star	star <points> <outerR> <innerR> <cx> <cy> [strokeWidth] [strokeRGBA] [fillRGBA]
Polygon	ngon <sides> <radius> <cx> <cy> [strokeWidth] [strokeRGBA] [fillRGBA]
Trapezoid	trapezoid <topW> <botW> <h> <cx> <cy> [strokeWidth] [strokeRGBA] [fillRGBA]
Arrow	arrow <length> <width> <cx> <cy> [rot] [strokeWidth] [strokeRGBA] [fillRGBA]
Line	line <x1> <y1> <x2> <y2> [strokeWidth] [strokeRGBA]
Arc	arc <radius> <angle> <length> <cx> <cy> [strokeWidth] [strokeRGBA] [fillRGBA]
Text	text <cx> <cy> <fontSize> "<content>" [strokeWidth] [strokeRGBA] [fillRGBA]
```
```text
Colors are defined using 8-digit hexadecimal RGBA (e.g., ffdc7aff).
```
