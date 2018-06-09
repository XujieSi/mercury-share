package old.edu.gatech.traceprocessor.offloadingalgorithms.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import old.edu.gatech.traceprocessor.Configuration;
import old.edu.gatech.traceprocessor.Function;
import old.edu.gatech.traceprocessor.VarEntry;
import old.edu.gatech.traceprocessor.Variable;
import edu.gatech.traceprocessor.utils.SortedList;
import edu.gatech.traceprocessor.utils.Utils;

public abstract class OffloadingAlgorithm {

//	protected Map<Function, SFunction> funcMap;
	protected Map<Integer, SFunction> funcIDMap;
	protected Map<VarEntry, SVariable> programVarMap;
//	protected Map<Integer, SVariable> programVarIDMap;
	protected Function forest;
	protected List<Constraint> constraints;
	protected Map<Integer, BooleanVariable> varDictionary;
	protected Set<VarEntry> programVarTable;
	protected List<Function> temporalFuncList; 
	protected List<Integer> threadStartIDs;
	protected Set<Function> toBeOffloaded;
	protected Set<Set<Function>> coLocSCCs;
	protected Map<String,Set<Function>> funcNameMap;

	@Deprecated
	boolean cumulativeStatsGenerated = false;

	public OffloadingAlgorithm(Function forest, Set<VarEntry> programVarTable, Set<Set<Function>> coLocSCCs) {
//		this.funcMap = new HashMap<Function, SFunction>();
		this.funcIDMap = new HashMap<Integer, SFunction>();
		this.programVarMap = new HashMap<VarEntry, SVariable>();
//		this.programVarIDMap = new HashMap<Integer, SVariable>();
		this.forest = forest;
		this.constraints = new ArrayList<Constraint>();
		this.varDictionary = new HashMap<Integer, BooleanVariable>();
		this.programVarTable = programVarTable;
		this.temporalFuncList = new ArrayList<Function>();
		this.threadStartIDs = new ArrayList<Integer>();
		this.toBeOffloaded = new HashSet<Function>();
		this.funcNameMap = new HashMap<String, Set<Function>>();
		this.coLocSCCs = new HashSet<Set<Function>>(coLocSCCs);
	}

	public void clear(){
	//	this.funcMap.clear();
		this.funcIDMap.clear();
		this.constraints.clear();
		this.varDictionary.clear();
		this.temporalFuncList.clear();
		this.toBeOffloaded.clear();
	}

	public void optimize() {
		Vector<Function> threads = forest.getChildren();
		int totalLocalTime = 0;
		for(int i = 0; i < threads.size(); i++){
			totalLocalTime += threads.get(i).getExecutionTime();
			/*		clear();
			List<Function> toBeOffloaded = new ArrayList<Function>();
			generateFunctionMap(threads.get(i),0);
			Utils.printLog("Map generated");
			double time = optimize(threads.get(i),toBeOffloaded);
			Utils.printResults("Thread "+i+" Local Time: "+ threads.get(i).getRemoteExecutionTime()*Configuration.cloudSpeedupFactor
					+"\t Optimal Time: "+ time);
			this.printOffloadingPoints(threads.get(i), toBeOffloaded, 0);
			 */		
		}

		generateFunctionMap(forest,0);
		populateThreadIDs();
		Utils.printLog("Map generated");
		double time = optimize(forest);
		Utils.printResults("Local Time: "+ totalLocalTime +"\t Optimal Time: "+ time);
		this.printOffloadingPoints(forest, 0, false);

	}

	abstract public double optimize(Function root);
	
	public void printOffloadingPoints(Function root, int layer, boolean prevOffloaded) {
		/*Utils.printResults("Methods to be offloaded:");
		for(Function f : toBeOffloaded){
			Utils.printResults(f.getName());
		}*/
		if(toBeOffloaded.contains(root) && !prevOffloaded){
//			for(int i = 0; i < layer; i++)
//				Utils.printAsIs(" ");
			Utils.printResults(root.getName()+", start_time = "+root.getStartTime()+", exec_time = "+root.getExecutionTime()+", thread = "+root.getThreadID());
			prevOffloaded = true;
		//	if(root.getChildren().size() == 0)
		//		Utils.printResults("==============================");
		}else if(!toBeOffloaded.contains(root) && prevOffloaded){
//			for(int i = 0; i < layer; i++)
//				Utils.printAsIs(" ");
			Utils.printResults("Offloaded back: " + root.getName() + ", start_time=" + root.getStartTime());
			prevOffloaded = false;
		}
	//	else{
			Vector<Function> children = root.getChildren();
			for(int i = 0; i < children.size(); i++){
				printOffloadingPoints(children.get(i), layer +1, prevOffloaded);
			}
	//	}
	}

