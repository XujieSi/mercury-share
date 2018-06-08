package old.edu.gatech.traceprocessor.offloadingalgorithms.solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import old.edu.gatech.traceprocessor.Function;
import old.edu.gatech.traceprocessor.VarEntry;
import old.edu.gatech.traceprocessor.Variable;
import edu.gatech.traceprocessor.utils.SortedList;
import edu.gatech.traceprocessor.utils.Utils;

public class SFunction extends Function{
	
	int treePositionID = -1;
//	SortedList<Variable> mSubTreeInput;
//	SortedList<Variable> mSubTreeOutput;
	
	/*
	 * ID of the last node in the
	 * subtree rooted at "this" function
	 */
	int lastSubTreeNodeID;
	
	/*
	 * ID of the last node in the thread
	 * to which "this" function belongs
	 */
	int lastThreadNodeID;
	
	/* 
	 * Earliest reader, not belonging to "this" subtree,
	 * of any value written in "this" subtree 
	 */
	Function earliestNonSubTreeReader; 
	
	/*
	 * Earliest node whose execution doesn't overlap with
	 * "this" subtree, if "this" subtree were to be offloaded
	 */
	int earliestNonOverlappingNodeID;
	
	Map<Integer,BooleanVariable> solverVars;
	
	int subtreeInputSize = 0;
	int subtreeOutputSize = 0;
	
	public SFunction(Function parent, String name, boolean isNative,
			long startTime, int threadId){
		super(parent, name, isNative, startTime, threadId);
		this.solverVars = new HashMap<Integer,BooleanVariable>();
	}

	public boolean addSubtreeInput(VarEntry v){
		if(v.getWriter() == this)
			return false;
		
		Function f = v.getWriter();
		Function p = null;
		while(f != null){
			if(this.getAncestors().contains(f))
			{
				p = f;
				break;
			}
			f = f.getParent();
		}
		if(p == null){
			Utils.printError("no common ancestor");
			throw new RuntimeException();
		}
		if(p == this)
			return false;
		if(mSubtreeInput.add(v)){
			this.subtreeInputSize += v.getSize();
			return true;
		}
		return false;
	}
	
	public boolean addSubtreeOutput(VarEntry v){		
		if(v.getReaders().size()==0)
			return false;
		if(v.getReaders().size()==1){
			if(v.getReaders().iterator().next() == this)
				return false;
		}

		boolean valid = false;
		for(Function f : v.getReaders()){
			Function p = null;
			while(f != null){
				if(this.getAncestors().contains(f))
				{
					p = f;
					break;
				}
				f = f.getParent();
			}
			if(p == null){
				Utils.printError("no common ancestor");
				throw new RuntimeException();
			}
			
			if(p == this){
				continue;
			}else{
				valid = true;
				break;
			}
		}
		if(!valid)
			return false;
		
		if(mSubtreeOutput.add(v)){
			this.subtreeOutputSize+=v.getSize();
			return true;
		}
		return false;
	}
	
	public boolean inlineChild(Function f){
		if(!mChildren.contains(f))
			return false;
		
		int indx = mChildren.indexOf(f);
		mChildren.remove(indx);
		mChildren.addAll(indx, f.getChildren());
		for(Function c : f.getChildren()){
			c.getAncestors().remove(f);
			c.setParent(this);
		}
		
		offloadable &= f.isOffloadable();
		
		for(VarEntry v : f.getInput()){
			Set<Function> readers = v.getReaders();
			readers.remove(f);
			readers.add(this);
			this.addInput(v);
		}
		
		for(VarEntry v : f.getOutput()){
			v.setWriter(this);
			this.addOutput(v);
		}

		f.setParent(null);
		
	/* This is incomplete as we also need to update the subtreeInputs and
	 * subtreeOutputs for all the ancestors of "this"
	 */
	/*	for(Variable v : f.getSubtreeInput()){
			this.addSubtreeInput(v);
		}
		
		for(Variable v : f.getSubtreeOutput()){
			this.addSubtreeOutput(v);
		}
	*/	
		return true;
	}
	
	public BooleanVariable getSolverVar(int idx) {
		return solverVars.get(idx);
	}

	public void addSolverVar(int idx, BooleanVariable solverVar) {
		solverVars.put(idx, solverVar);
	}
	
	public int getTreePostionID(){
		return this.treePositionID;
	}
	
	public int setTreePostionID(int treePositionID){
		return this.treePositionID = treePositionID;
	}
	
	public void setEarliestNonSubTreeReader(Function reader){
		this.earliestNonSubTreeReader = reader;
	}
	
	public Function getEarliestNonSubTreeReader(){
		return this.earliestNonSubTreeReader;
	}
	
	public void setLastSubTreeNodeID(int id){
		this.lastSubTreeNodeID = id;
	}
	
	public int getLastSubTreeNodeID(){
		return this.lastSubTreeNodeID;
	}
	
	public void setLastThreadNodeID(int id){
		this.lastThreadNodeID = id;
	}
	
	public int getLastThreadNodeID(){
		return this.lastThreadNodeID;
	}
	
	public void setEarliestNonOverlappingNodeID(int id){
		this.earliestNonOverlappingNodeID = id;
	}
	
	public int getEarliestNonOverlappingNodeID(){
		return this.earliestNonOverlappingNodeID;
	}

	@Override
	public boolean removeSubtreeInput(VarEntry v) {
		if(this.mSubtreeInput.remove(v)){
			this.subtreeInputSize-=v.getSize();
			return true;
		}
		return false;
	}

	@Override
	public boolean removeSubtreeOutput(VarEntry v) {
		if(this.mSubtreeOutput.remove(v)){
			this.subtreeOutputSize-=v.getSize();
			return true;
		}
		return false;
	}

//	@Override
//	public int getSubtreeInputSize() {
////		if(this.subtreeInputSize!=super.getSubtreeInputSize())
////			throw new RuntimeException("bug");
//		return this.subtreeInputSize;
//	}
//
//	@Override
//	public int getSubtreeOutputSize() {
////		if(this.subtreeOutputSize!=super.getSubtreeOutputSize())
////			throw new RuntimeException("bug");
//		return this.subtreeOutputSize;
//	}
	
}
