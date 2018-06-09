package edu.gatech.traceprocessor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import edu.gatech.traceprocessor.utils.Pair;
import edu.gatech.traceprocessor.utils.Trio;
import edu.gatech.traceprocessor.utils.Utils;
import edu.gatech.traceprocessor.utils.VarBitSet;


/*
 * 1. Assumes that function entries are always visible except for the first function called in a thread.
 * 2. Inlines the call to a function if the function exit is missing.
 * 3. If a read from a variable is not preceded by a write to that variable, the read is ignored.
 * 4. If an alloc statement is seen, the function containing alloc is considered to be the writer for the
 * 	  allocated variable.
 * 5. If a variable is written but never read, it not added to the output set of the writer function.
 * 6. Variables written and read by the same function (not read by any other function) are ignored. The code to
 *    ignore such variables is triggered in the Function class when adding a new variable.
 * 7. If a variable is read more than once in a function, it is treated as being read once.
 * 8. If alloc statements are ignored, then variable size is computed from the first write statement encountered for that variable.
 *    In the current trace, at each read and write, we record the size of the entire object and not the field.
 * 9. Current granularity of both, shipping and data dependence, is at the level of objects and not fields. 
 * 10.Only the methods mentioned in the local methods files are pinned to the device. The callers and callees 
 * 	  of such pinned methods are not pinned at this juncture.
 * 11.The root function of each thread is always considered un-offloadable.
 */
public class TraceProcessor {
	public static final String ROOT = "root";
	public static final String FORESTROOT = "forestroot";

	/*
	 * The below defines the type of functions regarding offloading
	 */
	public static final int STATELESS = 0;
	public static final int STATEFULL = 1;
	public static final int PINNED = 2;
	/*
	 * end
	 */
	/*
	 * The read/write type constants
	 */
	public static final int OBJ_JNI = 3;
	public static final int OBJ_JNI_HOLD_ARRAY = 5;
	public static final int OBJ_JNI_REL_ARRAY = 6;
	/*
	 * end
	 */
	
	//co-located SCC to address-size-referenceCount
	public Map<Set<String>,Map<String,Pair<Integer,Integer>>> jniArrayMap;
	
	public static Map<Character,Integer> primSizeMap;
	static{
		primSizeMap = new HashMap<Character,Integer>();
		primSizeMap.put('B', 1);
		primSizeMap.put('C', 2);
		primSizeMap.put('D', 8);
		primSizeMap.put('F', 4);
		primSizeMap.put('I', 4);
		primSizeMap.put('J', 8);
		primSizeMap.put('S', 2);
		primSizeMap.put('V', 0);
		primSizeMap.put('Z', 1);
		primSizeMap.put('L', 4);
	}
	
	long addrSeed = 0;
	
	Function forest;
	VarBitSet varTable;
	Map<String,Integer> localFuncMap;
	Set<Set<Function>> coLocFunctions;
	Set<Set<String>> coLocFunctionNames;
	Vector<Long>[] stacks;
	Function[] threads;
	Hashtable<Trio<String,Integer,Integer>, VarEntry> variables;
	Hashtable<Integer, Integer> threadMap;
	long funAddress = 0;
//	int id = 0;
	int mapP = 0;
	String logFile;
	String localFile;
	String coLocFile;
	FunctionFactory fFactory;
	
	double [] totalDelays;
	int logFunCount = 0;
	long logStartTime =0;
	long logTotalTime = 0;
	long logExitTime = 0;
	boolean nested = false;
	boolean stateful = false;
	boolean ifFixCon = true;
	long lineNumber = 0;
	
	boolean ifBin = true; // if the trace is in binary format
	
	PrintWriter plainOut = null; // used for binary format debug, will translate binary format to plain text format
	
	double instrTime = 0.013; // in microseconds.
	
	Map<Function,Double> execTimeMap;
	Map<Function,Double> startTimeMap;
	
	Set<String> pinByDefList;
	Map<String,Set<Function>> pinTimeMap = new HashMap<String,Set<Function>>(); 
	
	Set<Function> inlinedFunctions = new HashSet<Function>();
	
	public TraceProcessor(String logFile, String localFile, String coLocFile, boolean nested, boolean stateful,FunctionFactory fFactory){
		this.nested = nested;
		this.stateful = stateful;
		this.logFile = logFile;
		this.localFile = localFile;
		this.coLocFile = coLocFile;
		this.fFactory = fFactory;
		varTable = new VarBitSet(VarEntry.domain,100*1000);
		variables = new Hashtable<Trio<String,Integer,Integer>, VarEntry>();
		threadMap = new Hashtable<Integer, Integer>();
		localFuncMap = new HashMap<String,Integer>();
		coLocFunctions = new HashSet<Set<Function>>();
		coLocFunctionNames = new HashSet<Set<String>>();
		stacks = new Vector[100];
		totalDelays = new double[100];
		threads = new Function[100];
		pinByDefList = new HashSet<String>();
		
		for(int i = 0; i <100; i++){
			stacks[i] = new Vector<Long>();
			threads[i] = null;
			totalDelays[i] = 0;
		}
		
	//	forest = new Function(null, id++, "forestroot", false, 0,0);
		forest = fFactory.create(null, FORESTROOT, false, 0,0);
		forest.setNonOffloadable(/*LocalFun.UP*/);
	}
	
	public Function getForest(){
		return forest;
	}
	
