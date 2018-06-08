package edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import edu.gatech.traceprocessor.offloadingalgorithms.solver.OffloadingAlgorithm;
import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Pair;
import edu.gatech.traceprocessor.utils.Utils;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public abstract class MinCutOffloadingAlgorithm extends OffloadingAlgorithm {
	public final static int SRC_ID = 1;
	public final static int DST_ID = 2;
	public final static double INF = 1e100;
	
	Map<Method,Integer> methIdMap;
	//The domain of method. Index the domain by id - 3 as id 1 and 2 are reserved for src and dest
	List<Method> methDomain;
	Map<Pair<Integer, Integer>, Double> edgeCapMap;
	Map<Integer, Double> srcCapMap;
	Map<Integer, Double> dstCapMap;
//	Set<Integer> auxNodes;// nodes for helping encoding
	int nodeIdSeed;
	int maxMethId;
	public final static double EPS = 0.0000001; 

	//fields for LP
	private double[] solution;
	// to -> edgeId
	Map<Integer, Integer> srcEdgeIdMap;
	// from -> edgeId
	Map<Integer, Integer> dstEdgeIdMap;
	//<from, to> -> edgeId, src \notin from, dst \notin to
	Map<Pair<Integer,Integer>, Integer> edgeIdMap;
	//node -> outNodes
	Map<Integer,Set<Integer>> outNodeMap;// outgoing edge set except for src
	//node -> inNodes
	Map<Integer,Set<Integer>> inNodeMap;// incoming edge set except for dst
	//edgeId -> cap
	Map<Integer, Double> edgeIdCapMap;
	
	//public final static int E = 100; // inflate the numbers to reduce the precision 
	public final static int E = 1; // inflate the numbers to reduce the precision 

	public MinCutOffloadingAlgorithm(Program p) {
		super(p);
	}

	private void init(Program p){		
		methIdMap = new HashMap<Method, Integer>();
		methDomain = new ArrayList<Method>();
		edgeCapMap = new HashMap<Pair<Integer,Integer>, Double>();
		srcCapMap = new HashMap<Integer,Double>();
		dstCapMap = new HashMap<Integer,Double>();
//		auxNodes = new HashSet<Integer>();
		nodeIdSeed = 3; // 1 and 2 are reserved for src and dst
		for(Method t : p.getThreads())
			this.computeMethIdRecursively(t);
		this.maxMethId = nodeIdSeed-1;
		
		System.out.println("After init: maxMethId=" + maxMethId + ", nodeIdSeed=" + nodeIdSeed);
	}
	
	private void computeMethIdRecursively(Method m){
		methDomain.add(m);
		methIdMap.put(m, nodeIdSeed);
		nodeIdSeed++;
		for(Method c : m.getCallees())
			this.computeMethIdRecursively(c);
	}
	
	protected void addCapacity(Method from, Method to, double cap){
		int fid = this.getMethId(from);
		int tid = this.getMethId(to);
		Pair<Integer,Integer> edge = new Pair<Integer,Integer>(fid,tid);
		Double ecap = this.edgeCapMap.get(edge);
		if(ecap == null)
			ecap = 0.0;
		ecap+=cap;
		this.edgeCapMap.put(edge, ecap);
	}
	
	protected void addCapacity(int fid, int tid, double cap){
		Pair<Integer,Integer> edge = new Pair<Integer,Integer>(fid,tid);
		Double ecap = this.edgeCapMap.get(edge);
		if(ecap == null)
			ecap = 0.0;
		ecap+=cap;
		this.edgeCapMap.put(edge, ecap);
	}
	
	protected void addSrcCapacity(Method to, double cap){
		int mid = this.getMethId(to);
		Double ecap = this.srcCapMap.get(mid);
		if(ecap == null)
			ecap = 0.0;
		ecap += cap;
		this.srcCapMap.put(mid, ecap);
	}
	
	protected void addDstCapacity(Method from, double cap){
		int mid = this.getMethId(from);
		Double ecap = this.dstCapMap.get(mid);
		if(ecap == null)
			ecap = 0.0;
		ecap += cap;
		this.dstCapMap.put(mid, ecap);
	}
	
	protected int getMethId(Method m){
		return methIdMap.get(m);
	}
	
	protected boolean isSrc(int id){
		return id == 1;
	}
	
	protected boolean isDst(int id){
		return id == 2;
	}
	
	protected Method getMethod(int id){
		return methDomain.get(id - 3);
	}
	
	protected boolean isMethID(int id){
		return SRC_ID < id && DST_ID < id && id <= this.maxMethId;
	}
	
	protected boolean isDataId(int id){
		return id > this.maxMethId;
	}
	
	
	public void processPinAndColocation(){
		//pin
		for(Method m : program.getPinnedMethods()){
			pinMethod(m);
		}

		//colocation
		for(Set<Method> colSet : program.getColocSet()){
			Method last = null;
			double localTime = super.getLocalTime();
			for(Method m : colSet){
				if(last != null){
					addCapacity(m, last, localTime);
					addCapacity(last, m, localTime);
				}
				last = m;
			}
			
			//System.out.println("colSet.size : " + colSet.size());
		}
	}
	
	@Override
	public double optimize(Program p) {
		init(p);
		this.populateGraph();
		System.out.println("After populateGraph: maxMethId=" + maxMethId + ", nodeIdSeed=" + nodeIdSeed);

		Utils.printLogWithTime("A graph with "+nodeIdSeed+" nodes, "+(srcCapMap.size()+dstCapMap.size()+edgeCapMap.size())+
				" edges is generated.");
		double ret = 0.0;
		if(Configuration.MINCUT_SOLVER == Configuration.LP){
			Utils.printLogWithTime("Start solving with LP");
			ret = this.solveWithLP();
			Utils.printLogWithTime("Finish solving with LP");
		}
		if(Configuration.MINCUT_SOLVER == Configuration.PSUEDO_FLOW){
			Utils.printLogWithTime("Start solving with Pseudo Flow");
			ret = this.solveWithPseudoFlow();
			Utils.printLogWithTime("End solving with Pseudo Flow");
		}
		return ret;
	}
	
	@Override
	public double computeGuiOffloadedTime() {
		double ret = 0.0;
		for(Method ut : program.getUIThreads())
			ret += this.calculateOffloadedTimeRecursively(ut);
		return ret;
	}

	private double calculateOffloadedTimeRecursively(Method m){
		double ret = 0.0;
		for(Method c : m.getCallees())
			ret += this.calculateOffloadedTimeRecursively(c);
		if(this.toBeOffloaded.contains(m))
//			ret += m.getExclusiveTime()/Configuration.cloudSpeedupFactor;
			ret += Configuration.getRemoteExclusiveExecutionTime(m);
		else
//			ret += m.getExclusiveTime();
			ret += Configuration.getLocalExclusiveExecutionTime(m);
		return ret;	
	}

	
	private double solveWithLP(){
		srcEdgeIdMap = new HashMap<Integer,Integer>();
		dstEdgeIdMap = new HashMap<Integer,Integer>();
		edgeIdMap = new HashMap<Pair<Integer,Integer>,Integer>();
		outNodeMap = new HashMap<Integer,Set<Integer>>();// outgoing edge set except for src
		inNodeMap = new HashMap<Integer,Set<Integer>>();// incoming edge set except for dst
		edgeIdCapMap = new HashMap<Integer,Double>();
		int evId = 0;
		for(Map.Entry<Integer, Double> entry: srcCapMap.entrySet()){
			int si = entry.getKey();
			srcEdgeIdMap.put(si,evId);
			Set<Integer> inNodeSet = inNodeMap.get(si);
			if(inNodeSet != null)
				throw new RuntimeException("There might be more than one edge from src to a node!");
			inNodeSet = new HashSet<Integer>();
			inNodeSet.add(SRC_ID);
			inNodeMap.put(si, inNodeSet);
			edgeIdCapMap.put(evId, entry.getValue());
			evId++;
		}
		
		for(Map.Entry<Integer, Double> entry : dstCapMap.entrySet()){
			int ti = entry.getKey();
			dstEdgeIdMap.put(ti,evId);
			Set<Integer> outNodeSet = outNodeMap.get(ti);
			if(outNodeSet != null)
				throw new RuntimeException("There might be more than one edge a node to dst!");
			outNodeSet = new HashSet<Integer>();
			outNodeSet.add(DST_ID);
			outNodeMap.put(ti, outNodeSet);
			edgeIdCapMap.put(evId, entry.getValue());
			evId++;
		}
		
		for(Map.Entry<Pair<Integer, Integer>,Double> entry : this.edgeCapMap.entrySet()){
			Pair<Integer,Integer> edge = entry.getKey();
			edgeIdMap.put(edge, evId);
			Set<Integer> outNodeSet = outNodeMap.get(edge.getFirst());
			if(outNodeSet == null){
				outNodeSet = new HashSet<Integer>();
				outNodeMap.put(edge.getFirst(), outNodeSet);
			}
			outNodeSet.add(edge.getSecond());
			Set<Integer> inNodeSet = inNodeMap.get(edge.getSecond());
			if(inNodeSet == null){
				inNodeSet = new HashSet<Integer>();
				inNodeMap.put(edge.getSecond(), inNodeSet);
			}
			inNodeSet.add(edge.getFirst());
			edgeIdCapMap.put(evId, entry.getValue());
			evId++;
		}
		
		try {
			GRBEnv env = new GRBEnv("mcc_gurobi.log");
			GRBModel model = new GRBModel(env);
			GRBVar[] x = model.addVars(evId, GRB.CONTINUOUS);
			model.update();
			GRBLinExpr expr = new GRBLinExpr();
			//The objective function
			for(int var : srcEdgeIdMap.values()){
				expr.addTerm(1, x[var]);
			}
			model.setObjective(expr, GRB.MAXIMIZE);

			program.destory();
			System.gc();

			int n = 0;

			List<Pair<Integer,Double>> edgeCaps = new ArrayList<Pair<Integer,Double>>();
			for(Map.Entry<Integer, Double> srcCap : srcCapMap.entrySet()){
				edgeCaps.add(new Pair<Integer,Double>(srcEdgeIdMap.get(srcCap.getKey()), srcCap.getValue()));
			}
			for(Map.Entry<Integer, Double> dstCap : dstCapMap.entrySet()){
				edgeCaps.add(new Pair<Integer,Double>(dstEdgeIdMap.get(dstCap.getKey()), dstCap.getValue()));
			}
			for(Map.Entry<Pair<Integer,Integer>, Double> edgeCap : edgeCapMap.entrySet()){
				edgeCaps.add(new Pair<Integer,Double>(edgeIdMap.get(edgeCap.getKey()), edgeCap.getValue()));
			}
			
			for(Pair<Integer,Double> ecp : edgeCaps){
				n++;
				expr = new GRBLinExpr();
				expr.addTerm(1, x[ecp.getFirst()]);
				model.addRange(expr, 0,ecp.getSecond(), "capacity constraint "+n);
			}


			//The constraints on flow equation
			List<Pair<Set<Integer>,Set<Integer>>> flows = new ArrayList<Pair<Set<Integer>,Set<Integer>>>();
			for(Map.Entry<Integer, Set<Integer>> outNodeEntry : outNodeMap.entrySet()){
				int node = outNodeEntry.getKey();
				if(this.isSrc(node) || this.isDst(node))
					throw new RuntimeException("Src and dst shouldn't be kept tracked of in and out!");
				Set<Integer> out = new HashSet<Integer>();
				for(int o : outNodeEntry.getValue()){
						if(this.isSrc(o))
							throw new RuntimeException("Now edge should flow into src!");
						if(this.isDst(o))
							out.add(this.dstEdgeIdMap.get(node));
						else
							out.add(this.edgeIdMap.get(new Pair<Integer,Integer>(node, o)));
				}
				Set<Integer> in = new HashSet<Integer>();
				for(int i : inNodeMap.get(node)){
					if(this.isDst(i))
						throw new RuntimeException("Now edge should flow out of dst!");
					if(this.isSrc(i))
						in.add(this.srcEdgeIdMap.get(node));
					else
						in.add(this.edgeIdMap.get(new Pair<Integer,Integer>(i,node)));				
				}
				flows.add(new Pair<Set<Integer>,Set<Integer>>(in,out));
			}
			
			for(Pair<Set<Integer>,Set<Integer>> flow : flows){
				n++;
				GRBLinExpr expr1 = new GRBLinExpr();
				for(Integer i : flow.getFirst())
					expr1.addTerm(1, x[i]);
				GRBLinExpr expr2 = new GRBLinExpr();
				for(Integer i : flow.getSecond())
					expr2.addTerm(1, x[i]);
				model.addConstr(expr1, GRB.EQUAL, expr2, "flow equation "+n);
			}
			model.optimize();
			double solution[] = model.get(GRB.DoubleAttr.X, x);
			this.toBeOffloaded = this.computeOffloadedMethods(solution);
			return model.get(GRB.DoubleAttr.ObjVal);
		} catch (GRBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	
	
	/**
	 * Adjust capacity for edges with none vertices from boundary configurations to infinity
	 * 
	 */
	private void adjustCapacityAccordingBoundaryConfig(){
		
		//Set<String> nameSet = new HashSet<String>();
		
		System.out.println("src_map.size : " + srcCapMap.size());
		System.out.println("dst_map.size : " + dstCapMap.size());
		System.out.println("edge_map.size : " + edgeCapMap.size());
		
		int total = 0, adjusted = 0, skipped = 0;
		Set<Integer> dataIdSet = new HashSet<Integer>();
		for(Map.Entry<Pair<Integer,Integer>, Double>  entry: this.edgeCapMap.entrySet()){
			//System.out.println( entry.getKey().getFirst() + "\t" + entry.getKey().getSecond() );
			
			++total;
			if( isDataId( entry.getKey().getFirst() )  || isDataId( entry.getKey().getSecond() )){
				++skipped;
				if( isDataId( entry.getKey().getFirst() ) ) dataIdSet.add(entry.getKey().getFirst());
				if( isDataId( entry.getKey().getSecond() ) ) dataIdSet.add(entry.getKey().getSecond());
				
				continue;
			}
			
			Method from = getMethod(entry.getKey().getFirst());
			Method to = getMethod(entry.getKey().getSecond());
			
			//nameSet.add(from.methName());
			//nameSet.add(to.methName());
			
			//non-boundary (e.g. lib functions) are not allowed to be offloaded back
			/*if(program.inCuttingBoundary(to.methName()) == false){
				if( program.inCuttingBoundary(from.methName()) &&  to.getCaller().equals(from) ){
					this.edgeCapMap.put(entry.getKey(), this.getLocalTime() ); // use total Local time as INF
					++adjusted;
					continue;
				}
			}*/
			
			
			if( program.inCuttingBoundary(from.methName()) == false && program.inCuttingBoundary(to.methName()) == false ){
				this.edgeCapMap.put(entry.getKey(), this.getLocalTime() ); // use total Local time as INF
				++adjusted;
			}
		}
		
		/*
		 * We cannot adjust edge between source/sink and a method, 
		 * since a method has to be decided to put which side
		 * Our purpose is to make the cut between methods happen at boundary 
		for(Map.Entry<Integer, Double> entry : this.srcCapMap.entrySet()){
			++total;
			
			if( isDataId( entry.getKey() ) ) continue;
			Method m = getMethod( entry.getKey() );
			if( program.inCuttingBoundary(m.methName()) == false){
				++adjusted;
				this.srcCapMap.put(entry.getKey(), INF);
			}
		}
		
		for(Map.Entry<Integer, Double> entry : this.dstCapMap.entrySet()){
			++total;
			
			if( isDataId( entry.getKey() ) ) continue;
			Method m = getMethod( entry.getKey() );
			if( program.inCuttingBoundary(m.methName()) == false){
				++adjusted;
				this.dstCapMap.put(entry.getKey(), INF);
			}
		}*/
		
		//System.out.println("Methods in the trace:");
		//for(String s : nameSet) System.out.println(s);
		System.out.println("adjustCapacityBoundary: total=" + total + ", adjusted=" + adjusted +", skipped=" + skipped + ", dataSet.size=" + dataIdSet.size());

	
		// thread start point has to be on the mobile side
		for(Map.Entry<Integer, Double> entry : this.dstCapMap.entrySet()){
			if( isDataId( entry.getKey() ) ) continue;
			Method m = getMethod( entry.getKey() );
			if( m.methName().startsWith("thread_")){
				this.dstCapMap.put(entry.getKey(), this.getLocalTime() ); // use total Local time as INF
			}
		}
	}

	/**
	 * To correctly get the result from pseudoflow, add "#define DISPLAY_CUT" t othe source file.
	 * @return
	 */
	private double solveWithPseudoFlow(){
		
		adjustCapacityAccordingBoundaryConfig();
		
		try {
			File solver = new File(Configuration.PSEUDO_FLOW_PATH);
			Utils.printLogWithTime("Start the pseudo flow solver:");
			ProcessBuilder pb = new ProcessBuilder(solver.getAbsolutePath());
			pb.redirectErrorStream(true);
			final Process p = pb.start();
			int nNodes = this.nodeIdSeed - 1;
			int nArcs = this.srcCapMap.size()+this.dstCapMap.size()+this.edgeCapMap.size();
			PrintWriter pw = new PrintWriter(p.getOutputStream());
			PrintWriter pw2 = new PrintWriter("./test.dimacs");
			
			pw.println("p max "+nNodes+" "+nArcs);
			pw.println("n 1 s");
			pw.println("n 2 t");
			
			pw2.println("p max "+nNodes+" "+nArcs);
			pw2.println("n 1 s");
			pw2.println("n 2 t");
			
			for(Map.Entry<Integer, Double> entry : this.srcCapMap.entrySet()){
				if( entry.getValue() < 0 || entry.getKey() == 178277){
					System.out.println("Negative weight: " + entry.getValue() + ", method:" + getMethod( entry.getKey() ));
				}
				pw.println("a "+1+" "+entry.getKey()+" "+(int)(entry.getValue().doubleValue()* E));
				pw2.println("a "+1+" "+entry.getKey()+" "+(int)(entry.getValue().doubleValue()*E));
			}
			for(Map.Entry<Integer, Double> entry : this.dstCapMap.entrySet()){
				pw.println("a "+entry.getKey()+" "+2+" "+ (int)(entry.getValue().doubleValue()*E));	
				pw2.println("a "+entry.getKey()+" "+2+" "+ (int)(entry.getValue().doubleValue()*E));	
			}
			for(Map.Entry<Pair<Integer,Integer>, Double>  entry: this.edgeCapMap.entrySet()){
				pw.println("a "+entry.getKey().getFirst()+" "+entry.getKey().getSecond()
						+" "+(int)entry.getValue().doubleValue()*E);
				pw2.println("a "+entry.getKey().getFirst()+" "+entry.getKey().getSecond()
						+" "+(int)entry.getValue().doubleValue()*E);
			}
			pw.flush();
			pw.close();
			
			pw2.flush();
			pw2.close();
			
			Utils.printLogWithTime("Finish input generation.");
			ResultInterpreter ri = new ResultInterpreter(p.getInputStream());

			Future<Pair<Double, Set<Integer>>> futureResult = Utils.executor
					.submit(ri);

			if (p.waitFor() != 0) {
				throw new RuntimeException(
						"The pseudo flow solver did not terminate normally");
			}

			Pair<Double, Set<Integer>> ret = futureResult.get();
			super.toBeOffloaded = new HashSet<Method>();
			for(int mi : ret.getSecond()){
//				if(mi != SRC_ID && !this.auxNodes.contains(mi))
//				if(SRC_ID < mi && DST_ID < mi && mi <= this.maxMethId)
				if(this.isMethID(mi))
					super.toBeOffloaded.add(this.getMethod(mi));
			}
			
			System.out.println("re.first =" + ret.getFirst() );
			
			return ret.getFirst()/E;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
				throw new RuntimeException(e);
		}
	}
	
	private boolean checkSaturated(int edgeId){
		return this.edgeIdCapMap.get(edgeId) - solution[edgeId] < EPS;
	}
	
	protected Set<Method> computeOffloadedMethods(double[] solution) {
		this.solution = solution;
		Set<Integer> offloadedNodes = new HashSet<Integer>();
		for(Integer m : srcCapMap.keySet()){
			int edgeVar = this.srcEdgeIdMap.get(m);
			if(!this.checkSaturated(edgeVar)) // not full
				this.markOffloadedMethodRecursively(m, offloadedNodes);
		}
		Set<Method> offloadedMethods = new HashSet<Method>();
		for(Integer m : offloadedNodes){
			if(this.isMethID(m)){
				Method meth = this.getMethod(m);
				offloadedMethods.add(meth);
			}
		}
		return offloadedMethods;
	}
	
	private void markOffloadedMethodRecursively(Integer m, Set<Integer> offloadedNodes){
		if(offloadedNodes.contains(m))
			return;
		offloadedNodes.add(m);
		if(dstCapMap.containsKey(m)){
			int edgeVar = this.dstEdgeIdMap.get(m);
			if(!this.checkSaturated(edgeVar))
				throw new RuntimeException("The source should not reach destination!");
		}
		Set<Integer> oes = outNodeMap.get(m);
		if(oes != null){
			for(Integer o : oes){
				if(this.isDst(o))
					continue;
				if(!this.checkSaturated(this.edgeIdMap.get(new Pair<Integer,Integer>(m,o))))
					this.markOffloadedMethodRecursively(o, offloadedNodes);
			}
		}
		Set<Integer> ies = inNodeMap.get(m);
		if(ies != null){
			for(Integer i : ies){
				if(this.isSrc(i))
					continue;
				int var = this.edgeIdMap.get(new Pair<Integer,Integer>(i,m));
				if(solution[var] > EPS){
					this.markOffloadedMethodRecursively(i, offloadedNodes);
				}
			}
		}
	}
	
	protected void pinMethod(Method m){
		int mid = this.getMethId(m);
		Double cap = this.dstCapMap.get(mid);
		if(cap == null)
			cap = 0.0;
		cap += this.getLocalTime();
		this.dstCapMap.put(mid, cap);
	}
	
	protected abstract void populateGraph();
	
	class ResultInterpreter implements Callable<Pair<Double, Set<Integer>>> {
		private InputStream resultStream;

		public ResultInterpreter(InputStream resultStream) {
			this.resultStream = resultStream;
		}

		@Override
		public Pair<Double, Set<Integer>> call() throws Exception {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					this.resultStream));
			Set<Integer> cut = new HashSet<Integer>();
			double maxFlow = 0.0;
			String line;
			while ((line = in.readLine()) != null) {
				if(line.startsWith("n ")){
					String tokens[] = line.split("\\s+");
					cut.add(Integer.parseInt(tokens[tokens.length-1]));
				}
				else{// do not print out the mincut solution to avoid flushing the log file.
					Utils.printLogWithTime("SOLVER: "+line);
					if (line.startsWith("s ")) {
						String tokens[] = line.split("\\s+");
						maxFlow = Double.parseDouble(tokens[tokens.length-1]);
					}
				}

			}
			in.close();
			Pair<Double, Set<Integer>> ret = new Pair<Double, Set<Integer>>(
					maxFlow, cut);
			return ret;
		}
	}
	
	protected int generateAuxiliaryNodeId(){
		int ret = nodeIdSeed;
		nodeIdSeed++;
//		this.auxNodes.add(ret);
		return ret;
	}
}