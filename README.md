# Mercury

Mercury: A Unified Framework for Exploring Offloading Tradeoffs in Mobile-Cloud Computing


## Setup (tested on Linux)

1. enter `profiling` and set up the profiling framework according to README.
2. build min-cut solver: `cd solver/pseudo_max; make`
2. build Mercury partition framework: 
	- open `partition` project use eclipse 
	- export `runnable jar` (a pre-built runnable jar is included in `bin`)
3. (optional, only used for debugging) build trace visualization tool: 
	- similar to the above
	- a pre-built jar is also included in `bin`

## Run Mercury

```bash
# print help info
java -jar bin/mercury.jar

# compute optimal partitions for all strategies using an energy model
java -Xmx16g  -jar bin/mercury.jar \
-p config/nativeMethods.txt \
-c config/coLocList.txt  \
-sp bin/pseudo_fifo  \
-runAllSimulation  \
-fieldDep  \
-cloudSpeedup 10.0 \
-n models/energyModel/homewifi \
-i tests/example.trace \
-o out.log

```

### Mercury command line parameters

```
 -c VAL            : Extended annotation for co-located native method.
 -cloudSpeedup N   : Set the speedup of cloud. The default speedup is 10x.
                     Negative number means infinity.
 -disInline        : Disable the inlining optimization.
 -fieldDep         : Enable field level dependency.
 -guiThreads VAL   : Add additional gui threads in addition to the first one.
 -i VAL            : Specify the path of the trace file.
 -maxflowSolver N  : set the mincut-maxflow solver to use: 1. pseudo
                     flow(default) 2. gurobi:lp
 -n VAL            : Specify the network model.
 -o VAL            : The path to the result file.
 -p VAL            : Specify the annotation for each native method.
 -po VAL           : The output for the list of pinned methods.
 -runAll           : Run the first three potential study experiments and all
                     the simulation experiment
 -runAllSimulation : Run all the simulations.
 -runAllStudy      : Run all the studies.
 -runSimulation N  : Run the specified simulation:
                     1. Uni-directional stateless.
                     2. Bi-directional stateless.
                     3. Uni-directional stateful.
                     4. Bi-directional stateful.
 -runStudy N       : Run the specified study without communication cost:
                      1. Uni-directional offloading.
                     2. Bi-directional offloading.
                     3. Ideal case.
 -runtimeScale N   : Scale the running time by Nx.
 -sp VAL           : Specify the path to pseudo flow solver.
 -unpinGui         : Unpin the gui thread.
```
