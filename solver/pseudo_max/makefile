####################################################################################
#                                COMPILER FLAGS                                    #
####################################################################################
#                                                                                  #
# -DSTATS        : Display operation counts.                                       #
# -DDISPLAY_CUT  : Output includes the nodes in the source set of the minimum cut. #
# -DDISPLAY_FLOW : Displays flow values for each arc.                              #
#                                                                                  #
####################################################################################

CFLAGS=-O4 -Wall
BINDIR=bin
CC=gcc

all: ${BINDIR}/ pseudo_lifo ${BINDIR}/pseudo_fifo ${BINDIR}/pseudo_lo_fifo ${BINDIR}/pseudo_lo_lifo

pseudo_lifo: ${BINDIR}/pseudo_lifo

pseudo_fifo: ${BINDIR}/pseudo_fifo

pseudo_lo_fifo: ${BINDIR}/pseudo_lo_fifo

pseudo_lo_lifo: ${BINDIR}/pseudo_lo_lifo

${BINDIR}/pseudo_lifo:
	${CC} ${CFLAGS} -DLIFO_BUCKET src/3.23/pseudo.c -o bin/pseudo_lifo

${BINDIR}/pseudo_fifo:
	${CC} ${CFLAGS} -DFIFO_BUCKET src/3.23/pseudo.c -o bin/pseudo_fifo

${BINDIR}/pseudo_lo_fifo:
	${CC} ${CFLAGS} -DFIFO_BUCKET -DLOWEST_LABEL src/3.23/pseudo.c -o bin/pseudo_lo_fifo

${BINDIR}/pseudo_lo_lifo:
	${CC} ${CFLAGS} -DLIFO_BUCKET -DLOWEST_LABEL src/3.23/pseudo.c -o bin/pseudo_lo_lifo

clean:
	rm -f ${BINDIR}/*
