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


#
# Method run in class Foo:
#  (run has 2 arg(s) and 1 local(s))
#

Foo.run:
	# Prologue
	addi $sp, $sp, -28	# push new stack frame
	sw   $ra, 0($sp)	# save return address
	sw   $gp, 4($sp)	# save static link
	sw   $a0, 8($sp)	# store arg 1
	sw   $a1, 12($sp)	# store arg 2
	sw   $8, 20($sp)	# save $8
	sw   $9, 24($sp)	# save $9
	#
	# CMT: result = ... [line 10]
	#
	li  $8, 8
	add $8, $sp, $8
	lw  $8, 0($8)
	li  $9, 12
	add $9, $sp, $9
	lw  $9, 0($9)
	add $8, $8, $9
	li  $9, 16
	add $9, $sp, $9
	sw  $8, 0($9)
	# setting up to call print
	li  $a0, 16
	add $a0, $sp, $a0
	lw  $a0, 0($a0)
	jal print
	# done with call to print
	li  $v0, 16
	add $v0, $sp, $v0
	lw  $v0, 0($v0)
	# Epilogue
	lw   $8, 20($sp)	# restore $8
	lw   $9, 24($sp)	# restore $9
	lw   $ra, 0($sp)	# reload return address
	addi $sp, $sp, 28	# pop stack frame
	jr   $ra		# jump back to caller

#
# The main method.  Note that main does not save / restore any
# registers except $ra.  This is rude, but we're assuming that
# our caller doesn't have any state to be protected since we're
# the first procedure invoked.
#

main:
	sw  $ra, 0($sp)
	# setting up to call print (nested)
	# setting up to call Foo.run
	li  $a0, 5
	li  $a1, 10
	li  $gp, 0
	move $a0, $a0
	move $a1, $a1
	jal Foo.run
	# done with call to Foo.run
	move $a0, $v0
	jal print
	# done with call to print
	lw  $ra, 0($sp)
	jr $ra
