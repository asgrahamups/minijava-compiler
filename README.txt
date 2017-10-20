A compiler for mini-java (http://www.cambridge.org/us/features/052182060X/). Instead of outputing bytecode, this compiler compiles mini-java programs into full MIPS assembly. To use the compiler, simply clone the "WorkDir" folder,
compile all java programs in all folders, and use the command

java compiler.RunCompiler PROGRAM_FILE_PATH mips -> ASSEMBLY_FILE_NAME.txt

where PROGRAM_FILE_PATH is the path to your mini-java program and ASSEMBLY_FILE_NAME is the name of the file you want your assembly code
to be created. You can run this assembly using the MARS simulator (http://courses.missouristate.edu/KenVollmar/MARS/) to analyze your MIPS code and to make optimizations if you so desire.
