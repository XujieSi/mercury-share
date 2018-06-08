package edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut;

import java.util.Set;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Pair;


// bi-directional stateless
public class MinCutOffloadingAlgorithm2 extends MinCutOffloadingAlgorithm {

	public MinCutOffloadingAlgorithm2(Program p) {
		super(p);
	}


	@Override
	public boolean isStateful() {
		// TODO Auto-generated method stub
		return false;
	}

	private void computeCapsRecursively(Method m){
		Method caller = m.getCaller();
		double lcap = Configuration.getLocalExclusiveExecutionTime(m);
		super.addSrcCapacity(m, lcap);
		double rcap = Configuration.getRemoteExclusiveExecutionTime(m);
		super.addDstCapacity(m, rcap);
		double transferTime = Configuration.getTransportTime(program, m);
		if(caller == null){
			super.addDstCapacity(m, transferTime);
		}else{
			super.addCapacity(caller, m, transferTime);
			super.addCapacity(m, caller, transferTime);
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
				Method last = null;
				double localTime = super.getLocalTime();
				for(Method m : colSet){
					if(last != null){
						super.addCapacity(m, last, localTime);
						super.addCapacity(last, m, localTime);
					}
					last = m;
				}
				if(last != null)
					super.addCapacity(last, commonAncester, localTime);
			}
		}*/
		
		for(Method t : program.getThreads())
			this.computeCapsRecursively(t);
	}
	

}