	public void populateThreadIDs(){
	//	if(funcMap.isEmpty()){
		if(funcIDMap.isEmpty()){
			Utils.printError("Call generateFunctionMap before populateThreadIDs");
			System.exit(0);
		}
		
		for(Function f : forest.getChildren()){
		//	SFunction extF = funcMap.get(f);
		//	threadStartIDs.add(extF.getId());
			SFunction extF = (SFunction)f;
			threadStartIDs.add(extF.getTreePostionID());
		}
		
		Collections.sort(threadStartIDs);
		Utils.printLog("Thread IDs: " + threadStartIDs);

		for(Iterator<Integer> iter = threadStartIDs.iterator(); iter.hasNext();){
			Integer id = iter.next();
			Integer lastID = funcIDMap.get(id).getLastSubTreeNodeID();
			for(int i = id; i <= lastID; i++){
				SFunction node = funcIDMap.get(i);
				node.setLastThreadNodeID(lastID);
			}
		}
	}
	
	
	public void generateParallelizationInfo(){
	//	if(funcMap.isEmpty() || cumulativeStatsGenerated == false){
		if(funcIDMap.isEmpty() || cumulativeStatsGenerated == false){
			Utils.printError("Call generateFunctionMap and generateCumulativeDataStats before generateParallelizationInfo");
			System.exit(0);
		}
		for(Function f : temporalFuncList){
		//	SFunction extF = funcMap.get(f);
			SFunction extF = (SFunction) f;
			Function earliestReader = null;
			int earliestReaderID = Integer.MAX_VALUE;
			for(VarEntry v : extF.getSubtreeOutput()){
				int tempID = 0;
				for(Function reader : v.getReaders()){
					//tempID = funcMap.get(reader).getId();
					tempID = ((SFunction)reader).getTreePostionID();
					if(tempID < earliestReaderID && tempID > extF.getLastSubTreeNodeID() && tempID <= extF.getLastThreadNodeID() ){
						earliestReader = reader;
						earliestReaderID = tempID;
					}
				}
			}
			
			extF.earliestNonSubTreeReader = earliestReader;
			
			int ub = extF.getLastThreadNodeID();
			int lb = extF.getLastSubTreeNodeID() + 1;
			
			double remoteTime = Configuration.getLocalRemainderExecutionTime(extF) + Configuration.getTransportTime(extF);
			double lbTime = f.getEndTime();
			double ubTime = f.getEndTime() + remoteTime;
			int earliestNonOverlappingNodeID = lb > ub? -1 : binSearch(lb, ub, lbTime, ubTime);
			if(earliestNonOverlappingNodeID == ub){
			//	if(funcIDMap.get(ub).getBaseFunction().getStartTime() < ubTime) earliestNonOverlappingNodeID++;
				if(funcIDMap.get(ub).getStartTime() < ubTime) earliestNonOverlappingNodeID++;
			}
			extF.setEarliestNonOverlappingNodeID(earliestNonOverlappingNodeID);
		}
	}
	
	private int binSearch(int lb, int ub, double lbTime, double ubTime){
		if(lb == ub){
			return lb;
		}

		int id = (lb + ub)/2;
		SFunction f = funcIDMap.get(id);
	//	if(f.getBaseFunction().getStartTime() < lbTime){
		if(f.getStartTime() < lbTime){
			Utils.printError("error");
		}
		
	//	if(f.getBaseFunction().getStartTime() < ubTime){
		if(f.getStartTime() < ubTime){
			return binSearch(id+1, ub, lbTime, ubTime);
		}else{
			return binSearch(lb, id, lbTime, ubTime);
		}
	}

