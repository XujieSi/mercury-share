package edu.gatech.traceprocessor.offloadingalgorithms.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.Instruction;
import edu.gatech.traceprocessor.parser.InstructionVisitor;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.MethodEntry;
import edu.gatech.traceprocessor.parser.MethodExit;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.parser.Read;
import edu.gatech.traceprocessor.parser.Write;
import edu.gatech.traceprocessor.utils.Utils;

public abstract class OffloadingAlgorithm {

	protected Map<Integer, Method> methIDMap;
	protected Program program;
	protected List<Constraint> constraints;
	protected List<Constraint> heuristics; // These are heuristics are used to speed up the solver.
										  // The idea is to add heuristics as constraints and get
										  // some solution. We then remove the heuristics and use
										  // the solution as a warm start.
//	protected Map<Integer, BooleanVariable> varDictionary;
	protected List<Integer> threadStartIDs;
	protected Set<Method> toBeOffloaded;
	protected Set<Set<Method>> coLocSCCs;
	protected Map<String,Set<Method>> methNameMap;
	protected Map<Method,SMethod> methVarMap;
	protected Set<String> methodsToMark;

	public OffloadingAlgorithm(Program p) {
		this.methIDMap = new HashMap<Integer, Method>();
		this.program = p;
		this.constraints = new ArrayList<Constraint>();
		this.heuristics = new ArrayList<Constraint>();
//		this.varDictionary = new HashMap<Integer, BooleanVariable>();
		this.threadStartIDs = new ArrayList<Integer>();
		this.toBeOffloaded = new HashSet<Method>();
		this.methVarMap = new HashMap<Method,SMethod>();
		this.coLocSCCs = p.getColocSet();
		this.methodsToMark = new HashSet<String>();
	}

	public void clear(){
		this.methIDMap.clear();
		this.constraints.clear();
		this.heuristics.clear();
//		this.varDictionary.clear();
		this.toBeOffloaded.clear();
		this.coLocSCCs.clear();
		this.methNameMap.clear();
		this.methodsToMark.clear();
	}

	public void optimize() {
		List<Method> threads = program.getThreads();
		int totalLocalTime = 0;
		Method uiThread = threads.get(0);
		for(Method t : threads){
			totalLocalTime += t.getInclusiveTime();
			populateSMethodMapRecursively(t);
			if(t.getThreadID() < uiThread.getThreadID())
				uiThread = t;
		}

		double time = optimize(program);
		Utils.printResults("Local Time: "+ totalLocalTime +"\t Optimal Time: "+ time+"\t UI Time: "+uiThread.getInclusiveTime());
		for(Method t : threads)
			this.printOffloadingPoints(t, 0, false);
		this.printUnmarkedMethods();
//		Utils.printLogWithTime("Checking result feasibility in terms of concurrency.");
//		if(this.checkConcurrency(isStateful()))
//			Utils.printLogWithTime("Pass!");
//		else
//			Utils.printLogWithTime("Fail!");
	}
	
	private void printUnmarkedMethods(){
		Utils.printLogWithTime("WARNING: Methods below are offloaded but not marked.");
		for(String meth : methodsToMark)
			Utils.printLogWithTime(meth);
	}
	
	private void populateSMethodMapRecursively(Method m){
		this.methVarMap.put(m, new SMethod(m));
		for(Method c : m.getCallees())
			populateSMethodMapRecursively(c);
	}

	public abstract double optimize(Program p);
	public abstract boolean isStateful();
	
