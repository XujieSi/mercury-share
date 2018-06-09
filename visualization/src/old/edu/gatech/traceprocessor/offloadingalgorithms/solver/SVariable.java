package old.edu.gatech.traceprocessor.offloadingalgorithms.solver;

import java.util.HashMap;
import java.util.Map;

import old.edu.gatech.traceprocessor.VarEntry;
import old.edu.gatech.traceprocessor.Variable;

public class SVariable extends Variable{
	private static int VID;
	
	int id;
	Map<Integer,BooleanVariable> solverVars;
	
	public SVariable(VarEntry var){
		super(var);
		this.id = VID++;
		this.solverVars = new HashMap<Integer,BooleanVariable>();
	}
	
	public SVariable(Variable var){
		super(var);
		this.solverVars = new HashMap<Integer,BooleanVariable>();
	}

	public int getId() {
		return id;
	}

	public BooleanVariable getSolverVar(int idx) {
		return solverVars.get(idx);
	}

	public void addSolverVar(int idx, BooleanVariable solverVar) {
		solverVars.put(idx, solverVar);
	}
	
}
