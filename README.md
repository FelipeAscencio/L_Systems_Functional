# L-Systems and digital imaging

---

## Introduction

This project is based on the implementation of an L-system, which, from input files in ".sl" format, and turtle-type graphics modeling, generates digital images in ".svg" format.

In particular, what was sought with the development of this program was to create my first project with functional programming, working as much as possible with pure functions (with the exception of the functions of reading and writing the respective files) and working with images in " .svg" so that they can be viewed in almost any web browser.

Additionally, the program allows you to enter through the console (at the time of execution) the number of times you want to implement the L-system logic.

---

# Report

## SRC

In this folder is the "core" program that implements the project logic in "CLOJURE".

## DOC

In this folder are all the base ".sl" files and the images generated ".svg" by the program.

## Explanation of use

To run the program, just enter the containing folder and run: "$ lein run X.sl Y Z.svg", being:

- "X": Name of the ".sl" file from which you want to extract the initial information (it must be inside the "doc" folder).
- "Y": Number of times you want to implement the "System-L" logic.
- "Z": Name of the ".svg" file that you want to obtain with the generated image (it will be saved within the "doc" folder).
