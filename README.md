# Sketch2SVG

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/Build-Maven-C71A22?logo=apachemaven)
![JUnit](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5)
![Licence](https://img.shields.io/badge/Licence-MIT-blue)
[![Java CI with Maven](https://github.com/Furkan5E/Sketch2SVG/actions/workflows/maven.yml/badge.svg)](https://github.com/Furkan5E/Sketch2SVG/actions/workflows/maven.yml)

A lightweight, zero-dependency Java vector graphics engine and CLI tool that converts geometric sketch scripts (`.txt`) into standards-compliant Scalable Vector Graphics (`.svg`).

[![Download Latest Release](https://img.shields.io/github/v/release/Furkan5E/Sketch2SVG?style=for-the-badge&label=Download%20.jar&color=success&logo=github)](https://github.com/Furkan5E/Sketch2SVG/releases/latest)

---

## Features

- **Zero External Runtime Dependencies:** Pure Java; the SVG is written directly, no libraries needed.
- **Rich Shape Set:** `circle`, `ellipse`, `rect`, `roundrect`, `square`, `line`, `arc`, `star`, `ngon`, `trapezoid`, `arrow`, `polygon`, `polyline`, `path` (Bezier curves) and `text`.
- **Scripting:** Variables, `{expressions}`, `repeat` loops, `group` blocks, `include` files, gradients, named arguments and named colours.
- **Fluent Java API:** Build drawings in code with `.at()`, `.fill()`, `.stroke()`, `.rotate()`, `.scale()`.
- **Fault-Tolerant Parser:** Bad lines are skipped and reported with their line (and included file); the rest of the sketch still renders.
- **Flexible CLI:** Single file, batch (`-d`, `-r`), stdin/stdout pipelines, `--watch` and `--check` modes.
- **Automated CI/CD:** JUnit 5 test suite and tagged releases via GitHub Actions.

---

## Examples

| Night | Sunflowers |
|:---:|:---:|
| <img src="examples/night.svg" width="300" alt="Cabin by a mountain lake under a full moon"> | <img src="examples/sunflower.svg" width="300" alt="Three sunflowers on a sunny hillside"> |
| [`night.txt`](examples/night.txt): gradients, loops, groups | [`sunflower.txt`](examples/sunflower.txt): loops, expressions, groups |
| **Balloons** | **Sunset** |
| <img src="examples/balloons.svg" width="300" alt="Hot-air balloons over misty mountain ridges at dawn"> | <img src="examples/sunset.svg" width="300" alt="Sailboat and lighthouse in front of the setting sun"> |
| [`balloons.txt`](examples/balloons.txt): `include` files as reusable parts, colour variables | [`sunset.txt`](examples/sunset.txt): gradients, curves, dashes, text options |

The three sunflowers are one drawing repeated at different positions and sizes, and each flower's 16 petals come from a loop:
```text
set petals 16
repeat 3 f
  group at={-62 + f*62},{20 - abs(f - 1) * 24} scale={0.75 - abs(f - 1) * 0.23}
    repeat petals i
      set a {i * 360 / petals}
      circle 12 {28*cos(a)} {28*sin(a)} scale=1.6,0.7 rot=a 1 #e09f3e #ffc300
    end
  end
end
```
Every balloon is the same included file, [`parts/balloon.txt`](examples/parts/balloon.txt), drawn in whatever colours are set before it:
```text
set c1 #e63946
set c2 #fdf0d5
group at=-40,52 scale=1.5
  include parts/balloon.txt
end
```
The examples double as snapshot tests: `SnapshotTest` fails if the output of any `examples/*.txt` stops matching its `.svg`. After an intended change, regenerate them with `java -jar target/Sketch2SVG.jar -d examples`.

---

## Requirements

- **Java 25 or newer** to run the jar (older versions fail with `UnsupportedClassVersionError`).
- Nothing else to build from source: the included Maven wrapper downloads Maven on first use.

## Quick Start

Download `Sketch2SVG.jar` from the [latest release](https://github.com/Furkan5E/Sketch2SVG/releases/latest), then:
```bash
java -jar Sketch2SVG.jar -i sketch.txt            # writes sketch.svg next to it
java -jar Sketch2SVG.jar -i sketch.txt -o art.svg
java -jar Sketch2SVG.jar sketch.txt art.svg       # -i and -o are optional
```

## Build Instructions
```bash
./mvnw clean package        # Windows: mvnw.cmd clean package
```
This runs the tests and produces `target/Sketch2SVG.jar`. With Maven installed, `mvn clean package` does the same.

---

## CLI Usage

The commands below use the jar built by Maven; with a downloaded release, use `Sketch2SVG.jar` instead of `target/Sketch2SVG.jar`. Without packaging, `java -cp target/classes com.sketch2svg.Main` works the same way.

### Basic Conversion
```bash
java -jar target/Sketch2SVG.jar -i examples/night.txt -o output.svg
```
The exit code is 0 on success and 1 on failure. To protect your work, the tool refuses to write the SVG over the sketch itself or over a file it includes.

### Watch Mode
```bash
java -jar target/Sketch2SVG.jar -w -i sketch.txt -o output.svg
```
Rebuilds `output.svg` every time `sketch.txt` or any file it includes is saved. Stop with Ctrl+C.

### Pipelines
```bash
cat sketch.txt | java -jar target/Sketch2SVG.jar -i - > output.svg
```
When writing to stdout, progress messages go to stderr so the output is pure SVG. When reading from stdin, `include` paths resolve against the current folder.

### Batch Directory Conversion
```bash
java -jar target/Sketch2SVG.jar -d ./sketches -o ./dist
```

### Options
```text
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
```

---

## Writing Sketches

A sketch is a text file with one command per line. Lines starting with `#` are comments, and `#` after a command's arguments starts a trailing comment. Put a space after that `#`: a word such as `#add` or `#face` is read as a hex colour.

**Coordinates work like a maths graph, not like plain SVG:**
- `(0, 0)` is the **centre** of the canvas, and **+y points up**.
- The default canvas spans **-100 to 100** on both axes (change it with `canvas`).
- Angles (`rot=`, `arc`, gradients) are in degrees, **anticlockwise**.
- Shapes are positioned by their **centre** (`<cx> <cy>`).

Use `--grid` to overlay the axes and a grid while you work out positions.

```text
# a gold sun in the middle, with a comment at the end
circle 20 0 0 2 black gold   # radius 20, stroke 2
```

## Commands
| Command | Syntax |
|---|---|
| Circle | `circle <radius> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Ellipse | `ellipse <rx> <ry> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Rectangle | `rect <w> <h> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Rounded rectangle | `roundrect <w> <h> <cornerRadius> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Square | `square <size> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Star | `star <points> <outerR> <innerR> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Regular polygon | `ngon <sides> <radius> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Trapezium | `trapezoid <topW> <botW> <h> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Arrow | `arrow <length> <width> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Line | `line <x1> <y1> <x2> <y2> [strokeWidth] [strokeColour]` |
| Arc | `arc <radius> <angle> <length> <cx> <cy> [strokeWidth] [strokeColour] [fillColour]` |
| Text | `text <cx> <cy> <fontSize> "<content>" [strokeWidth] [strokeColour] [fillColour]` |
| Polygon | `polygon <x,y> <x,y> <x,y> ... [strokeWidth] [strokeColour] [fillColour]` |
| Polyline | `polyline <x,y> <x,y> ... [strokeWidth] [strokeColour]` |
| Path | `path M <x,y> L <x,y> Q <c,c> <x,y> C <c,c> <c,c> <x,y> Z [strokeWidth] [strokeColour] [fillColour]` |
| Canvas | `canvas <w> <h> [cx cy]` or `canvas auto [padding]` (default: 200×200 centred on 0,0) |
| Title / description | `title "<text>"`, `desc "<text>"` (accessible name and description, shown by screen readers and as tooltips) |
| Background | `background <colour>` (fills the whole canvas, always drawn first) |

Notes:
- `arc`: `<angle>` is the direction of the arc's middle and `<length>` is how many degrees it spans, so `arc 10 90 180 0 0` is the top half of a circle.
- `text` content needs quotes only when it contains spaces: `text 0 0 12 Hello` works too.
- `text` is filled white with no outline unless you say otherwise, so on a light background give it a colour, e.g. `fill=black`.
- `title` and `desc` without quotes take the rest of the line, including any `#` comment; quote the text if a comment follows.
- `path` uses absolute, uppercase commands only (`M`, `L`, `Q`, `C`, `Z`). Extra points repeat the previous command, and points after `M` continue as lines.

### Colours
| Form | Examples |
|---|---|
| `RRGGBBAA` / `RRGGBB` hex, `#` optional | `ffdc7aff`, `#ffdc7a` |
| `#RGB` / `#RGBA` shorthand (`#` required) | `#fd7`, `#fd78` |
| A name | `black` `white` `gray` `silver` `red` `maroon` `orange` `gold` `yellow` `olive` `lime` `green` `teal` `cyan` `blue` `navy` `purple` `magenta` `pink` `brown` (`grey` also works) |
| No paint | `none`, `transparent` |
| A gradient | any name defined with `gradient` (see below) |

### Variables & expressions
`set <name> <value>` stores a number, colour, `"text"` or the result of an expression. Use a variable by writing its name as an argument (or after `key=`), and put arithmetic inside `{ }` anywhere in an argument.
```text
set gold ffd700ff
set r 5
set d {r * 2}
star 5 {r} 2 -60 {d*3} 0 none gold
polygon 0,0 {d},0 {d},{d} fill=gold
```
Expressions support `+ - * / % ^`, parentheses, `pi`, and `sin cos tan` (degrees), `sqrt abs floor ceil round min max`. Quoted text is never substituted.

### Loops
`repeat <count> [index]` … `end` runs the lines in between `count` times (at most 100,000). The optional index variable counts from 0 and is restored afterwards. Loops can be nested. A problem on a line inside a loop is reported once, with the number of times it happened.
```text
# 12 dots around a circle
repeat 12 i
  circle 3 {40*cos(i*30)} {40*sin(i*30)} 0 none gold
end
```

### Groups
`group [options]` … `end` wraps shapes in an SVG `<g>`. `at=`, `rot=` and `scale=` move, turn and resize the whole group; stroke and fill options become defaults for the shapes inside (a shape's own arguments still win). Groups can be nested and combined with loops.
```text
group at=30,-40 rot=10 stroke=2 fill=brown
  rect 42 28 0 0
  square 8 12 0 fill=gold
end
```

### Gradients
`gradient <name> linear [angle] <colour> <colour> …` or `gradient <name> radial <colour> <colour> …` defines a gradient with evenly spaced colours. Use its name anywhere a colour goes: `fill=`, `stroke=`, the positional colours, `background`, and group styles. The angle is anticlockwise (0 = left to right, 90 = bottom to top) and the gradient stretches over each shape it paints. One exception: a gradient stroke does not show on a perfectly horizontal or vertical `line`, because such a line has no area to stretch over.
```text
gradient sky linear 90 #ff9e6d #0b1020
gradient sun radial #fff3b0 #ffd166 #f77f00
background sky
circle 30 0 20 0 none sun
```

### Include
`include <path>` inlines another sketch file at that point. Paths are relative to the file containing the `include`; wrap them in quotes if they contain spaces. Errors inside included files report the file name, and circular includes are rejected.
```text
include parts/house.txt
include "night sky.txt"
```

### Named arguments
Any shape also accepts named arguments, in any order, after its required parameters. They override the positional `[strokeWidth] [strokeColour] [fillColour]`.

| Argument | Meaning |
|---|---|
| `rot=<deg>` | Rotate anticlockwise around the shape's centre |
| `stroke=<colour>` / `stroke=<width>` | Stroke colour, or stroke width when given a number |
| `stroke-width=<n>` / `sw=<n>` | Stroke width |
| `fill=<colour>` | Fill colour |
| `at=<x,y>` | Move the shape's centre |
| `scale=<s>` / `scale=<sx,sy>` | Multiply the shape's size |
| `dash=<a,b,…>` / `dash=none` | Dashed stroke (alternating dash and gap lengths) |
| `cap=butt\|round\|square` | Stroke line caps |
| `join=miter\|round\|bevel` | Stroke line joins |
| `arrow=end\|start\|both\|none` | Arrowheads on a `line` (drawn in its stroke colour and width) |
| `font=<family>` / `font="Family Name"` | Text font family |
| `weight=normal\|bold\|lighter\|bolder\|100…900`, `bold`, `italic` | Text weight and style |
| `align=left\|center\|right` | Which side of the text sits on its position (default `center`) |

```text
rect 40 20 0 0 rot=30 stroke=2 fill=ffdc7a
text 0 -90 14 "Hello World" rot=-15 fill=navy
```

---

## Java API

Put `Sketch2SVG.jar` on your classpath and build drawings in code:
```java
import com.sketch2svg.parser.Sketch;
import com.sketch2svg.shapes.Circle;
import com.sketch2svg.shapes.Rect;

Sketch sketch = new Sketch()
        .add(new Rect(60, 30, 0, 0).fill("navy").rotate(15))
        .add(new Circle(10, 0, 0).fill("gold").stroke("black").strokeWidth(2));

String svg = sketch.toSVGString();   // or sketch.exportSVG("out.svg")
```
Scripts can be loaded the same way with `sketch.fromFile("sketch.txt")`. The library never prints: script errors and warnings are returned by `sketch.getDiagnostics()`, and `exportSVG` throws an `IOException` if the file can't be written.

---

## Licence

MIT, see [LICENSE](LICENSE).