	public void generateProgramVarMap(){
//		int id = 0;
		for(VarEntry var : programVarTable){
			SVariable extVar = new SVariable(var); 
			programVarMap.put(var, extVar);
//			programVarIDMap.put(id, extVar);
//			id++;
		}
	}
	
	public int generateFunctionMap(Function currNode, int treePositionID){
		if(currNode==null) return treePositionID;

		SFunction extFunc = null;
		//process current Node
		if(currNode.getParent() != null){
		//	extFunc = new SFunction(currNode, id);
		//	funcMap.put(currNode, extFunc);
			extFunc = (SFunction)currNode;
			extFunc.setTreePostionID(treePositionID);
			funcIDMap.put(treePositionID, extFunc);
			temporalFuncList.add(currNode);
			treePositionID++;
			
			Set<Function> sameNameFuncs = funcNameMap.get(currNode.getName());
			if(sameNameFuncs == null){
				sameNameFuncs = new HashSet<Function>();
				funcNameMap.put(currNode.getName(), sameNameFuncs);
			}
			sameNameFuncs.add(currNode);
		}

		for(Function child : currNode.getChildren()){
			treePositionID = generateFunctionMap(child, treePositionID);
		}
		
		if(currNode.getParent() != null){
			extFunc.setLastSubTreeNodeID(treePositionID-1);
		}

		return treePositionID;
	}

	/**
	 * This function has been moved to trace processor
	 * @param root
	 */
	@Deprecated
	public void generateDataStatsRecursive2(Function root){
		if(root==null)return;

		SFunction extRoot = null;
		if(root.getParent() != null){
		//	extRoot = funcMap.get(root);
			extRoot = (SFunction)root;
		}
		
		Vector<Function> children = root.getChildren();
		for(int i = 0; i < children.size(); i++){
			generateDataStatsRecursive2(children.get(i));
			if(extRoot != null){
				//if(root.isOffloadable()){
			//	SFunction extChild = funcMap.get(children.get(i));
				SFunction extChild = (SFunction)children.get(i);
				Set<VarEntry> vars = extChild.getSubtreeInput();
				for(VarEntry v : vars){
					extRoot.addSubtreeInput(v);
				}
				vars = extChild.getSubtreeOutput();
				for(VarEntry v : vars){
					extRoot.addSubtreeOutput(v);
				}
				//}
			}
		}
	
		cumulativeStatsGenerated = true;
		throw new RuntimeException("This function is abandoned");
	}

	@Deprecated
	public void generateDataStatsRecursive(Function currNode){
		if(currNode==null)return;


		for(Function child : currNode.getChildren()){
			generateDataStatsRecursive(child);
		}

		if(currNode.getParent() != null){
		//	SFunction extCurr = funcMap.get(currNode);
			SFunction extCurr = (SFunction)currNode;
			Set<Function> allSubtreeElements = new HashSet<Function>();
			allSubtreeElements.add(currNode);
			allSubtreeElements.addAll(currNode.getChildren());

			for(VarEntry var : currNode.getInput()){
				if(!allSubtreeElements.contains(var.getWriter())){
					extCurr.getSubtreeInput().add(var);
					//		extCurr.mInputSize += var.size;
				}
			}
			for(VarEntry var : currNode.getOutput()){
				if(!allSubtreeElements.containsAll(var.getReaders())){
					extCurr.getSubtreeOutput().add(var);
					//		extCurr.mOutputSize += var.size;
				}
			}

			for(Function child : currNode.getChildren()){
		//		SFunction extChild = funcMap.get(child);
		//		assert(extChild!=null);		
				SFunction extChild = (SFunction)child;
				for(VarEntry var : extChild.getSubtreeInput()){
					if(!allSubtreeElements.contains(var.getWriter())){
						extCurr.getSubtreeInput().add(var);
						//			extCurr.mInputSize += var.size;
					}
				}


				for(VarEntry var : extChild.getSubtreeOutput()){
					if(!allSubtreeElements.containsAll(var.getReaders())){
						extCurr.getSubtreeOutput().add(var);
						//			extCurr.mOutputSize += var.size;
					}
				}
			}
		}
		cumulativeStatsGenerated = true;
		throw new RuntimeException("This function has been abandoned");
	}

/*	public void generateFunctionMap(Function currNode){
		int id = 0;
		if(currNode==null)return;

		//Traversing the n-ary tree in a pre-order manner
		Stack<Function> stack=new Stack<Function>();
		stack.add(currNode);
		while(!stack.isEmpty()){
			currNode = stack.pop();
			for (ListIterator<Function> iterator = currNode.getChildren().listIterator(currNode.getChildren().size()); iterator.hasPrevious();) {
				Function child = iterator.previous();
				stack.add(child);
			}
			//process current Node
			if(currNode.getParent() != null){
				ExtFunction extFunc = new ExtFunction(currNode, id); 
				funcMap.put(currNode, extFunc);
				funcIDMap.put(id, extFunc);
				temporalFuncList.add(currNode);
				id++;
			}
			
			//TODO
			//The function is incomplete since lastSubTreeNodeID hasn't been updated.
		}
	}
*/
	