	public void printOffloadingPoints(Method root, int layer, boolean prevOffloaded) {
		if(toBeOffloaded.contains(root) && program.isMethodPinned(root)){
			this.methodsToMark.add(root.methName());
		}
		if(toBeOffloaded.contains(root) && !prevOffloaded){
			String layerIndent = "";
			for(int i = 0 ; i < layer; i ++)
				layerIndent +=" ";
			Utils.printResults(layerIndent+root.methName()+", start_time = "+root.getStartTime()+", exec_time = "+root.getInclusiveTime()+", thread = "+root.getThreadID());
			prevOffloaded = true;
		}else if(!toBeOffloaded.contains(root) && prevOffloaded){
			String layerIndent = "";
			for(int i = 0 ; i < layer; i ++)
				layerIndent +=" ";
			Utils.printResults(layerIndent+"Offloaded back: " + root.methName() + ", start_time=" + root.getStartTime()+", exec_time = "+root.getInclusiveTime()+", thread = "+root.getThreadID());
			prevOffloaded = false;
		}
			List<Method> children = root.getCallees();
			for(int i = 0; i < children.size(); i++){
				printOffloadingPoints(children.get(i), layer +1, prevOffloaded);
			}
	}
	
	/**
	 * Check if the solution can generate a valid schedule
	 */
	public boolean checkConcurrency(boolean ifStateful){
		Map<Instruction,Set<Instruction>> happensBeforeMap = new HashMap<Instruction, Set<Instruction>>();
		List<TreeSet<Instruction>> compressedThreads = new ArrayList<TreeSet<Instruction>>();
		//Extract instructions from each thread affecting the schedule: 1. thread-shared data accesses
		//2. cloud-mobile switch
		for(Method t : program.getThreads()){
			ConcurVisitor v = new ConcurVisitor();
			v.concurEvents = new TreeSet<Instruction>();
			t.visitPreOrder(v);
			compressedThreads.add(v.concurEvents);
		}
		//Generate happens-before order between instructions within a thread
		for(TreeSet<Instruction> ct : compressedThreads){
			Instruction li = null;
			for(Instruction i : ct){
				if(li != null){
					this.addHappensBefore(happensBeforeMap, li, i);
				}
				li = i;
			}
		}
		//Generate happens-before order based on data read-write
		for(Data d : program.getData().values()){
			if(d.isThreadShared()){
				for(List<Instruction> ag : d.groupAccessors()){
					Iterator<Instruction> it = ag.iterator();
					Instruction w = it.next();
					boolean isWLocal = isLocal(w);
					while(it.hasNext()){
						Instruction r = it.next();
						if(isWLocal){
							if(isLocal(r)){
								this.addHappensBefore(happensBeforeMap, w, r);
							}else{
								Instruction sw = this.findLastSwitchPoint(compressedThreads, r);
								this.addHappensBefore(happensBeforeMap, w, sw);
							}
						}
						else{
							if(isLocal(r)){
								Instruction sw = this.findNextSwitchPoint(compressedThreads, w);
								this.addHappensBefore(happensBeforeMap, sw, r);
							}else{
								if(ifStateful){
									this.addHappensBefore(happensBeforeMap, w, r);
								}else{
									Instruction sw1 = this.findNextSwitchPoint(compressedThreads,w);
									Instruction sw2 = this.findLastSwitchPoint(compressedThreads, r);
									this.addHappensBefore(happensBeforeMap,sw1,sw2);
								}
							}
						}
					}
				}
			}
		}
		//Detect cycles in happens before relation
		boolean changed = false;
		do{
			changed = false;
			for(Map.Entry<Instruction, Set<Instruction>> entry : happensBeforeMap.entrySet()){
				Set<Instruction> instsToGrow = new HashSet<Instruction>();
				Set<Instruction> iaSet = entry.getValue();
				for(Instruction ia : iaSet){
					Set<Instruction> iaaSet = happensBeforeMap.get(ia);
					if(iaaSet!=null){
						instsToGrow.addAll(iaaSet);
						if(iaaSet.contains(entry.getKey())){
							Utils.printLogWithTime("Source node: " + entry.getKey() + " " + entry.getKey().lineNum);
							Utils.printLogWithTime("Dest set: "+ia +" "+ia.lineNum);
							return false;
						}
					}
				}
				changed |= iaSet.addAll(instsToGrow);
			}
		}while(changed);
		return true;
	}

