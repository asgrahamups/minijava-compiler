#----FINAL MIPS OUTPUT -----
#
# MIPS assembly header for MiniJava code.  Defines the "built-in"
# functions, and does initialization for the memory allocator.  It
# then jumps to a procedure named "main" (so there had better be one),
# and exits once main returns.
#
        .data
        .align 2
_mem:   .space 102400       # 100K of heap space
_next:  .word 0
_nl:    .asciiz "\n"
_exit:  .asciiz "Exited with value "

        .text

        la   $s0, _mem 
        la   $s1, _next  
        sw   $s0, 0($s1)    # Set up ptr to start of malloc pool
        jal  main           # Jump to start of Minijava program
        nop
        li    $v0 10
        syscall             # syscall 10 (exit)  
        
#
# Implements the "built-in" print.  It expects a single integer
# argument to be passed in $a0.
#
print:  
        li   $v0, 1         # Specify the "print int" syscall
        syscall             # Arg is in $a0, so just do call
        la   $a0, _nl       # Load addr of \n string
        li   $v0, 4         # Specify the "print string" syscall
        syscall             # Print the string
        jr   $ra        
        
#
# Implements the "built-in" exit.  It expects a single integer
# argument to be passed in $a0.
#
exit:  
        move $s0, $a0       # Store the integer arg 
        la   $a0, _exit     # Load addr of "exit" string
        li   $v0, 4         # Specify the "print string" syscall
        syscall             # Print the string
        move $a0, $s0       # Set up the integer arg for printing
        li   $v0, 1         # Specify the "print int" syscall
        syscall             # Print the integer
        la   $a0, _nl       # Load addr of \n string
        li   $v0, 4         # Specify the "print string" syscall
        syscall             # Print the \n
        li    $v0 10        # Specify the MIPS exit syscall
        syscall             # exit

#
# Implements a quick and dirty "malloc" that draws from a fixed-size
# pool of memory, and never frees or reallocates memory.  Expects a
# single integer argument to be passed in $a0.  Written so that it
# uses only $a and $v registers and therefore needs no stack frame.
# (Look into into sbrk as a better way to allocate memory.)
#
malloc: 
        addi $a0, $a0, 3    # Round up to next word boundary
        srl  $a0, $a0, 2    # Remove lowest two bits by shifting
        sll  $a0, $a0, 2    #  right and then back to left
        la   $a1, _next     # Global pointing to free memory
        lw   $v0, 0($a1)    # Load its contents
        add  $v1, $v0, $a0  # Bump up to account for this chunk
        sw   $v1, 0($a1)    # Store new value back in global
        jr   $ra


Foo.run:
	#Prologue
	addi $sp, $sp, -16
	sw $ra, 0($sp)
	sw $gp, 4($sp)
	sw $8, 12($sp)

	#
	# dummy=[line 21]
	#
	#Setting up call to Foo.print
	li $4,50
	#
	# this
	#
	li $gp, 4
	add $8 $sp, $gp
	lw $8 0($8)
	move $gp, $8
	jal Foo.print
	#Done with call to Foo.print
	li $8,8
	add $8 $sp $8
	sw $v0 0($8)
	#Setting up call to Foo.print
	li $4,100
	#
	# this
	#
	li $gp, 4
	add $8 $sp, $gp
	lw $8 0($8)
	move $gp, $8
	jal Foo.print
	#Done with call to Foo.print
	#Epilogue
	sw $8, 12($sp)
	lw $ra, 0($sp)
	addi $sp, $sp, 16
	jr $ra
	#End of Epilogue

Foo.print:
	#Prologue
	addi $sp, $sp, -16
	sw $ra, 0($sp)
	sw $gp, 4($sp)
	sw $4, 8($sp)
	sw $8, 12($sp)

	#Setting up call to print
	li $8,8
	add $8 $sp $8
	lw $4, 0($8)
	jal print
	#Done with call to print
	li $8,0
	#Epilogue
	sw $8, 12($sp)
	lw $ra, 0($sp)
	addi $sp, $sp, 16
	jr $ra
	#End of Epilogue

main:
	sw $ra 0($sp)
	#Setting up call to print (nested)
	li $gp,0
	jal Foo.run
	#Done with call to Foo.run
	move $4, $v0
	jal print
	#Done with call to print
	lw $ra, 0($sp)
	jr $ra
