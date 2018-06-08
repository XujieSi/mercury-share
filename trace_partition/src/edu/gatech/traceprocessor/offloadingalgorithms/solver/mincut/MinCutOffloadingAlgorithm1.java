package edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut;

import java.util.Set;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Pair;



// uni-directional stateless
public class MinCutOffloadingAlgorithm1 extends MinCutOffloadingAlgorithm {

	public MinCutOffloadingAlgorithm1(Program p) {
		super(p);
	}


	@Override
	public boolean isStateful() {
		// TODO Auto-generated method stub
		return false;
	}

	private void computeCapsRecursively(Method m){
		Method caller = m.getCaller();
//		int mid = super.getMethId(m);
//		Double lcap = super.srcCapMap.get(mid);
//		if(lcap == null)
//			lcap = 0.0;
		double lcap = Configuration.getLocalExclusiveExecutionTime(m);
		
		if(lcap < -1e-6){
			System.out.println("computeCapsRecursively: negative weight: " + lcap + ", method: "+ m );
		}
		
//		super.srcCapMap.put(mid, lcap);
		super.addSrcCapacity(m, lcap);
//		Double rcap = super.dstCapMap.get(mid);
//		if(rcap == null)
//			rcap = 0.0;
		double rcap = Configuration.getRemoteExclusiveExecutionTime(m);
//		super.dstCapMap.put(mid, rcap);
		super.addDstCapacity(m, rcap);
		double transferTime = Configuration.getTransportTime(program, m);
		if(caller == null){
			super.addDstCapacity(m, transferTime);
		}else{
//			int pid =  super.getMethId(caller);
			double localTime = this.getLocalTime();   // Here, local time is used as INF
			super.addCapacity(caller, m, localTime);
//			this.edgeCapMap.put(new Pair<Integer,Integer>(pid,mid), localTime);
			super.addCapacity(m, caller, transferTime);
//			this.edgeCapMap.put(new Pair<Integer,Integer>(mid,pid),transferTime);
		}
		for(Method c : m.getCallees())
			this.computeCapsRecursively(c);
	}


	@Override
	protected void populateGraph() {
		processPinAndColocation();
		
		/*
		//pin
		for(Method m : program.getPinnedMethods()){
			this.pinMethod(m);
		}

		//colocation
		for(Set<Method> colSet : program.getColocSet()){
			Method commonAncester = program.findCommonAncestor(colSet);
			if(commonAncester == null){
				for(Method m : colSet){
					this.pinMethod(m);
				}
			}
			else{
//				int ancesterId = super.getMethId(commonAncester);
				for(Method m : colSet){
//					int mid = super.getMethId(m);
//					super.edgeCapMap.put(new Pair<Integer,Integer>(ancesterId,mid),super.getLocalTime());
//					super.edgeCapMap.put(new Pair<Integer,Integer>(mid,ancesterId),super.getLocalTime());
					super.addCapacity(commonAncester, m, super.getLocalTime());
					super.addCapacity(m, commonAncester, super.getLocalTime());
				}
				System.out.println("colSet.size : " + colSet.size());
			}
		}*/
		
		for(Method t : program.getThreads())
			this.computeCapsRecursively(t);
	}

}
