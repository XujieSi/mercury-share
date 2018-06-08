package edu.gatech.traceprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.gatech.traceprocessor.offloadingalgorithms.solver.OffloadingAlgorithm;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut.MinCutOffloadingAlgorithm1;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut.MinCutOffloadingAlgorithm2;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut.MinCutOffloadingAlgorithm3;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut.MinCutOffloadingAlgorithm4;
import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Utils;
import edu.gatech.traceprocessor.utils.ValueReverseComparator;

public class Batch {
	public final static String SEP = "\t";
	
	@Option(name="-cut", required=true,usage="Specify cutting boundary")
	private String cuttingBoundary;
	
	@Option(name="-cutprefix", usage="Specify cutting boundary by prefix matching")	
	public static String cutPrefix;
	
	@Option(name="-i", required=true,usage="Specify the path of the trace file.")
	private String tracePath;
	
	@Option(name="-runAll", usage = "Run the first three potential study experiments and all the simulation experiment")
	private boolean runAll = false;
	
	@Option(name="-runStudy", usage = "Run the specified study without communication cost:\n "
			+ "1. Uni-directional offloading.\n"
			+ "2. Bi-directional offloading.\n"
			+ "3. Ideal case.\n")
//			+ "1. Uni-directional stateless offloading.\n"
//			+ "2. Bi-directional stateless offloading.\n"
//			+ "3. Uni-directional stateful offloading.\n"
//			+ "4. Bi-directional stateful offloading.\n"
//			+ "5. Nested bi-directional offloading.\n")
	private int studyToRun = -1;
	
	@Option(name="-runAllStudy", usage = "Run all the studies.")
	private boolean runAllStudy = false;
	
	@Option(name="-runAllSimulation", usage = "Run all the simulations.")
	private boolean runAllSimulation = false;
	
	@Option(name="-runSimulation", usage = "Run the specified simulation:\n"
			+ "1. Uni-directional stateless.\n"
			+ "2. Bi-directional stateless.\n"
			+ "3. Uni-directional stateful.\n"
			+ "4. Bi-directional stateful.")
	private int simulationToRun = -1;
	
	@Option(name="-n", required = true, usage="Specify the network model.")
	private String networkPath = null;
	
	@Option(name="-p", required = true, usage="Specify the annotation for each native method.")
	private String nativePath = null;
	
	@Option(name="-c", required = true, usage="Extended annotation for co-located native method.")
	private String colocPath = null;
	
	@Option(name="-po", usage="The output for the list of pinned methods.")
	private String outPinPath = null;
	
	@Option(name="-o", required=true, usage="The path to the result file.")
	private String outPath;
	
//	@Option(name="-cl", usage="Collapse the library methods.")
//	private boolean collapseLibrary;
	
//	@Option(name="-t", usage="Tune gurobi's parameters.")
//	private boolean ifTune;
	
	@Option(name="-sp", required=true, usage="Specify the path to pseudo flow solver.")
	private String spPath;
	
	@Option(name="-disInline", usage="Disable the inlining optimization.")
	private boolean disInline = false;
	
//	@Option(name="-device", usage="set the hardware model: 1. kvm-qemu(default) 2. nexus 7")
//	private int device = 1;
	
	@Option(name="-maxflowSolver", usage="set the mincut-maxflow solver to use: 1. pseudo flow(default) 2. gurobi:lp")
	private int flowSolver = Configuration.PSUEDO_FLOW;
	
	@Option(name="-runtimeScale", usage="Scale the running time by Nx.")
	private double runTimeScale = 10.0;
	
	@Option(name="-cloudSpeedup", usage="Set the speedup of cloud. The default speedup is 10x. Negative number means infinity.")
	private double cloudSpeedup = 10.0;
	
	@Option(name="-unpinGui", usage="Unpin the gui thread.")
	private boolean unpinGui = false;
	
	@Option(name="-guiThreads", usage="Add additional gui threads in addition to the first one.")
	private String guiTids = null;
	
	@Option(name="-fieldDep", usage="Enable field level dependency.")
	private boolean enableFieldDep = false;

	public static void main(String[] args) throws FileNotFoundException{
		new Batch().doMain(args);
	}
	