	@Deprecated
	public void generateDataStatsNonRecursive(Function currNode){
		if(currNode==null)return;

		//Traversing the n-ary tree in a post-order manner
		Function prevNode = null;
		Stack<Function> stack=new Stack<Function>();
		stack.add(currNode);
		while(!stack.isEmpty()){
			currNode = stack.peek();
			if(prevNode == null || !(prevNode==currNode || currNode.getChildren().contains(prevNode))){
				for (ListIterator<Function> iterator = currNode.getChildren().listIterator(currNode.getChildren().size()); iterator.hasPrevious();) {
					Function child = iterator.previous();
					stack.add(child);
				}
			}else if(prevNode == currNode){
				//process current node:a leaf node
				assert(currNode.getChildren().size()==0);
				currNode = stack.pop();
				if(currNode.getParent() != null){
			//		SFunction extCurr = funcMap.get(currNode);
			//		assert extCurr!=null;
					SFunction extCurr = (SFunction)currNode;
					
					for(VarEntry var : currNode.getInput()){
						if(var.getWriter()!=currNode){
							extCurr.getSubtreeInput().add(var);
							//		extCurr.mInputSize += var.size;
						}
					}
					for(VarEntry var : currNode.getOutput()){
						if(!(var.getReaders().size()==1 && var.getReaders().contains(currNode))){
							extCurr.getSubtreeOutput().add(var);
							//		extCurr.mOutputSize += var.size;
						}
					}
					//		extCurr.mInput.addAll(currNode.mInput);
					//		extCurr.mOutput.addAll(currNode.mOutput);
					//		extCurr.mInputSize = currNode.getInputSize();
					//		extCurr.mOutputSize = currNode.getOutputSize();
				}
			}else{
				//process current node:a non-leaf node
				currNode = stack.pop();
				if(currNode.getParent() != null){
			//		SFunction extCurr = funcMap.get(currNode);
			//		assert(extCurr!=null);
					SFunction extCurr = (SFunction)currNode;

					Set<Function> allSubtreeElements = new HashSet<Function>();
					allSubtreeElements.add(currNode);
					allSubtreeElements.addAll(currNode.getChildren());

					for(VarEntry var : currNode.getInput()){
						if(!allSubtreeElements.contains(var.getWriter())){
							extCurr.getSubtreeInput().add(var);
							//		extCurr.mInputSize += var.size;
						}
					}
					for(VarEntry var : currNode.getOutput()){
						if(!allSubtreeElements.containsAll(var.getReaders())){
							extCurr.getSubtreeOutput().add(var);
							//		extCurr.mOutputSize += var.size;
						}
					}

					for(Function child : currNode.getChildren()){
					//	SFunction extChild = funcMap.get(child);
					//	assert(extChild!=null);
						SFunction extChild = (SFunction)child;
						for(VarEntry var : extChild.getSubtreeInput()){
							if(!allSubtreeElements.contains(var.getWriter())){
								extCurr.getSubtreeInput().add(var);
								//			extCurr.mInputSize += var.size;
							}
						}


						for(VarEntry var : extChild.getSubtreeOutput()){
							if(!allSubtreeElements.containsAll(var.getReaders())){
								extCurr.getSubtreeOutput().add(var);
								//			extCurr.mOutputSize += var.size;
							}
						}
					}
				}
			}
			prevNode = currNode;
		}
		cumulativeStatsGenerated = true;
		throw new RuntimeException("This function has been abandoned");
	}
	
}