	public Set<VarEntry> getVarTable(){
		return varTable;
	}
	
	
	private void loadLocalFunctions(){
		if(localFile == null)
			return;
		
		try{
			BufferedReader r = new BufferedReader(new FileReader(localFile));
			String line;
			while((line = r.readLine()) != null){
				if(line.startsWith("#"))
					continue;
				String[] items = line.split("\t");
				localFuncMap.put(items[0],Integer.parseInt(items[1]));
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void pinLocalMethods(Function fun/*, int type*/){
		/*	if(type == LocalFun.CHILDOFFLOADABLE){
				fun.localType = type;
				return;
			}else
				fun.setNonOffloadable(type);
			
			if(type == LocalFun.DOWN && nested)
				return;
			while((fun = fun.mParent) != null){
				if(!fun.isOffloadable())
					return;
				fun.setNonOffloadable(LocalFun.UP);
			}
		*/	
			fun.setNonOffloadable();
		}

		private boolean isLocalFunction(Function function, boolean isNative){
			String functionName = function.getName();
			Integer type = localFuncMap.get(functionName);
			if(type == null)
				if(isNative){
					pinByDefList.add(functionName);
					Set<Function> funcSet = pinTimeMap.get(functionName);
					if(funcSet == null){
						funcSet = new HashSet<Function>();
						pinTimeMap.put(functionName, funcSet);
					}
					funcSet.add(function);
					return true;
					}
				else
					return false;
			int typeValue = type.intValue();
			if(typeValue == STATEFULL && !this.stateful)
				return true;
			if(typeValue == PINNED)
				return true;
			return false;
		}
	
	private void loadCoLocFunctions(){
		jniArrayMap = new HashMap<Set<String>,Map<String,Pair<Integer,Integer>>>();
		if(coLocFile == null)
			return;
		
		try{
			BufferedReader r = new BufferedReader(new FileReader(coLocFile));
			String line;
			while((line = r.readLine()) != null){
				if(line.startsWith("#"))
					continue;
				String names[] = line.split("\\s+");
				Set<String> coSet = new HashSet<String>();
				for(String name : names){
					coSet.add(name);
				}
				coLocFunctionNames.add(coSet);
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	Set<Function> noninlinableSet;
	
	private void inlineFunctions(){
		System.out.println("Function count before inlining: "+countFunctions(forest));
		noninlinableSet = new HashSet<Function>();
		for(Function t : forest.mChildren)
			for(Function top:t.mChildren)
				inlineRecursively(top);
		System.out.println("Function count after inlining: "+countFunctions(forest));
	}
	
	private boolean isColocated(Function f){
		for(Set<Function> scc : coLocFunctions)
			if(scc.contains(f))
				return true;
		return false;
	}
	
	public boolean inlinable(Function f){
		if(noninlinableSet.contains(f))
			return false;
		if(!f.isOffloadable()){
			if(this.nested){
				noninlinableSet.add(f);
				noninlinableSet.addAll(f.getAncestors());
			}
			return false;
		}
		if(isColocated(f)){//Will only reach here in a stateful setting
			if(!this.stateful)
				throw new RuntimeException("In stateless setting, co-located methods should be pinned!");
			noninlinableSet.add(f);
			noninlinableSet.addAll(f.getAncestors());
			return false;
		}
		if(Configuration.getLocalExecutionTime(f) + Configuration.applyModel(f.getSubtreeInputSize(), -1)+Configuration.applyModel(f.getSubtreeOutputSize(), -1) 
				<= 2*Configuration.latency+Configuration.getRemoteExecutionTime(f))
			return true;
		return false;
	}
	
	private void inlineRecursively(Function f){
		Set<Function> childrenToInline = new HashSet<Function>();
		for(Function c : f.getChildren()){
			inlineRecursively(c);
				if(inlinable(c)){
					childrenToInline.add(c);
				}
		}
		if(childrenToInline.size() > 0){
			for(Function c : childrenToInline){
				f.inlineChild(c);
				inlinedFunctions.add(c);
			}
			f.updateRemainderTime();
		}
	}
	
	private void updateCoLocSCC(){
		//Step 1,generate co-location set according to the co-location file
		Utils.printLogWithTime("Loading user provided colocation list.");
		Map<String,Set<Function>> colFuncMap = new HashMap<String,Set<Function>>();
		for(Set<String> nameSet:coLocFunctionNames){
			Set<Function> colSet = new HashSet<Function>();
			for(String name: nameSet)
				colFuncMap.put(name, colSet);
		}
		populateColFuncMap(forest,colFuncMap);
		for(Map.Entry<String, Set<Function>> entry : colFuncMap.entrySet()){
			if(entry.getValue().size() > 0)
				this.coLocFunctions.add(entry.getValue());
		}
		//step 2, use co-location to solve the concurrency problem, this step is done in OffloadingAlgorithm.java
		if(this.ifFixCon){
			Utils.printLogWithTime("Fixing concurrecy issue using colocations");
			//		this.fixConcurrency();
			//		this.fixConcurrency2();
			this.fixConcurrency3();
		}
		this.mergeSccs();
		System.out.println("Co-loc set size: "+getColNum());
		System.out.println("Co-loc set: "+this.coLocFunctions);
	}

	private int getColNum(){
		int ret = 0;
		for(Set<Function> s : coLocFunctions)
			ret+=s.size();
		return ret;
	}
	
	private void fixConcurrency3(){
		Utils.printLogWithTime("Building hyper edges.");
		//Map from thread pairs to the set of writer-reader pairs connecting them
		Map<Pair<Function,Function>,Set<Pair<Function,Function>>> hyperEdgeMap = new HashMap<Pair<Function,Function>,Set<Pair<Function,Function>>>();
		List<Function> threads = forest.getChildren();
		for(Function t : threads){
			for(VarEntry v : t.getSubtreeOutput()){
				Function writer = v.getWriter();
				for(Function reader : v.getReaders()){
					Function t1 = reader.getThread();
					if(!t.equals(t1)){
						Pair<Function,Function> wrPair = new Pair<Function,Function>(writer,reader);
						Pair<Function,Function> wrTPair = new Pair<Function,Function>(t,t1);
						Set<Pair<Function,Function>> edges = hyperEdgeMap.get(wrTPair);
						if(edges == null){
							edges = new HashSet<Pair<Function,Function>>();
							hyperEdgeMap.put(wrTPair, edges);
						}
						edges.add(wrPair);
					}
				}
			}
		}
		Utils.printLogWithTime("Building reachable threads relation.");	
		Map<Function,Set<Function>> reachableT = new HashMap<Function,Set<Function>>();
		for(Function t : threads){
			Set<Function> reachSet = new HashSet<Function>();
			reachSet.add(t);
			this.buildReachMRecursively(t, reachSet, hyperEdgeMap);
			reachableT.put(t, reachSet);
		}
		Utils.printLogWithTime("Building SCCs of threads");
		Set<Set<Function>> threadSCC= new HashSet<Set<Function>>();
		Set<Function> workList = new HashSet<Function>(threads);
		while(!workList.isEmpty()){
			Function seed = workList.iterator().next();
			Set<Function> scc = new HashSet<Function>();
			scc.add(seed);
			for(Function f : reachableT.get(seed)){
				Set<Function> fReach = reachableT.get(f);
				if(fReach.contains(seed))
					scc.add(f);
			}
			threadSCC.add(scc);
			workList.removeAll(scc);
		}
		Utils.printLogWithTime("Colocating methods");
		for(Set<Function> scc : threadSCC){
			if(scc.size() <= 1)
				continue;
			Set<Function> sccColoc = new HashSet<Function>();
			for(Function f1 : scc){
				Set<Pair<Function,Function>> writerEdgeSet = new HashSet<Pair<Function,Function>>();
				Set<Pair<Function,Function>> readerEdgeSet = new HashSet<Pair<Function,Function>>();
				for(Function f2 : scc){
					if(f2.equals(f1))
						continue;
					Set<Pair<Function,Function>> writerEdges = hyperEdgeMap.get(new Pair<Function,Function>(f1,f2));
					if(writerEdges!=null)
						writerEdgeSet.addAll(writerEdges);
					Set<Pair<Function,Function>> readerEdges = hyperEdgeMap.get(new Pair<Function,Function>(f2,f1));
					if(readerEdges!=null)
						readerEdgeSet.addAll(readerEdges);
				}
				sccColoc.addAll(this.findColocateMethods(readerEdgeSet, writerEdgeSet));
			}
			this.coLocFunctions.add(sccColoc);
		}
	}
	
	private void buildReachMRecursively(Function cf,Set<Function> reachSet, Map<Pair<Function,Function>,Set<Pair<Function,Function>>> hyperEdgeMap){
		for(Map.Entry<Pair<Function,Function>, Set<Pair<Function,Function>>> entry : hyperEdgeMap.entrySet()){
			Pair<Function,Function> wrTPair = entry.getKey();
			if(wrTPair.getFirst().equals(cf)&&!entry.getValue().isEmpty()){
				if(reachSet.add(wrTPair.getSecond())){
					buildReachMRecursively(wrTPair.getSecond(),reachSet,hyperEdgeMap);
				}
			}
		}
	}
	
	private void fixConcurrency2(){
		Utils.printLogWithTime("Building hyper edges.");
		//Map from thread pairs to the set of writer-reader pairs connecting them
		Map<Pair<Function,Function>,Set<Pair<Function,Function>>> hyperEdgeMap = new HashMap<Pair<Function,Function>,Set<Pair<Function,Function>>>();
		List<Function> threads = forest.getChildren();
		for(Function t : threads){
			for(VarEntry v : t.getSubtreeOutput()){
				Function writer = v.getWriter();
				for(Function reader : v.getReaders()){
					Function t1 = reader.getThread();
					if(!t.equals(t1)){
						Pair<Function,Function> wrPair = new Pair<Function,Function>(writer,reader);
						Pair<Function,Function> wrTPair = new Pair<Function,Function>(t,t1);
						Set<Pair<Function,Function>> edges = hyperEdgeMap.get(wrTPair);
						if(edges == null){
							edges = new HashSet<Pair<Function,Function>>();
							hyperEdgeMap.put(wrTPair, edges);
						}
						edges.add(wrPair);
					}
				}
			}
		}
		Utils.printLogWithTime("Detect edges in hyper graph.");
		Set<Function> workList = new HashSet<Function>(threads);
		while(!workList.isEmpty()){
			List<Pair<Function,Function>> path = new ArrayList<Pair<Function,Function>>();
			Function start = workList.iterator().next();
			this.detectCycles2(start, path, workList, hyperEdgeMap);
		}
	}
	
	private void detectCycles2(Function node,List<Pair<Function,Function>> path, Set<Function> workList,Map<Pair<Function,Function>,Set<Pair<Function,Function>>> hyperEdgeMap){
		workList.remove(node);
		out:for(Map.Entry<Pair<Function, Function>,Set<Pair<Function,Function>>> entry: hyperEdgeMap.entrySet()){
			Pair<Function, Function> wrTPair = entry.getKey();
			if(entry.getValue().isEmpty()||wrTPair.getFirst() != node)//Find the writer-reader pair using node as the writer
				continue;
			Function rt = wrTPair.getSecond();
			for(int wIdx = 0;wIdx < path.size();wIdx++){
				Pair<Function,Function> e1 = path.get(wIdx);
				if(e1.getFirst().equals(rt)){//cycle detected
					{
						System.out.print("Cycle found: ");
						System.out.print(node.getThreadID()+" "+rt.getThreadID()+",");
					}
					Set<Function> colSet = new HashSet<Function>();
					colSet.addAll(findColocateMethods(hyperEdgeMap.get(wrTPair),hyperEdgeMap.get(e1)));
					for(int j = wIdx; j < path.size()-1;j++){//handle the dest node of each edge
						Pair<Function,Function> e2 = path.get(j);
						Pair<Function,Function> e3 = path.get(j+1);
					{
						System.out.print(e2.getFirst().getThreadID()+" "+e2.getSecond()+",");
					}
						colSet.addAll(findColocateMethods(hyperEdgeMap.get(e2),hyperEdgeMap.get(e3)));
					}
					Pair<Function,Function> e4 = path.get(path.size() - 1);
					{
						System.out.println(e4.getFirst().getThreadID()+" "+e4.getSecond().getThreadID());
					}
					colSet.addAll(findColocateMethods(hyperEdgeMap.get(e4),hyperEdgeMap.get(wrTPair)));
					if(coLocFunctions.add(colSet)){
						System.out.println("Co-locate the following methods because of concurrency issue: ");
						System.out.println(colSet);
					}
					continue out;
				}
			}
			//no cycles found, continue traversing
			List<Pair<Function,Function>> localPath = new ArrayList<Pair<Function,Function>>(path);//avoid data sharing among different search paths
			localPath.add(wrTPair);
			detectCycles2(rt,localPath,workList,hyperEdgeMap);
		}
	}
	
	/**
	 * ps1: t1(w)->t2(r), ps2: t2(w)->t3(r). find parents of <r,w> pairs in t2
	 * @param p1
	 * @return
	 */
	private Set<Function> findColocateMethods(Set<Pair<Function,Function>> ps1, Set<Pair<Function,Function>> ps2){
		Set<Function> ret = new HashSet<Function>();
		for(Pair<Function,Function> p1 : ps1)
			for(Pair<Function,Function> p2 : ps2){
//				ret.add(this.findCommonParent(p1.getSecond(), p2.getFirst()));
				ret.add(p1.getSecond());
				ret.add(p2.getFirst());
			}
		return ret;
	}

	/**
	 * Fix the concurrency issue documented in the googledoc using co-location.
	 * Note this method should always be called after generateDataStatsRecursive2
	 * and before generateConstraintsAndSolve in the optimize function of each subclass.
	 */
	private void fixConcurrency(){
		Set<Function> workList = new HashSet<Function>(forest.getChildren());//all the threads
		while(!workList.isEmpty()){
			List<Edge> path = new ArrayList<Edge>();
			Function start = workList.iterator().next();
			detectCycles(start,path,workList);
		}
	}
	
	
	private void mergeSccs(){
		//merge sccs with intersection
		while(true){
			Set<Set<Function>> ncoLocSCCs = new HashSet<Set<Function>>();
			Set<Set<Function>> ocoLocSCCs = new HashSet<Set<Function>>(coLocFunctions);
			while(!coLocFunctions.isEmpty()){
				Set<Function> sccToGrow = new HashSet<Function>(coLocFunctions.iterator().next());
				Set<Set<Function>> sccsToRemove = new HashSet<Set<Function>>();
				out:for(Set<Function> tscc : coLocFunctions){
					for(Function tf : tscc){
						if(sccToGrow.contains(tf)){
							sccToGrow.addAll(tscc);
							sccsToRemove.add(tscc);
							continue out;
						}
					}
				}
				ncoLocSCCs.add(sccToGrow);
				coLocFunctions.removeAll(sccsToRemove);
			}
			coLocFunctions = ncoLocSCCs;	
			if(coLocFunctions.equals(ocoLocSCCs))
				break;
		}
	}
	
		/**
	 * Only call this function in stateless offloading algorithm.
	 */
	private void pinCoLocFuncs(){
		for(Set<Function> scc : coLocFunctions)
			for(Function func : scc)
				func.setNonOffloadable();
	}
	
	
	/**
	 * add out edges of node to path. see if it forms a cycle
	 * @param node 
	 * @param path
	 * @param workList
	 */
	private void detectCycles(Function node,List<Edge> path, Set<Function> workList){
		workList.remove(node);
		Set<Edge> exploredEdges = new HashSet<Edge>();
		for(VarEntry v: node.getSubtreeOutput()){
			out: for(Function f : v.getReaders()){
				Function rt = f.getThread(); 
				if(!rt.equals(node)){//data dependency across threads
					Edge e = new Edge();
					e.reader = f;
					e.writer = v.getWriter();
					e.writerThread = node;
					e.readerThread = rt;
					if(!exploredEdges.add(e))
						continue;
					for(int wIdx = 0;wIdx < path.size();wIdx++){
						Edge e1 = path.get(wIdx);
						if(e1.writerThread.equals(rt)){//cycle detected
							Set<Function> colSet = new HashSet<Function>();
							Function cf = findCommonParent(f,e1.writer);
							colSet.add(cf);
							for(int j = wIdx; j < path.size()-1;j++){//handle the dest node of each edge
								Edge e2 = path.get(j);
								Edge e3 = path.get(j+1);
								cf = findCommonParent(e2.reader,e3.writer);
								colSet.add(cf);
							}
							Edge e4 = path.get(path.size() - 1);
							cf = findCommonParent(e4.reader,e.writer);
							colSet.add(cf);
							if(coLocFunctions.add(colSet)){
								System.out.println("Co-locate the following methods because of concurrency issue: ");
								System.out.println(colSet);
							}
							continue out;
						}
					}
					//no cycles found, continue traversing
					List<Edge> localPath = new ArrayList<Edge>(path);//avoid data sharing among different search paths
					localPath.add(e);
					detectCycles(rt,localPath,workList);
				}
			}
		}
	}
	
	private Function findCommonParent(Function f1, Function f2){
		while(!f2.getAncestors().contains(f1)&&f1.getParent()!=null)
			f1 = f1.getParent();
		return f1;	
	}
	
	private void populateColFuncMap(Function f, Map<String,Set<Function>> funcMap){
		Set<Function> colSet = funcMap.get(f.mName);
		if(colSet!=null){
			colSet.add(f);
		}
		if(f.mChildren != null)
			for(Function c : f.mChildren)
				populateColFuncMap(c,funcMap);
	}

	
	private long genDummyAddr(){
		addrSeed--;
		return addrSeed;
	}
	
	private void handleParameters(Function caller, Function callee){
		String name = callee.mName;
		int start = name.indexOf("(")+1;
		int end = name.length() - 1;
		String sig = name.substring(start,end);
		for(int i = 1; i < sig.length(); i++){
			char parType = sig.charAt(i);
			Integer size = primSizeMap.get(parType);
			if(size == null || size == 0){
				System.err.println("Ill formated method name: "+name);
				throw new RuntimeException("Ill formated method name: "+name);
			}
			long addr = genDummyAddr();
			String address = Long.toString(addr);
			int type = 0; //OBJ_REGISTER, refer to the source of trace generation
			int offset = 0;
			this.insertWrite(type, address, offset, size, caller);
			this.insertRead(type, address, offset, size, callee);
		}
	}

	private void handleReturn(Function caller, Function callee){
		String name = callee.mName;
		int start = name.indexOf("(")+1;
		int end = name.length() - 1;
		String sig = name.substring(start,end);	
		char ret = sig.charAt(0);
		int size = primSizeMap.get(ret);
		if(size != 0){
			long addr = genDummyAddr();
			String address = Long.toString(addr);
			int type = 0; // OBJ_REGISTER, refer to the source of trace generation
			int offset = 0;
			this.insertWrite(type, address, offset, size, callee);
			this.insertRead(type, address, offset, size, caller);
		}
	}
	
	private Function parseFunEntrance(String line, Function parent){
		String name = null;
		boolean nativeMeth = false;
		long time = 0;
		long count = 0;
		long address = 0;
		String[] params = line.split(" ");
		for(int k = 0; k < params.length; k++){
			String[] items = params[k].split("=");
			if(items.length <= 1)
				continue;
			if(items[0].compareTo("n") == 0){
				//function name;
				name = items[1];
			}
			else if(items[0].compareTo("a") == 0){
				//function address
				address = Long.parseLong(items[1]);
			}
			else if(items[0].compareTo("nt") == 0){
				if(Integer.parseInt(items[1]) == 1)
					nativeMeth = true;
			}
			else if(items[0].compareTo("t") == 0){
				time = Long.parseLong(items[1]);
				if(time > logExitTime)
					logExitTime = time;
				if(logFunCount == 0){
					logStartTime = Long.parseLong(items[1]);
				}
				logFunCount++;
				logTotalTime = Long.parseLong(items[1])-logStartTime;
			}/*else if(items[0].compareTo("st") == 0){
				if(logFunCount == 0)
					logStartTime = Long.parseLong(items[1]);
				logFunCount++;
				logTotalTime = Long.parseLong(items[1])-logStartTime;
			}*/
			else if(items[0].compareTo("c") == 0){
				count = Long.parseLong(items[1]);
			}
		}
		if(parent.mStartTime > time){
			Utils.printLog(lineNumber+"\tStart time of caller is after start time of callee!");
			return null;
		}

		if(parent.mStartCount > count){
			Utils.printLog(lineNumber+"\tStart count in trace of caller is after start count of callee!");
			return null;		
		}		

		funAddress = address;
	//	Function f = new Function(parent, id++, name, nativeMeth, time, parent.threadID);
		Function f = fFactory.create(parent, name, nativeMeth, time, parent.threadID);
		f.setStartCount(count);
		f.entryLineNum = this.lineNumber;
		
	/*	if(parent.getLocalType() == LocalFun.DOWN){
			//f's parent is a non-offloadable function with type DOWN
			//f should also be non-offloadble
			this.pinLocalMethods(f, LocalFun.DOWN);
		}else if(parent.getLocalType() == LocalFun.CHILDOFFLOADABLE){
			//its children are always offloadable
			this.pinLocalMethods(f, LocalFun.CHILDOFFLOADABLE);
		}else{
			int type = isLocalFunction(name);
			if(type != LocalFun.OFFLOADABLE)
				this.pinLocalMethods(f, type);
		}
	*/	
		if(isLocalFunction(f,nativeMeth))
			this.pinLocalMethods(f/*, type*/);
		parent.addChild(f);
		
		this.checkJNIArray(f);
		this.handleParameters(parent,f);
		return f;
	}

/**
 * Format: type(1)thread id(2)time(4)count(4)addr(4)native(1)method name(end with space)	
 * @param input
 * @param parent
 * @return
 * @throws IOException
 */
	private Function parseFunEntranceBin(DataInputStream input, Function parent) throws IOException{
		long time = Utils.intToUnsigned(input.readInt()); // no unsigned int in java
		long count = Utils.intToUnsigned(input.readInt()); // no unsigned int in java
		long addr = Utils.intToUnsigned(input.readInt());
		Byte isNative = input.readByte();
		boolean nativeMeth = (isNative !=0);
		String name = Utils.readStringUntilHashTag(input);
		if(time > logExitTime)
			logExitTime = time;
		if(logFunCount == 0){
			logStartTime = time;
		}
		logFunCount++;
		logTotalTime = time-logStartTime;
		if(parent.getStartTime() > time ){
			Utils.printLog(lineNumber+"\tStart time in trace of caller is after start time of callee!");
			return null;
		}
		if(parent.mStartCount > count){
			Utils.printLog(lineNumber+"\tStart count in trace of caller is after start count of callee!");
			return null;		
		}
		
		funAddress = addr;
	//	Function f = new Function(parent, id++, name, nativeMeth, time, parent.threadID);
		Function f = fFactory.create(parent, name, nativeMeth, time, parent.threadID);
		f.setStartCount(count); //log the instruction count of function entrance
		f.entryLineNum = this.lineNumber;

		if(isLocalFunction(f, nativeMeth))
			this.pinLocalMethods(f/*, type*/);
		parent.addChild(f);

		if(plainOut!=null)
			plainOut.println("<fun n="+name+" a="+addr+" nt="+(int)isNative+" t="+time+" c="+count+">");

		this.checkJNIArray(f);
		this.handleParameters(parent,f);	
		return f;
	}
	
	private void parseFunExit(int thread, String line, Function function){
		String[] params = line.split(" ");
		long funAddress = 0;
		long time = 0;
		long sysTime = 0;
		long count = 0;
		for(int k = 0; k < params.length; k++){
			String[] items = params[k].split("=");
			if(items.length <= 1)
				continue;
			else if(items[0].compareTo("a") == 0){
				//function address
				funAddress = Long.parseLong(items[1]);
			}
			else if(items[0].compareTo("t") == 0){
				time = Long.parseLong(items[1]);
				if(time > logExitTime)
					logExitTime = time;
			}/*else if(items[0].compareTo("st") == 0){
				sysTime = Long.parseLong(items[1]);
			}*/
			else if(items[0].compareTo("c") == 0){
				count = Long.parseLong(items[1]);
			}
		}


		boolean found = false;
		if(!stacks[thread].isEmpty()){	
			int p = stacks[thread].size();
			for(int j = 0; j < stacks[thread].size(); j++){
				if(funAddress == stacks[thread].get(j)){
					p = j;
					
					while(p>0){
						p--;
						stacks[thread].remove(0);
						Utils.printLog(lineNumber+"\tno exit\t"+function.getName());
					//	function.setEndTime(time);
						function = function.mParent;
						function.mParent.inlineChild(function);
					}
					found = true;
					stacks[thread].remove(0);
					function.setEndTime(time);
					function.setEndCount(count);
					this.handleReturn(function.mParent,function);
					function.setExitLineNum(this.lineNumber);
					function = function.mParent;
					threads[thread] = function;
					break;
				}
			}
		}
		if(!found ){
			Utils.printLog(lineNumber+"\tno entry\t"+funAddress);
		}
	}

	/**
	 * Format: type(1)thread id(2)time(4)count(4)addr(4)
	 * @param thread
	 * @param r
	 * @param function
	 * @throws IOException
	 */
	private void parseFunExitBin(int thread, DataInputStream r, Function function) throws IOException{
		long time = Utils.intToUnsigned(r.readInt());
		if(time > logExitTime)
					logExitTime = time;
		long count = Utils.intToUnsigned(r.readInt());
		long funAddress = Utils.intToUnsigned(r.readInt());

		boolean found = false;
		if(!stacks[thread].isEmpty()){	
			int p = stacks[thread].size();
			for(int j = 0; j < stacks[thread].size(); j++){
				if(funAddress == stacks[thread].get(j)){
					p = j;
					
					while(p>0){
						p--;
						stacks[thread].remove(0);
						Utils.printLog(lineNumber+"\tno exit\t"+function.getName());
					//	function.setEndTime(time);
						function = function.mParent;
						function.mParent.inlineChild(function);
					}
					found = true;
					stacks[thread].remove(0);
					function.setEndTime(time);
					function.setEndCount(count);
					function.setExitLineNum(this.lineNumber);
					this.handleReturn(function.mParent,function);
					function = function.mParent;
					threads[thread] = function;
					break;
				}
			}
		}
		if(!found ){
			Utils.printLog(lineNumber+"\tno entry\t"+funAddress);
		}
		if(plainOut!=null)
			plainOut.println("</fun a="+funAddress+" t="+time+" c="+count+">");
	}
	
	private void parseAlloc(String line, Function function){
		String[] params = line.split(" ");		
		int size = 0;
		String addr = null;
		int offset = 0;
		for(int k = 0; k < params.length; k++){
			String[] items = params[k].split("=");
			if(items.length <= 1)
				continue;			
			else if(items[0].compareTo("v") == 0){
				//object address
				addr = items[1];
			}
			else if(items[0].compareTo("s") == 0){
				//size
				size = Integer.parseInt(items[1]);
			}else if(items[0].compareTo("p") == 0){
				//offset
				offset = Integer.parseInt(items[1]);
			}
		}
		VarEntry v = new VarEntry(addr, size, offset, function);
		variables.put(new Trio<String,Integer,Integer>(addr,size,offset), v);
	}

	protected void insertWrite(int type, String addr, int offset, int size, Function function){
		VarEntry v = variables.get(new Trio<String,Integer,Integer>(addr,size,offset));
		if(v == null){
			v = new VarEntry(addr, size, offset, function);
		}
		else{
			v = new VarEntry(v);
			v.setWriter(function);
		}
		variables.put(new Trio<String,Integer,Integer>(addr,size,offset), v);
	}
	
	private void parseWrite(String line, Function function){
		String[] params = line.split(" ");
		int size = 0;
		String type= null;
		String addr = null;
		String offset = null;
		for(int k = 0; k < params.length; k++){
			String[] items = params[k].split("=");
			if(items.length <= 1)
				continue;
			else if(items[0].compareTo("t") == 0){
				//object type
				type = items[1];
			}
			else if(items[0].compareTo("v") == 0){
				//object address
				addr = items[1];
			}
			else if(items[0].compareTo("p") == 0){
				//offset
				offset = items[1];
			}
			else if(items[0].compareTo("s") == 0){
				//size
				size = Integer.parseInt(items[1]);
			}
		}
		insertWrite(Integer.parseInt(type),addr,Integer.parseInt(offset),size,function);
	}

	/**
	 * Format: type(1)thread id(2)type(1)addr(4)size(4)offset(4) 
	 * @param r
	 * @param function
	 * @throws IOException
	 */
	private void parseWriteBin(DataInputStream r, Function function) throws IOException{
		byte type = r.readByte();
		long addr = Utils.intToUnsigned(r.readInt());
		int size = r.readInt(); // well, in the dalvik side, the size is written as unsigned int
		int offset = r.readInt();// currently not used
		String address = Long.toString(addr);
		//TODO: refine this. Currently we use 8 as default field size
		if(type < 5)
			size = 8;
		insertWrite(type,address,offset,size,function);
		if(plainOut!=null)
			plainOut.println("<w t="+(int)type+" v="+addr+" p="+offset+" s="+size+"/>");
	}

	private Set<String> getColNameSet(String fname){
		for(Set<String> s : coLocFunctionNames)
			if(s.contains(fname))
				return s;
		return null;
	}
	
	/**
	 * Check if we need to insert read/write for array object of native functions.
	 * Some co-located method might hold Java array pointers
	 * @param f
	 */
	private void checkJNIArray(Function f){
		if(f.isNative()){
			Set<String> colSet = getColNameSet(f.getName());
			Map<String,Pair<Integer,Integer>> arraySet = jniArrayMap.get(colSet);
			if(arraySet != null)
				for(Map.Entry<String,Pair<Integer,Integer>> arrayInfo : arraySet.entrySet()){
					if(arrayInfo.getValue().getSecond() > 0){
						this.insertRead(OBJ_JNI, arrayInfo.getKey(), 0, arrayInfo.getValue().getFirst(), f);
						this.insertWrite(OBJ_JNI, arrayInfo.getKey(), 0, arrayInfo.getValue().getFirst(), f);
					}
				}
		}
	}
	
	protected void insertRead(int type, String addr, int offset, int size, Function function){
		if(type == OBJ_JNI_HOLD_ARRAY){
//			if(!function.isNative())
//				throw new RuntimeException(function.getName()+" is accessing JNI bridge but it is shown as not native?");
			System.out.println("Native method "+function.getName()+" is accessing array through JNI. Make sure the co-location relation is right about it");
			Set<String> colNameSet = getColNameSet(function.getName());
			if(colNameSet!=null){
				Map<String,Pair<Integer,Integer>> arraySet = jniArrayMap.get(colNameSet);
				if(arraySet == null){
					arraySet = new HashMap<String,Pair<Integer,Integer>>();
					jniArrayMap.put(colNameSet, arraySet);
				}
				Pair<Integer,Integer> p = arraySet.get(addr);
				if(p == null){
					p = new Pair<Integer,Integer>(size,0);
				}
				arraySet.put(addr, new Pair<Integer,Integer>(p.getFirst(),p.getSecond()+1));
				if(p.getSecond() == 0){
					this.insertRead(OBJ_JNI, addr, offset, size, function);
					this.insertWrite(OBJ_JNI, addr, offset, size, function);
				}
			}
			else{
				this.insertRead(OBJ_JNI, addr, offset, size, function);
				this.insertWrite(OBJ_JNI, addr, offset, size, function);
			}
			return;
		}
		if(type == OBJ_JNI_REL_ARRAY){
//			if(!function.isNative())
//				throw new RuntimeException(function+" is accessing JNI bridge but it is shown as not native?");
			System.out.println("Native method "+function.getName()+" is releasing an array through JNI. Make sure the co-location realtion is right about it");
			Set<String> colNameSet = getColNameSet(function.getName());
			if(colNameSet!=null){
				Map<String,Pair<Integer,Integer>> arraySet = jniArrayMap.get(colNameSet);
				if(arraySet == null||!arraySet.containsKey(addr)){
					throw new RuntimeException(function.getName()+" is releasing an array it doesn't hold.");
				}
				Pair<Integer,Integer> p = arraySet.get(addr);
				if(p.getSecond() == 1)
					arraySet.remove(addr);
				else
					arraySet.put(addr, new Pair<Integer,Integer>(p.getFirst(),p.getSecond()-1));
			}
			return;
		}
		VarEntry v = variables.get(new Trio<String,Integer,Integer>(addr,size,offset));
		if(v != null){
			if(v.getReaders().size() == 0)
				varTable.add(v);
			v.addReader(function);

			function.addInput(v);

			//	var.writer.addOutput(var);
			v.getWriter().addOutput(v);
		}	
	}
	
	private void parseRead(String line, Function function){
		String[] params = line.split(" ");
		int size = 0;
		String type= null;
		String addr = null;
		String offset = null;
		for(int k = 0; k < params.length; k++){
			String[] items = params[k].split("=");
			if(items.length <= 1)
				continue;
			else if(items[0].compareTo("t") == 0){
				//object type
				type = items[1];
			}
			else if(items[0].compareTo("v") == 0){
				//object address
				addr = items[1];
			}
			else if(items[0].compareTo("p") == 0){
				//offset
				offset = items[1];
			}
			else if(items[0].compareTo("s") == 0){
				//size
				size = Integer.parseInt(items[1]);
			}
		}
		insertRead(Integer.parseInt(type),addr,Integer.parseInt(offset),size,function);
	}
	
	/**
	 * Format: type(1)thread id(2)type(1)addr(4)size(4)offset(4) 
	 * @param r
	 * @param function
	 * @throws IOException
	 */
	private void parseReadBin(DataInputStream r, Function function) throws IOException{
		byte type = r.readByte();
		long addr = Utils.intToUnsigned(r.readInt());
		int size = r.readInt(); // well, in the dalvik side, the size is written as unsigned int
		int offset = r.readInt();// currently not used
		String address = Long.toString(addr);
		//TODO: refine this. currently use 8 as default field size
		if(type < 5)
			size = 8;
		insertRead(type,address,offset,size,function);
		if(plainOut!=null)
			plainOut.println("<r t="+(int)type+" v="+addr+" p="+offset+" s="+size+"/>");
	}
	
	private void parseNativeBin(DataInputStream r) throws IOException{
		String fName = Utils.readStringUntilHashTag(r);
		String libName = Utils.readStringUntilHashTag(r);
		System.out.println("Function name: "+fName+", Library name: "+libName);
		if(plainOut!=null)
			plainOut.println("<nl f="+fName+" so="+libName+"/>");
	}
	
	private void postProcess(){
		//Close any open functions
		for(int i = 0; i < 100; i ++){
			while(!stacks[i].isEmpty()){
				long fun = stacks[i].remove(0);
				//Utils.printLog("no exit at end of trace\t"+fun);
			}
			Function f = threads[i];
			while( f != null){
				Utils.printLog(lineNumber+"\tno exit at end of trace\t"+f.getName());
				if(f.mChildren.isEmpty()){
					f.setEndTime(f.mStartTime);
					f.setEndCount(f.mStartCount);
				}
				else{
					int cs = f.mChildren.size();
					f.setEndTime(f.mChildren.get(cs-1).getEndTime());
					f.setEndCount(f.mChildren.get(cs-1).getEndCount());
				}
				if(f.mParent!=null && !f.mName.equals(ROOT))
					this.handleReturn(f.mParent, f);
				f = f.mParent;
			}
		}
		
		forest.updateRemainderTime();
	}
	
	private void fixTime(){
		execTimeMap = new HashMap<Function,Double>();
		startTimeMap = new HashMap<Function,Double>();
		for(Function t : forest.getChildren()){
			calExecTimeRecursively(t);
			startTimeMap.put(t, (double)t.getStartTime());
			calStartTimeRecursively(t);
		}
		for(Map.Entry<Function, Double> execEntry : execTimeMap.entrySet()){
			Function f = execEntry.getKey();
			long startTime = (long)startTimeMap.get(f).doubleValue();
			f.setStartTime(startTime);
			f.setEndTime((long)(startTime+execEntry.getValue()));
		}	
		forest.updateRemainderTime();
	}
	
	private double calExecTimeRecursively(Function f){
		double execTime = 0;
		if(f.isNative()){
			double lastEndTime = f.getStartTime();
			for(Function c : f.getChildren()){
				double callInterval = (double)c.getStartTime()-lastEndTime;
				if(callInterval < 0)
					throw new RuntimeException("Error: check child method sequence");
				execTime += callInterval;
				execTime += calExecTimeRecursively(c);
				lastEndTime = c.getEndTime();
			}
			double callInterval = (double)f.getEndTime() - lastEndTime;
			if(callInterval < 0)
				throw new RuntimeException("Error: how can child call end before parent?");
			execTime+=callInterval;
		}else{
			long lastEndCount = f.getStartCount();
			for(Function c : f.getChildren()){
				double callInterval = (c.getStartCount() - lastEndCount)*instrTime;
				if(callInterval < 0)
					throw new RuntimeException("Error: check child method sequence");
				execTime += callInterval;
				execTime += calExecTimeRecursively(c);
				lastEndCount = c.getEndCount();
			}
			double callInterval = (f.getEndCount() - lastEndCount)*instrTime;
			if(callInterval < 0)
				throw new RuntimeException("Error: how can child call end before parent?");
			execTime+=callInterval;
		}
		execTimeMap.put(f, execTime);
		return execTime;
	}
	
	/**
	 * Update the starting time recursively, note root is not taken care
	 * @param f
	 * @return
	 */
	private void calStartTimeRecursively(Function f){
		double startTime = startTimeMap.get(f);
		if(f.isNative()){
			double lastTraceTime = f.getStartTime();
			for(Function c : f.getChildren()){
				double cStartTime = startTime+(double)c.getStartTime()-lastTraceTime;
				if(cStartTime < 0)
					throw new RuntimeException("Negative start time.");
				startTimeMap.put(c, cStartTime);
				startTime = cStartTime + execTimeMap.get(c);
				lastTraceTime = c.getEndTime();
			}
		}else{
			long lastTraceCount = f.getStartCount();
			for(Function c : f.getChildren()){
				double cStartTime = startTime+(c.getStartCount()-lastTraceCount)*instrTime;
				if(cStartTime < 0)
					throw new RuntimeException("Negative start time.");
				startTimeMap.put(c, cStartTime);
				startTime = cStartTime + execTimeMap.get(c);
				lastTraceCount = c.getEndCount();
			}
		}
		for(Function c : f.getChildren())
			calStartTimeRecursively(c);
	}

	public void parse(){
		Utils.printLogWithTime("Start processing trace.");
		if(ifBin)
			parseBin();
		else parsePlain();
		postProcess();
		Utils.printLogWithTime("Generating running time using timing model");
		fixTime();
		Utils.printLogWithTime("Generating write/read data set of each method");
		this.generateDataStatsRecursive(forest);
		if(Configuration.removeLocal){
			Utils.printLogWithTime("Removing variables local to a method");
			removeLocalVars();
		}
		if(Configuration.mergeVar){
			Utils.printLogWithTime("Merging variable sharing exact same writer and readers");
			mergeVars();
		}
		Utils.printLogWithTime("Colocating functions.");
		updateCoLocSCC();
//		checkVarTable();
		//inlining functions should not change the colocation relation
		if(!this.stateful)
			this.pinCoLocFuncs();
		if(Configuration.inlineOpt){
			Utils.printLogWithTime("Inlining methods");
			inlineFunctions();
		}
		if(Configuration.removeLocal){
			Utils.printLogWithTime("Removing variables local to a method");
			removeLocalVars();
		}
		if(Configuration.mergeVar){
			Utils.printLogWithTime("Merging variable sharing exact same writer and readers");
			mergeVars();
		}
		writePinList();
//		checkVarTable();
	}
	
	private void removeLocalVars(){
		Set<VarEntry> varsToRemove = new VarBitSet(VarEntry.domain,10000);
		for(VarEntry var : varTable){
			Function writer = var.getWriter();
			if(var.getReaders().contains(writer)){
				var.getReaders().remove(writer);
				writer.getInput().remove(var);
				if(var.getReaders().size() == 0){
					varsToRemove.add(var);
					writer.getOutput().remove(var);
				}
			}
		}
		varTable.removeAll(varsToRemove);
	}
	
	/**
	 * Parse the trace file in binary format. Note in C++ (at least in android), sizeof(char) = 1, while in java, sizeof(char)=2
	 */
	private void parseBin(){
		loadLocalFunctions();
		loadCoLocFunctions();
		
		try{
			DataInputStream r = new DataInputStream(new BufferedInputStream(new FileInputStream(logFile)));
			int entries = 0;
			int exits = 0;
			int mem = 0;
			
			out:while(true){
				try{
					lineNumber++;
					int type = (int)r.readByte();
					int tid = 1;
					Integer thread = null;
					Function function = null;
					if(type < 5) // not native lib information
					{	
						tid = (int)r.readShort();
						thread = threadMap.get(tid);
						if(thread == null){
							thread = mapP++;
							threadMap.put(tid, thread);
							//		threads[thread] = new Function(forest, id++, "root", false, 0, tid);
							threads[thread] = fFactory.create(forest, ROOT, false, 0, tid);
							function = threads[thread];
							function.setStartCount(0);
							function.setNonOffloadable(/*LocalFun.UP*/);
							forest.addChild(function);
						}
						else
							function = threads[thread];
					}
					if(plainOut!=null)
						plainOut.print(tid);
					switch(type){
					case 1://the method entry
						entries++;
						totalDelays[thread] += Configuration.SYSCALLDELAY;
						Function f = parseFunEntranceBin(r,function);
						if(f != null){
							stacks[thread].add(0,funAddress);
							threads[thread] = f;
						}
						break;
					case 2://the method exit
						exits++;
						totalDelays[thread] += Configuration.SYSCALLDELAY;
						parseFunExitBin(thread,r,function);
						break;
					case 3: //read
						parseReadBin(r,function);
						mem++;
						break;
					case 4: //write
						parseWriteBin(r,function);
						mem++;
						break;
					case 5: //native lib
						parseNativeBin(r);
						break;
					case 37:
						System.out.println("Find the magic number "+37+". terminate.");
						break out;
					default:
						System.out.println("Unknown event type: "+(int)type + "on Line "+lineNumber);
						System.exit(1);
					}
				}catch(EOFException e){
					System.out.println("Reach the end of the file");
					break;
				}catch(Exception e){
					Utils.printError(lineNumber+"\t Exeception thrown while parsing");
					e.printStackTrace();
					System.exit(1);
				}
			}
			r.close();
			
			if(plainOut!=null)
				plainOut.flush();
			Utils.printResults("entry: "+entries+"\texits:"+exits+"\tmem:"+mem);
			Utils.printResults("Execution info:\t"+logFunCount+"\t"+logTotalTime);
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * 
	 * Parse the trace file in plain text
	 */
	private void parsePlain(){
		loadLocalFunctions();
		loadCoLocFunctions();
		
		try{
			BufferedReader r = new BufferedReader(new FileReader(logFile));
			String line;
			int entries = 0;
			int exits = 0;
			int mem = 0;

			while( (line = r.readLine())!= null){
				try{
					lineNumber++;
					if(line.startsWith("1<nl")){
						System.out.println("Native method informatioN: "+line);
						continue;
					}
					int i = line.indexOf("<")+1;
					Integer tid = Integer.parseInt(line.substring(0, i-1));
					Integer thread = threadMap.get(tid);
					Function function;
					if(thread == null){
						thread = mapP++;
						threadMap.put(tid, thread);
				//		threads[thread] = new Function(forest, id++, "root", false, 0, tid);
						threads[thread] = fFactory.create(forest, ROOT, false, 0, tid);
						function = threads[thread];
						function.setNonOffloadable(/*LocalFun.UP*/);
						forest.addChild(function);
					}
					else
						function = threads[thread];
					//parse the data
					if(line.substring(i).startsWith("fun")){
						//function entrance
						entries++;
						totalDelays[thread] += Configuration.SYSCALLDELAY;//Xin: what is this?
						Function f = parseFunEntrance(line.substring(i,line.lastIndexOf(">")),function);
						if(f != null){
							stacks[thread].add(0,funAddress);
							threads[thread] = f;
						}
					}
					else if(line.substring(i).startsWith("/fun")){
						//the function exit
						exits++;
						totalDelays[thread] += Configuration.SYSCALLDELAY;
						parseFunExit(thread, line.substring(i,line.indexOf(">")),function);
					}
					else if(line.substring(i).startsWith("w")){
						//write a field
						parseWrite(line.substring(i,line.indexOf("/>")),function);
						mem++;
					}
					else if(line.substring(i).startsWith("a")){
						parseAlloc(line.substring(i,line.indexOf("/>")),function);
						mem++;
					}
					else if(line.substring(i).startsWith("r")){
						parseRead(line.substring(i,line.indexOf("/>")),function);
						mem++;
					}
				}catch(Exception e){
					Utils.printError(lineNumber+"\t Exeception thrown while parsing");
					e.printStackTrace();
					System.exit(1);
				}
			}
			r.close();

			Utils.printResults("entry: "+entries+"\texits:"+exits+"\tmem:"+mem);
			Utils.printResults("Execution info:\t"+logFunCount+"\t"+logTotalTime);
			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void generateDataStatsRecursive(Function root){
		if(root==null)return;

		Vector<Function> children = root.getChildren();
		for(int i = 0; i < children.size(); i++){
			generateDataStatsRecursive(children.get(i));
			if(root.getParent() != null){//avoid forest
				//if(root.isOffloadable()){
			//	SFunction extChild = funcMap.get(children.get(i));
				Function extChild = children.get(i);
				Set<VarEntry> vars = extChild.getSubtreeInput();
				for(VarEntry v : vars){
					root.addSubtreeInput(v);
				}
				vars = extChild.getSubtreeOutput();
				for(VarEntry v : vars){
					root.addSubtreeOutput(v);
				}
				//}
			}
		}
	}
	
	private void checkVarTable(){
		for(VarEntry var : varTable){
			if(var.getReaders().contains(var.getWriter()))
				throw new RuntimeException("Variable error");
			if(var.getReaders().size() == 0)
				throw new RuntimeException("Variable error");
			if(var.getWriter() == null)
				throw new RuntimeException("Variable error");
			for(Function reader : var.getReaders()){
				if(!reader.getInput().contains(var))
					throw new RuntimeException("Variable error");
			}
			if(!var.getWriter().getOutput().contains(var))
				throw new RuntimeException("Variable error");
		}
	}
	
	private void mergeVars(){
		Map<Pair<Function,Set<Function>>,Set<VarEntry>> wrMap = new HashMap<Pair<Function,Set<Function>>,Set<VarEntry>>(); 
		System.out.println("Before merging variables, the var table size is "+varTable.size());
		for(VarEntry var : varTable){
			Pair<Function,Set<Function>> wrPair = new Pair<Function,Set<Function>>(var.getWriter(),var.getReaders());
			Set<VarEntry> colVars = wrMap.get(wrPair);
			if(colVars == null){
				colVars = new HashSet<VarEntry>();
				wrMap.put(wrPair, colVars);
			}
			colVars.add(var);
		}
		Set<VarEntry> allVarsToRemove = new VarBitSet(VarEntry.domain,varTable.size());
		for(Map.Entry<Pair<Function, Set<Function>>,Set<VarEntry>> entry : wrMap.entrySet()){
			Pair<Function,Set<Function>> wrPair = entry.getKey();
			Set<VarEntry> vars = entry.getValue();
			if(vars.size() > 1){
				VarEntry representative = vars.iterator().next();
				int totalSize = 0;
				for(VarEntry var : vars)
					totalSize += var.getSize();
				VarEntry mergedVar = representative.changeSize(totalSize);
				Set<VarEntry> varsToRemove = vars;
				varTable.add(mergedVar);
//				varsToRemove.remove(representative);
				Function writer = wrPair.getFirst();
				if(!writer.getOutput().containsAll(varsToRemove))
					throw new RuntimeException("Something wrong with the reader/writer and input/output relation!");
				writer.addOutput(mergedVar);
				writer.getOutput().removeAll(varsToRemove);
				Function pw = writer;
				while(pw != null&&pw.removeSubtreeOutputAll(varsToRemove)){
					pw.addSubtreeOutput(mergedVar);
					pw = pw.getParent();
				}
				for(Function reader : wrPair.getSecond()){
					if(!reader.getInput().containsAll(varsToRemove))
						throw new RuntimeException("Something wrong with the reader/writer and input/output relation!");
					reader.getInput().removeAll(varsToRemove);
					reader.addInput(mergedVar);
					Function pr = reader;
					while(pr != null&&pr.removeSubtreeInputAll(varsToRemove)){
						pr = pr.getParent();
						pr.addSubtreeInput(mergedVar);
					}
				}
				allVarsToRemove.addAll(varsToRemove);
			}
		}
		varTable.removeAll(allVarsToRemove);
		System.out.println("After merging variables, the var table size is "+varTable.size());
	}
	
	public int countFunctions(Function f){
		int ret = 1;
		for(Function c : f.getChildren())
			ret += countFunctions(c);
		return ret;
	}
	
	class FuncTimeComp implements Comparator<Function>{

		@Override
		public int compare(Function o1, Function o2) {
			long ret = o2.getExecutionTime() - o1.getExecutionTime();
			if(ret > 0)
				return 1;
			if(ret < 0)
				return -1;
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	private void writePinList(){
		try {
			PrintWriter pw;
			pw = new PrintWriter(new File("pinlist.txt"));
			SortedSet<Function> outputList = new TreeSet<Function>(new FuncTimeComp());
			for(String fun : pinByDefList){
				Function goalFunc = null;
				for(Function funInst : pinTimeMap.get(fun))
					if(goalFunc == null || funInst.getExecutionTime() > goalFunc.getExecutionTime())
						goalFunc = funInst;
				outputList.add(goalFunc);
			}
			
			for(Function out:outputList)
				pw.println(out.getName() + "\t"+out.getExecutionTime());
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean isIfBin() {
		return ifBin;
	}

	public void setIfBin(boolean ifBin) {
		this.ifBin = ifBin;
	}

	public PrintWriter getPlainOut() {
		return plainOut;
	}

	public void setPlainOut(PrintWriter plainOut) {
		this.plainOut = plainOut;
	}

	public Set<Function> getInlinedFunctions() {
		return inlinedFunctions;
	}

	public boolean isIfFixCon() {
		return ifFixCon;
	}

	public void setIfFixCon(boolean ifFixCon) {
		this.ifFixCon = ifFixCon;
	}

}

class HyberEdge{
	Function writerThread;
	Function readerThread;
	Set<Pair<Function,Function>> edges;
}

class Edge{
	Function reader;
	Function writer;
	Function writerThread;//the thread that writes to the variable
	Function readerThread;//the thread that reads from the variable
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((reader == null) ? 0 : reader.hashCode());
		result = prime * result + ((readerThread == null) ? 0 : readerThread.hashCode());
		result = prime * result + ((writer == null) ? 0 : writer.hashCode());
		result = prime * result + ((writerThread == null) ? 0 : writerThread.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (reader == null) {
			if (other.reader != null)
				return false;
		} else if (!reader.equals(other.reader))
			return false;
		if (readerThread == null) {
			if (other.readerThread != null)
				return false;
		} else if (!readerThread.equals(other.readerThread))
			return false;
		if (writer == null) {
			if (other.writer != null)
				return false;
		} else if (!writer.equals(other.writer))
			return false;
		if (writerThread == null) {
			if (other.writerThread != null)
				return false;
		} else if (!writerThread.equals(other.writerThread))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Edge [reader=" + reader + ", writer=" + writer + ", writerThread=" + writerThread + ", readerThread=" + readerThread + "]";
	}

}