	private void doMain(String[] args) throws FileNotFoundException{
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			System.exit(1);
		}
		PrintWriter outPrinter = new PrintWriter(new File(outPath));
		Configuration.MINCUT_SOLVER = flowSolver;
		Configuration.dataLevel = Configuration.OBJ_LEVEL;
		if(this.enableFieldDep)
			Configuration.dataLevel = Configuration.FLD_LEVEL;
		Configuration.loadLocalMethods(this.nativePath);
		Configuration.loadCoLocMethods(this.colocPath);
		Configuration.loadNetworkModel(this.networkPath);
		Configuration.runningTimeScale = this.runTimeScale;
		if(this.cloudSpeedup < 0)
			Configuration.cloudSpeedupFactor = Double.POSITIVE_INFINITY;
		else
			Configuration.cloudSpeedupFactor = this.cloudSpeedup;
		Configuration.pinGUI = !this.unpinGui;
//		Configuration.performTune = ifTune;
		Configuration.PSEUDO_FLOW_PATH = this.spPath;
		if(this.guiTids != null){
			Configuration.guiThreads = new HashSet<Integer>();
			for(String token : this.guiTids.trim().split(","))
				Configuration.guiThreads.add(Integer.parseInt(token));
		}
//		if(device == 1){
//			Configuration.instrTime = Configuration.KVM_TIME;
//		}
//		else if(device == 2)
//			Configuration.instrTime = Configuration.NEXUS_INST_TIME;
//		else{
//			Utils.printLogWithTime("Unrecognized device setting, set to kvm-qemu by default.");
//			Configuration.instrTime = Configuration.KVM_TIME;
//		}
		Program p = null;
		this.printHeader(outPrinter);
		if(this.runAll || this.runAllStudy || this.studyToRun > 0){
			Utils.printLogWithTime("Start loading the trace for the potential study.");
			Program p1 = new Program();
			p = p1;
			p1.load(this.tracePath, this.cuttingBoundary);
			Utils.printLogWithTime("Loading trace is done, method count: "+p1.getMethCount());
			
			p1.calculateInOutSetSize();
			Utils.printLogWithTime("Calculate In Out Set Size is done.");
			
			p1.updatePinAndColocMethods();
			Utils.printLogWithTime("Update Pin & Co-loc are done.");
			
			p1.mergeSCCs();
			Utils.printLogWithTime("Merge SCC is done.");
			
			
			p1.updatePinningBasedOnColoc();
			Utils.printLogWithTime("update Pin base is done.");
			
//			if(this.collapseLibrary)
//				p1.collapseLibrary();
			Configuration.ignoreNetworkLatency = true;
			Utils.printLogWithTime("Method count: "+p1.getMethCount());
			if(!this.disInline){
				p1.inlineOptimizeNoNetworkLatency();
				Utils.printLogWithTime("Method count after inlining optimization: "+p1.getMethCount());
			}
//			if(this.runAll || this.studyToRun == 1){
//				OffloadingAlgorithm algo1 = this.getAlgo(1, p1);
//				Utils.printLogWithTime("Start running the uni-directional stateless offloading study without communication cost.");
//				algo1.optimize();
//				this.printResult("uni-directional stateless study", algo1, outPrinter);
//				outPrinter.flush();
//			}
//			if(this.runAll || this.studyToRun == 2){
//				OffloadingAlgorithm algo2 = this.getAlgo(2, p1);
//				Utils.printLogWithTime("Start running the bi-directional stateless offloading study without communication cost.");
//				algo2.optimize();
//				this.printResult("bi-directional stateless study", algo2, outPrinter);
//				outPrinter.flush();
//			}
			if(this.runAll || this.runAllStudy || this.studyToRun == 1){
				OffloadingAlgorithm algo3 = this.getAlgo(3, p1);
				Utils.printLogWithTime("Start running the uni-directional stateful offloading study without communication cost.");
				algo3.optimize();
				this.printResult("uni-directional study", algo3, outPrinter);
				outPrinter.flush();
			}
			if(this.runAll || this.runAllStudy || this.studyToRun == 2){
				OffloadingAlgorithm algo4 = this.getAlgo(4, p1);
				Utils.printLogWithTime("Start running the bi-directional stateful offloading study without communication cost.");
				algo4.optimize();
				this.printResult("bi-directional study", algo4, outPrinter);
				outPrinter.flush();
			}
			if(this.runAll || this.runAllStudy || this.studyToRun == 3){
//				Utils.printLog("Start running the nested bi-directional offloading study without communication cost. This is equivalent with "
//						+ "comparing pinned native method time and other.");
//				Utils.printLogWithTime("Total time: "+p1.getTotalTime()*Configuration.runningTimeScale+", pinned time: "+p1.getTotalPinnedNativeTime()*Configuration.runningTimeScale+
//						", gui time: "+p1.getUITime()*Configuration.runningTimeScale+", ui pinned time: "+p1.getUIPinnedNativeTime()*Configuration.runningTimeScale);
//				outPrinter.println("Result for nested bi-directional offloading study: ");
//				double offloadedTime = (p1.getTotalTime()-p1.getTotalPinnedNativeTime())/Configuration.cloudSpeedupFactor+p1.getTotalPinnedNativeTime();
//				double uiOffloadedTime = (p1.getUITime() - p1.getUIPinnedNativeTime())/Configuration.cloudSpeedupFactor+p1.getUIPinnedNativeTime();
//				outPrinter.println("Total local time: "+p1.getTotalTime()*Configuration.runningTimeScale+", gui local time: "+p1.getUITime()*Configuration.runningTimeScale+
//						", total offloaded time: " + offloadedTime*Configuration.runningTimeScale+", UI offloaedTime "+uiOffloadedTime*Configuration.runningTimeScale);
//				outPrinter.println("Speedup: "+(p1.getTotalTime()/offloadedTime)+", Speedup(without GUI): "+((p1.getTotalTime() - p1.getUITime())/(offloadedTime - uiOffloadedTime)));	
//				outPrinter.flush();
				Utils.printLogWithTime("Start calculating the offloadable ratio.");
				this.printResultRatio("oracle study", p1, outPrinter);
			}
		}
		
