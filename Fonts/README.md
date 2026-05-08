# Fonts

The fonts are ASCII files, one for standard resolution and one for high resolution.  These are used as input for all the other programs.  
  
* The ScreenDump directory has the Java code to take the files and create a test pattern of the fonts (with a blue border showing the blank pixels needed to pad to 800x600)
* The CharacterROM directory has the Java code to take the files and create the Intel Hex file to burn into the AT27C1024­45PU character ROM
* The FontDiagram directory has the Java code to take the files and create a test pattern of the fonts showing the actual bit patterns

The font files need to be in each of the directories to allow the code to work. Each directory has a `COMPILE.BAT` and a `RUN.BAT` that assumes that `java` and `javac` are
on your current PATH.  None of the code requires any external libraries.
