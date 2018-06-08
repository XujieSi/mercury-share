package edu.gatech.traceprocessor.offloadingalgorithms.solver;

import java.util.HashMap;
import java.util.Map;

import edu.gatech.traceprocessor.parser.Method;

public class SMethod {
	Map<Integer,BooleanVariable> solverVars;
	Method m;
	
	public SMethod(Method m){
		this.m = m;
		this.solverVars = new HashMap<Integer,BooleanVariable>();
	}
	
	
	public BooleanVariable getSolverVar(int idx) {
		return solverVars.get(idx);
	}

	public void addSolverVar(int idx, BooleanVariable solverVar) {
		solverVars.put(idx, solverVar);
	}
	
	public Method getMethod(){
		return m;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m == null) ? 0 : m.hashCode());
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
		SMethod other = (SMethod) obj;
		if (m == null) {
			if (other.m != null)
				return false;
		} else if (!m.equals(other.m))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "SMethod [m=" + m + "]";
	}
	
	
}