		if(this.runAll || this.runAllSimulation || this.simulationToRun > 0){
			Utils.printLogWithTime("Start loading the trace for the simulation.");
			Program p2 = new Program();
			p = p2;
			p2.load(this.tracePath, this.cuttingBoundary);
			Utils.printLogWithTime("Loading trace is done. method count: "+p2.getMethCount());
			
			// dbg sxj
			Set<String> names = p2.getAllUniqMethodNames();
			Set<String> excludeNames = new HashSet<String>();
			for(String x : names){
				if(x.contains("java/")) continue;
				if(x.contains("android/")) continue;
				if(x.contains("davik/")) continue;
				excludeNames.add(x);
			}
			Utils.printLogWithTime("Uniq method count (exclude android/java lib):" + excludeNames.size() + "\n" + excludeNames);
			
			
			p2.updatePinAndColocMethods();
			Utils.printLogWithTime("Update Pin & Co-loc are done.");
			
			p2.mergeSCCs();
			Utils.printLogWithTime("Merge SCC is done.");
			
			p2.updatePinningBasedOnColoc();
			Utils.printLogWithTime("update Pin base on Co-location is done.");
			
			
//			if(this.collapseLibrary){
			
//				Utils.printLogWithTime("Method count before collapsing the library: "+p2.getMethCount());
//				p2.collapseLibrary();
//				Utils.printLogWithTime("Method count after collapsing the library: "+p2.getMethCount());
//			}
			p2.calculateInOutSetSize();
			Utils.printLogWithTime("compute In Out set size is done.");
			
			
			Configuration.ignoreNetworkLatency = false;
			boolean ifStateful;
			if(!this.runAll && !this.runAllSimulation &&(this.simulationToRun == 1 || this.simulationToRun == 2))
				ifStateful = false;
			else
				ifStateful = true;
			boolean ifNested;
			if(this.runAll || this.runAllSimulation || (this.simulationToRun == 2 || this.simulationToRun == 4))
				ifNested = true;
			else
				ifNested = false;
			Utils.printLogWithTime("Method count: "+p2.getMethCount());
			if(!this.disInline){
				p2.inlineOptimize(ifStateful, ifNested);
				Utils.printLogWithTime("Method count after inlining optimization: "+p2.getMethCount());
			}
			
			if(this.runAll || this.runAllSimulation || this.simulationToRun == 1){
				OffloadingAlgorithm algo1 = this.getAlgo(1, p2);
				Utils.printLogWithTime("Start running the uni-directional stateless simulation.");
				algo1.optimize();
				this.printResult("uni-stateless simulation", algo1, outPrinter);
				outPrinter.flush();
			}
			if(this.runAll || this.runAllSimulation || this.simulationToRun == 2){
				OffloadingAlgorithm algo2 = this.getAlgo(2, p2);
				Utils.printLogWithTime("Start running the bi-directional stateless offloading simulation.");
				algo2.optimize();
				this.printResult("bi-stateless simulation", algo2, outPrinter);
				outPrinter.flush();
			}
			if(this.runAll || this.runAllSimulation || this.simulationToRun == 3){
				OffloadingAlgorithm algo3 = this.getAlgo(3, p2);
				Utils.printLogWithTime("Start running the uni-directional stateful offloading simulation.");
				algo3.optimize();
				this.printResult("uni-stateful simulation", algo3, outPrinter);
				outPrinter.flush();
			}
			if(this.runAll || this.runAllSimulation || this.simulationToRun == 4){
				OffloadingAlgorithm algo4 = this.getAlgo(4, p2);
				Utils.printLogWithTime("Start running the bi-directional stateful offloading simulation.");
				algo4.optimize();
				this.printResult("bi-stateful simulation", algo4, outPrinter);
				outPrinter.flush();
			}
		}