	private Instruction findLastSwitchPoint(List<TreeSet<Instruction>> threads, Instruction i){
		for(TreeSet<Instruction> t : threads){
			Instruction head = t.iterator().next();
			if(head.getThreadID() == i.getThreadID()){
				TreeSet<Instruction> prefix = (TreeSet<Instruction>)t.headSet(i);
				Iterator<Instruction> ri = prefix.descendingIterator();
				while(ri.hasNext()){
					Instruction pi = ri.next();
					if(pi instanceof MethodEntry || pi instanceof MethodExit)
						return pi;
				}
			}
		}
		throw new RuntimeException("No switch point found for instruction: "+i);
	}
	
	private Instruction findNextSwitchPoint(List<TreeSet<Instruction>> threads, Instruction i){
		for(TreeSet<Instruction> t : threads){
			Instruction head = t.iterator().next();
			if(head.getThreadID() == i.getThreadID()){
				TreeSet<Instruction> postfix = (TreeSet<Instruction>)t.tailSet(i, false);
				Iterator<Instruction> iter = postfix.iterator();
				while(iter.hasNext()){
					Instruction pi = iter.next();
					if(pi instanceof MethodEntry || pi instanceof MethodExit)
						return pi;
				}
			}
		}
		throw new RuntimeException("No switch point found for instruction: "+i);
	}
	
	private boolean addHappensBefore(Map<Instruction,Set<Instruction>> happensBeforeMap, Instruction f, Instruction t){
		Set<Instruction> hfSet = happensBeforeMap.get(f);
		if(hfSet == null){
			hfSet = new HashSet<Instruction>();
			happensBeforeMap.put(f, hfSet);
		}
		return hfSet.add(t);
	}
	
	public boolean isMethodPinned(Method m){
		return program.isMethodPinned(m); //&& !(m.isNative() && Configuration.getMethodPinTag(m.methName())==Method.UNDEF);
	}
	
	/**
	 * This visitor will generate a sequence of instructions of a given thread with only
	 * instructions that affect the schedule preserved.
	 * @author xin
	 *
	 */
	class ConcurVisitor implements InstructionVisitor{
		TreeSet<Instruction> concurEvents;

		@Override
		public void visit(Instruction i) {
			if(i instanceof MethodEntry){
				MethodEntry entry = (MethodEntry)i;
				if(isSwitchPoint(entry))
					concurEvents.add(entry);
			}
			else
				if(i instanceof MethodExit){
					MethodExit exit = (MethodExit)i;
					if(isSwitchPoint(exit)){
						concurEvents.add(exit);
					}
				}
				else
					if(i instanceof Write){
						Write w = (Write)i;
						if(w.data.isThreadShared())
							concurEvents.add(w);
					}
					else
						if(i instanceof Read){
							Read r = (Read)i;
							if(r.data.isThreadShared())
								concurEvents.add(r);
						}
		}

	}
	
	private boolean isSwitchPoint(MethodEntry entry){
		if(entry.curMeth != null && entry.getMethod() != null)
			return !entry.curMeth.equals(entry.getMethod());
		return false;
	}
	
	private boolean isSwitchPoint(MethodExit exit){
		if(exit.curMeth != null && exit.getMethod() != null)
			return !exit.curMeth.equals(exit.getMethod());
		return false;
	}
	
	private boolean isCloud(Instruction i){
		if(i instanceof MethodEntry){
			MethodEntry entry = (MethodEntry)i;
			return toBeOffloaded.contains(entry.curMeth);
		}
		else
			if(i instanceof MethodExit){
				MethodExit exit = (MethodExit)i;
				return toBeOffloaded.contains(exit.curMeth);
			}
		return toBeOffloaded.contains(i.getMethod()); 
	}
	
	private boolean isLocal(Instruction i){
		return !isCloud(i);
	}
}
