package edu.gatech.traceprocessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.gatech.traceprocessor.offloadingalgorithms.solver.OffloadingAlgorithm;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm1;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm2;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm3;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm4;
import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Utils;
import edu.gatech.traceprocessor.utils.ValueReverseComparator;

public class Main {
	static String tracePath;
	static String outPath;
	static String pinMethodPath;
	static String colMethodPath;
	static String colLinePath;
	static String networkPath;
	static String pinOutPath;
	static boolean ifFixCon;
	static boolean ifDisCol;
	static int algorithm;
	
	public static void main(String[] args) throws FileNotFoundException {
		tracePath = System.getProperty(Utils.TRACE_PATH);
		outPath = System.getProperty(Utils.PLAIN_TRACE_OUT_PATH);
		pinMethodPath = System.getProperty(Utils.PIN_METHOD_LIST);
		colMethodPath = System.getProperty(Utils.COLOC_METHOD_LIST);
		colLinePath = System.getProperty(Utils.COLOC_LINE_LIST);
		networkPath = System.getProperty(Utils.NETWORK_MODEL);
		pinOutPath = System.getProperty(Utils.PIN_METHOD_OUT,Utils.DEF_PIN_OUT);
		algorithm = Integer.getInteger(Utils.ALGORITHM, 1);
		ifFixCon = Boolean.getBoolean(Utils.IF_FIXCONCUR);
		ifDisCol = Boolean.getBoolean(Utils.DIS_COL);
		
		Configuration.dataLevel = Integer.getInteger(Utils.DEP_LEVEL, Configuration.OBJ_LEVEL);
		Utils.printConfig();
		Configuration.loadLocalMethods(pinMethodPath);
		if(colMethodPath != null)
			Configuration.loadCoLocMethods(colMethodPath);
		Configuration.loadNetworkModel(networkPath);
//		if(colLinePath != null)
//			Configuration.loadCoLocMethodInstances(colLinePath);
		Program program = new Program();
		Utils.printLogWithTime("Loading the trace.");
		program.load(tracePath);
		boolean ifStateful;
		if(algorithm == 1 || algorithm == 2)
			ifStateful = false;
		else
			ifStateful = true;
		boolean ifNested;
		if(algorithm == 2 || algorithm == 4)
			ifNested = true;
		else
			ifNested = false;
		Utils.printLogWithTime("Calculating the input and output set for each method.");
		program.calculateInOutSet();
		Utils.printLogWithTime("Pinning and colocating methods.");
		program.updatePinAndColocMethods();
		if(ifFixCon && !ifDisCol)
			program.fixConcurrencyWithColoc(false);
		program.mergeSCCs();
	
		if(ifDisCol)
			program.cleanSCCs();
		
		writePinList(pinOutPath,program.getPinnedMethods());
		
		Set<Set<Method>> colocSets = program.getColocSet();
		Utils.printLogWithTime("Colocated: methhods");
		for(Set<Method> colSet : colocSets){
			TreeSet<Method> sortedSet = new TreeSet<Method>(colSet);
			Utils.printLogWithTime("********************");
			for(Method m : sortedSet)
				Utils.printLogWithTime(m.toString());
		}

		Utils.printLogWithTime("Inlining methods.");
		program.inlineOptimize(ifStateful, ifNested);
		OffloadingAlgorithm offAlgorithm = null;
		switch(algorithm){
		case 1:
			System.out.println("Running uni-directional stateless offloading algorithm");
			offAlgorithm = new ILPOffloadingAlgorithm1(program);
			break;
		case 2:
			System.out.println("Running bi-directional stateless offloading algorithm");
			offAlgorithm = new ILPOffloadingAlgorithm2(program);
			break;
		case 3:
			System.out.println("Running uni-directional stateful offloading algorithm");
			offAlgorithm = new ILPOffloadingAlgorithm3(program);
			break;
		case 4:
			System.out.println("Running bi-directional stateful offloading algorithm");
			offAlgorithm = new ILPOffloadingAlgorithm4(program);
			break;
		default:
			throw new RuntimeException("Unsupported offloading algorithm: " + algorithm);	
		}
		Utils.printLogWithTime("Start optimizing.");
		offAlgorithm.optimize();
		Utils.printLogWithTime("Terminated.");
	}
	
	private static void writePinList(String path, Set<Method> pinList) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(path));
		Map<String,Long> methTimeMap = new HashMap<String,Long>();
		for(Method m : pinList){
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
	
}
