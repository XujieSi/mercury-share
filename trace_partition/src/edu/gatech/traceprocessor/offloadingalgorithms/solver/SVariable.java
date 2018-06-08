package edu.gatech.traceprocessor.offloadingalgorithms.solver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.Instruction;

/**
 * The ILP variable to represent 
 * @author xin
 *
 */
public class SVariable{
	private static int VID;
	
	int id;
	Map<Integer,BooleanVariable> solverVars;
	Data d;
	Instruction writer;
	List<Instruction> readers;
	
	public SVariable(Data d, Instruction writer, List<Instruction> readers){
		this.id = VID++;
		this.d = d;
		this.solverVars = new HashMap<Integer,BooleanVariable>();
		this.writer = writer;
		this.readers = readers;
	}
	
	public SVariable(SVariable var){
		this(var.d,var.getWriter(),var.getReaders());
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
	
	public Data getData(){
		return d;
	}
	
	public Instruction getWriter(){
		return writer;
	}
	
	public List<Instruction> getReaders(){
		return readers;
	}
	
	public static void clean(){
		VID = 0;
	}
}