		if(p != null && outPinPath != null){
			writePinList(p,outPinPath, p.getPinnedMethods());
		}
		outPrinter.flush();
		outPrinter.close();
	}

	public static void writePinList(Program p,String path, Set<Method> pinList) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(path));
		Map<String,Long> methTimeMap = new HashMap<String,Long>();
		for(Method m : pinList){
			if(!m.isNative())
				continue;
			if(Configuration.pinGUI){
				if(p.getUIThreads().contains(p.getThread(m.getThreadID())))
					continue;
			}
			if(Configuration.getMethodPinTag(m.methName()) != Method.UNDEF)
				continue;
			Long time = methTimeMap.get(m.methName());
			if(time == null)
				time = 0L;
			methTimeMap.put(m.methName(), time+m.getInclusiveTime());
		}
		List<Map.Entry<String,Long>> entryList = new ArrayList<Map.Entry<String,Long>>(methTimeMap.entrySet());
		Collections.sort(entryList, new ValueReverseComparator());
		for(Map.Entry<String, Long> entry : entryList)
			pw.println(entry.getKey()+"\t\t\t\t"+entry.getValue());
		pw.flush();
		pw.close();
	}
	
	private void printResult(String tag, OffloadingAlgorithm algo, PrintWriter out){
		out.print(tag);
		out.print(SEP);
		double totalTime = algo.getLocalTime();
		double guiTime = algo.getGuiLocalTime();
		//GUI time
		out.print(guiTime);
		out.print(SEP);
		//Total local time excluding GUI
		out.print(totalTime - guiTime);
		out.print(SEP);
		//Mobile time 
		out.print(algo.getMobileTime() - guiTime);
		out.print(SEP);
		//Cloud time
		out.print(algo.getCloudTime());
		out.print(SEP);
		//Transfer time
		out.print(algo.getTranferTime());
		out.print(SEP);
		List<Integer> sizes = algo.getOffSizes();
		out.print(sizes.size());
		for(int s : sizes)
			out.print(" "+s);
		out.print(SEP);
		out.print(algo.getMaxDepth());
		out.println();
	}
	
	private void printHeader(PrintWriter out){
		out.print(SEP);
		out.print(SEP);
		//gui time
		out.print("GUI time");
		out.print(SEP);
		//total time excluding gui
		out.print("Total time");
		out.print(SEP);
		//mobile time after offloading
		out.print("Mobile time");
		out.print(SEP);
		// cloud time after offloading
		out.print("Cloud time");
		out.print(SEP);
		// transfer time
		out.print("Transfer time");
		out.print(SEP);
		out.print("Transfer Data packet sizes");
		out.print(SEP);
		out.print("Max Depth of offloading");
		out.println();	
	}
	private void printResultRatio(String tag, Program p, PrintWriter out){
		double totalTime = p.getTotalTime();
		double guiTime = p.getUITime();
		double pinTime = p.getTotalPinnedNativeTime();
		out.print(tag);
		out.print(SEP);
		//gui time
		out.print(guiTime*Configuration.runningTimeScale);
		out.print(SEP);
		//total time excluding gui
		out.print((totalTime - guiTime)*Configuration.runningTimeScale);
		out.print(SEP);
		//mobile time after offloading
		out.print((pinTime - guiTime) * Configuration.runningTimeScale);
		out.print(SEP);
		// cloud time after offloading
		out.print((totalTime - pinTime) * Configuration.runningTimeScale / Configuration.cloudSpeedupFactor);
		out.print(SEP);
		// transfer time
		out.print("0");
		out.print(SEP);
		out.print("0");
		out.print(SEP);
		out.print("0");
		out.println();
	}

	private OffloadingAlgorithm getAlgo(int i, Program p){
		switch (i){
		case 1:
			return new MinCutOffloadingAlgorithm1(p);
		case 2:
			return new MinCutOffloadingAlgorithm2(p);
		case 3:
			return new MinCutOffloadingAlgorithm3(p);
		case 4:
			return new MinCutOffloadingAlgorithm4(p);
		}
		throw new RuntimeException("Unkown offloading algorithm: "+i);
	}
	
//	private OffloadingAlgorithm getAlgo(int i, Program p){
//		SVariable.clean();
//		ILPOffloadingAlgorithm.clean();
//		switch (i){
//		case 1:
//			return new ILPOffloadingAlgorithm1(p);
//		case 2:
//			return new ILPOffloadingAlgorithm2(p);
//		case 3:
//			return new ILPOffloadingAlgorithm3(p);
//		case 4:
//			return new ILPOffloadingAlgorithm4(p);
//		}
//		throw new RuntimeException("Unkown offloading algorithm: "+i);
//	}

}
