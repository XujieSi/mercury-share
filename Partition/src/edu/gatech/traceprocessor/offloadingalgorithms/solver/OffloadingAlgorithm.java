package edu.gatech.traceprocessor.offloadingalgorithms.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.FldData;
import edu.gatech.traceprocessor.parser.Instruction;
import edu.gatech.traceprocessor.parser.InstructionVisitor;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.MethodEntry;
import edu.gatech.traceprocessor.parser.MethodExit;
import edu.gatech.traceprocessor.parser.ObjData;
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
	
	private double localTime;
	//the total time after offloading(including gui)
	private double offloadedTime;
//	private double computationTime;
//	private double computationTimeOnPhone;
	private double mobileTime;
	private double cloudTime;
	private double tranferTime;
	private List<Integer> offSizes;
	private double guiLocalTime;
	private double guiOffloadedTime;
	private int maxDepth;

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
		for(Method t : threads){
//			totalLocalTime += t.getInclusiveTime();
			totalLocalTime += Configuration.getLocalInclusiveTime(t);
			populateSMethodMapRecursively(t);
		}

		this.localTime = totalLocalTime;
		this.offloadedTime = optimize(program);
//		this.guiLocalTime = uiThread.getInclusiveTime();
		this.guiLocalTime = 0.0;
		for(Method ut : program.getUIThreads())
			this.guiLocalTime += Configuration.getLocalInclusiveTime(ut);
		this.guiOffloadedTime = this.computeGuiOffloadedTime();
		this.computeOffloadSizes();
		this.computetStatistics();
		Utils.printLogWithTime("Local Time: "+ totalLocalTime +"\t Offloaded Time: "+ this.offloadedTime+"\t UI Time: "+
		//uiThread.getInclusiveTime()+
				this.guiLocalTime+
				"\t UI Offloaded Time: "+this.guiOffloadedTime);
		for(Method t : threads){
			Utils.printLogWithTime("Print offloading points for " + t);
			this.printOffloadingPoints(t, 0, false);
		}
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

	Map<Method, Integer> upSizeMap;
	Map<Method, Set<Data>> upDataMap;
	
	Map<Method, Integer> downSizeMap;
	Map<Method, Set<Data>> downDataMap;
	
	List<Instruction> switchPoints;
	
	/**
	 * Compute the data transfer size of each cloud/mobile switch. For stateful setting, we assume the data exchange
	 * happens at the first mobile/cloud switch after the write.
	 */
	protected void computeOffloadSizes(){
		if(this.isStateful()){
			//inputSet = new HashSet<Data>();
			//inputMap.put(m, inputSet);
			
			upSizeMap = new HashMap<Method,Integer>();
			upDataMap = new HashMap<Method,Set<Data>>();
			
			downSizeMap = new HashMap<Method,Integer>();
			downDataMap = new HashMap<Method,Set<Data>>();
			
			switchPoints = new ArrayList<Instruction>();
			for(Method t : program.getThreads()){
				this.computeSwitchPointsRecursively(t,switchPoints);
			}
			Collections.sort(switchPoints);
			for(Data d : program.getData().values()){
				if((!(d instanceof ObjData)) || Configuration.dataLevel == Configuration.OBJ_LEVEL){
					this.computeOffloadSizesForData(d);
				}else{
					ObjData od = (ObjData)d;
					for(FldData fd : od.getFields().values())
						this.computeOffloadSizesForData(fd);
				}
			}
		}else
			return;
	}
	
	private void computeOffloadSizesForData(Data d){
		for(List<Instruction> accs: d.groupAccessors()){
			Instruction writer = accs.get(0);
			boolean isWriterInCloud = this.toBeOffloaded.contains(writer.getMethod());
			for(int i = 1; i < accs.size(); i++){
				Method r = accs.get(i).getMethod();
				if(isWriterInCloud){
					if(!this.toBeOffloaded.contains(r)){//one reader is in mobile
						Instruction sw = this.findNextSwitch(writer.getLineNum());
						Method switchMethod = null;
						if(sw instanceof MethodEntry){
							switchMethod = ((MethodEntry)sw).curMeth;
						}
						else
							switchMethod = ((MethodExit)sw).curMeth;
						
						Integer downSize = this.downSizeMap.get(switchMethod);
						Set<Data> downData = this.downDataMap.get(switchMethod);
						
						if(downSize == null)
							downSize = 0;
						if(downData == null){
							downData = new HashSet<Data>();
							downDataMap.put(switchMethod, downData);
						}
						
						downSizeMap.put(switchMethod, downSize+d.getSize());
						downData.add(d);
						break;
					}
				}else
					if(this.toBeOffloaded.contains(r)){//one reader is in cloud
						Instruction sw = this.findNextSwitch(writer.getLineNum());
						Method switchMethod = null;
						if(sw instanceof MethodEntry){
							switchMethod = ((MethodEntry)sw).curMeth;
						}
						else
							switchMethod = ((MethodExit)sw).curMeth;
						
						Integer upSize = this.upSizeMap.get(switchMethod);
						Set<Data> upData = this.upDataMap.get(switchMethod);
						
						if(upSize == null)
							upSize = 0;
						
						if(upData == null){
							upData = new HashSet<Data>();
							upDataMap.put(switchMethod, upData);
						}
						
						upSizeMap.put(switchMethod, upSize+d.getSize());
						upData.add(d);
						
						break;
					}
			}
		}	
	}
	
	/**
	 * Compute the switch points in sequence
	 * @param m
	 * @param switchPoints
	 */
	private void computeSwitchPointsRecursively(Method m, List<Instruction> switchPoints){
		Method p = m.getCaller();
		if(p == null && toBeOffloaded.contains(m)){
			switchPoints.add(m.getEntry());
			for(Method c : m.getCallees())
				this.computeSwitchPointsRecursively(c, switchPoints);
			switchPoints.add(m.getExit());
		}
		else
			if(toBeOffloaded.contains(m) ^ toBeOffloaded.contains(p)){
				switchPoints.add(m.getEntry());
				for(Method c : m.getCallees())
					this.computeSwitchPointsRecursively(c, switchPoints);
				switchPoints.add(m.getExit());			
			}
			else{
				for(Method c : m.getCallees())
					this.computeSwitchPointsRecursively(c, switchPoints);
			}
	}
	
	private Instruction findNextSwitch(long lineNum){
//		for(Instruction i : switchPoints)
//			if(i.getLineNum() > lineNum)
//				return i;
//		throw new RuntimeException("Failed to find the switch!");
		int snum = switchPoints.size();
		if(lineNum > this.switchPoints.get(snum-1).getLineNum())
			throw new RuntimeException("Failed to find the switch.");
		int low = 0;
		int high = snum;
		while(high >= low){
			if(lineNum < switchPoints.get(low).getLineNum())
				return switchPoints.get(low); 
			int mid = (high+low)/2;
			if(switchPoints.get(mid).getLineNum() >= lineNum)
				high = mid;
			else
				low = mid+1;
		}
		throw new RuntimeException("Failed to find the switch.");
	}

	protected String niceDisplay(Set<Data> dt){

		Map<String, Set<String>> mp = new HashMap<String, Set<String>>();
		Map<String, Long> sizemp = new HashMap<String, Long>();
		
		for(Data d : dt){
			if(mp.containsKey(d.typeName) == false){
				Set<String> st = new HashSet<String>();
				Long sz = new Long(0);
				mp.put(d.typeName, st);
				sizemp.put(d.typeName, sz);
			}
			if(d instanceof FldData){
				FldData f = (FldData)d;
				mp.get(d.typeName).add(f.fieldTypeName);
				
				Long sz = sizemp.get(d.typeName);
				sz += f.getSize();
				sizemp.put(d.typeName, sz);
			}
			else{
				mp.get(d.typeName).add(d.typeName);
				Long sz = sizemp.get(d.typeName);
				sz += d.getSize();
				sizemp.put(d.typeName, sz);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for(String t : mp.keySet()){
			sb.append("\nType: ");
			sb.append(t);
			sb.append("\nSize: " + sizemp.get(t));
			sb.append("\nFields[");
			for(String t2 : mp.get(t)){
				int k = t2.indexOf(t);
				if(k >=0 && k + t.length() + 2 < t2.length()){
					sb.append( t2.substring( k + t.length() + 2) );
				}
				else
					sb.append(t2);
				sb.append(", ");
			}
			sb.append("]\n");
		}
		return sb.toString();
	}
	
	protected String niceDisplay_0(Set<Data> dt){
		
		if(dt == null) return null;
		
		StringBuilder sb = new StringBuilder();
		Map<Data, Integer> mp = new HashMap<Data, Integer>();
		
		
		for(Data d : dt){
			if(d instanceof FldData){
				FldData f = (FldData)d;
				ObjData p = f.getParent();
				if(mp.containsKey(p)==false){
					mp.put(p, 1);
				}
				else{
					Integer x = mp.get(p);
					mp.put(p, x+1);
				}				
			}
			else if(mp.containsKey(d)==false){
				mp.put(d, 1);
			}
		}
		
		for(Data d : mp.keySet()){
			if(d instanceof ObjData){
				sb.append(mp.get(d) + " offsets from "+ d +"\n");
			}
			else sb.append( d +"\n");
		}
		
		return sb.toString();
	}
	
	protected Set<Data> getUpData(Method offPoint){
		
		if(this.isStateful()){
			return this.upDataMap.get(offPoint);
		}else{
			Method p = offPoint.getCaller();
			if(p == null){
				if(this.toBeOffloaded.contains(offPoint)){//in cloud
					return program.getInput(offPoint);
					//return program.getInputSize(offPoint);
				}else{
					throw new RuntimeException(offPoint+" is not a offloading point");
				}				
			}else{
				if(this.toBeOffloaded.contains(offPoint) && !this.toBeOffloaded.contains(p)){//in cloud
					return program.getInput(offPoint);
					//return program.getInputSize(offPoint);
				}
				else
					if(this.toBeOffloaded.contains(p) && !this.toBeOffloaded.contains(offPoint)){//in mobile
						return program.getOutput(offPoint);
						//return program.getOutputSize(offPoint);
					}
					else
						throw new RuntimeException(offPoint+" is not a offloading point");
			}
		}
	}
	
	protected int getUpSize(Method offPoint){
		if(this.isStateful()){
			Integer ret = this.upSizeMap.get(offPoint);
			if(ret == null)
				return 0;
			return ret;
		}else{
			Method p = offPoint.getCaller();
			if(p == null){
				if(this.toBeOffloaded.contains(offPoint)){//in cloud
					return program.getInputSize(offPoint);
				}else{
					throw new RuntimeException(offPoint+" is not a offloading point");
				}				
			}else{
				if(this.toBeOffloaded.contains(offPoint) && !this.toBeOffloaded.contains(p)){//in cloud
					return program.getInputSize(offPoint);
				}
				else
					if(this.toBeOffloaded.contains(p) && !this.toBeOffloaded.contains(offPoint)){//in mobile
						return program.getOutputSize(offPoint);
					}
					else
						throw new RuntimeException(offPoint+" is not a offloading point");
			}
		}
	}
	
	protected Set<Data> getDownData(Method offPoint){
		
		if(this.isStateful()){
			return this.downDataMap.get(offPoint);
		}else{
			Method p = offPoint.getCaller();
			if(p == null){
				if(this.toBeOffloaded.contains(offPoint)){//in cloud
					return program.getOutput(offPoint);
					//return program.getOutputSize(offPoint);
				}else{
					throw new RuntimeException(offPoint+" is not a offloading point");
				}				
			}else{
				if(this.toBeOffloaded.contains(offPoint) && !this.toBeOffloaded.contains(p)){//in cloud
					return program.getOutput(offPoint);
					//return program.getOutputSize(offPoint);
				}
				else
					if(this.toBeOffloaded.contains(p) && !this.toBeOffloaded.contains(offPoint)){//in mobile
						return program.getInput(offPoint);
						//return program.getInputSize(offPoint);
					}
					else
						throw new RuntimeException(offPoint+" is not a offloading point");
			}
		}
		
	}
	
	protected int getDownSize(Method offPoint){
		if(this.isStateful()){
			Integer ret = this.downSizeMap.get(offPoint);
			if(ret == null)
				return 0;
			return ret;
		}else{
			Method p = offPoint.getCaller();
			if(p == null){
				if(this.toBeOffloaded.contains(offPoint)){//in cloud
					return program.getOutputSize(offPoint);
				}else{
					throw new RuntimeException(offPoint+" is not a offloading point");
				}				
			}else{
				if(this.toBeOffloaded.contains(offPoint) && !this.toBeOffloaded.contains(p)){//in cloud
					return program.getOutputSize(offPoint);
				}
				else
					if(this.toBeOffloaded.contains(p) && !this.toBeOffloaded.contains(offPoint)){//in mobile
						return program.getInputSize(offPoint);
					}
					else
						throw new RuntimeException(offPoint+" is not a offloading point");
			}
		}
	}
	
	public void printOffloadingPoints(Method root, int layer, boolean prevOffloaded) {
		if(toBeOffloaded.contains(root) && program.isMethodPinned(root)){
			this.methodsToMark.add(root.methName());
		}
		if(toBeOffloaded.contains(root) && !prevOffloaded){
			String layerIndent = "";
			for(int i = 0 ; i < layer; i ++)
				layerIndent +=" ";
			Utils.printResults(layerIndent+root.methName()+", original_start_time = "+root.getStartTime()+", original_exec_time = "+root.getInclusiveTime()+", thread = "+root.getThreadID());
			//Utils.printResults(layerIndent+"Upload size: "+this.getUpSize(root)+", Download size: "+this.getDownSize(root));
			Utils.printResults(layerIndent+"Upload size: " + this.getUpSize(root) +"\nUpload Data: " +  niceDisplay(this.getUpData(root)) );
			Utils.printResults(layerIndent+"Download size: " + this.getDownSize(root) +"\nDownload Data: " + niceDisplay(this.getDownData(root)) );
			
			prevOffloaded = true;
		}else if(!toBeOffloaded.contains(root) && prevOffloaded){
			String layerIndent = "";
			for(int i = 0 ; i < layer; i ++)
				layerIndent +=" ";
			Utils.printResults(layerIndent+"Offloaded back: " + root.methName() + ", original_start_time=" + root.getStartTime()+", original_exec_time = "+root.getInclusiveTime()+", thread = "+root.getThreadID());
			//Utils.printResults(layerIndent+"Download size: "+this.getDownSize(root)+", Upload size: "+this.getUpSize(root));
			Utils.printResults(layerIndent+"Upload size: " + this.getUpSize(root) +"\nUpload Data: " +  niceDisplay(this.getUpData(root)));
			Utils.printResults(layerIndent+"Download size: " + this.getDownSize(root) +"\nDownload Data: " + niceDisplay(this.getDownData(root)));

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
		
	public abstract double computeGuiOffloadedTime();

	public double getLocalTime() {
		return localTime;
	}

	public double getOffloadedTime() {
		return offloadedTime;
	}

	public double getGuiLocalTime() {
		return guiLocalTime;
	}

	public double getGuiOffloadedTime() {
		return guiOffloadedTime;
	}
	
	private void computetStatistics(){
		this.cloudTime = 0;
		this.mobileTime = 0;
		this.tranferTime = 0;
		this.maxDepth = 0;
		this.offSizes = new ArrayList<Integer>();
		for(Method t : program.getThreads()){
			this.computeStatisticsRecursively(t, 0);
		}
		
		System.out.println("OffloadedTime: " + offloadedTime);
		System.out.println("cloudTime: " + cloudTime);
		System.out.println("mobileTime: " + mobileTime);
		
		this.tranferTime = this.offloadedTime - this.cloudTime - this.mobileTime;
	}
	
	private boolean isSwitchToCloud(Method m){
		Method p = m.getCaller();
		if(p == null && this.toBeOffloaded.contains(m))
			return true;
		if((!this.toBeOffloaded.contains(p)) && this.toBeOffloaded.contains(m))
			return true;
		return false;
	}
	
	private boolean isSwitchToMobile(Method m){
		Method p = m.getCaller();
		if(p == null)
			return false;
		if(this.toBeOffloaded.contains(p) && (!this.toBeOffloaded.contains(m)))
			return true;
		return false;	
	}
	
	private int increaseDepth(int curDepth){
		curDepth = curDepth+1;
		if(curDepth > this.maxDepth){
			this.maxDepth = curDepth;
		}
		return curDepth;
	}
	
	private void computeStatisticsRecursively(Method m, int curDepth){
		if(this.toBeOffloaded.contains(m))
			this.cloudTime += Configuration.getRemoteExclusiveExecutionTime(m);
		else
			this.mobileTime += Configuration.getLocalExclusiveExecutionTime(m);
		if(this.isSwitchToCloud(m)){
			this.offSizes.add(this.getUpSize(m));
			curDepth = this.increaseDepth(curDepth);
		}
		if(this.isSwitchToMobile(m)){
			this.offSizes.add(this.getDownSize(m));
			curDepth = this.increaseDepth(curDepth);
		}
		for(Method c : m.getCallees())
			this.computeStatisticsRecursively(c,curDepth);
		if(this.isSwitchToCloud(m)){
			this.offSizes.add(this.getDownSize(m));
		}
		if(this.isSwitchToMobile(m)){
			this.offSizes.add(this.getUpSize(m));
		}
	}

	public double getMobileTime() {
		return mobileTime;
	}

	public double getCloudTime() {
		return cloudTime;
	}

	public double getTranferTime() {
		return tranferTime;
	}

	public List<Integer> getOffSizes() {
		return offSizes;
	}
	
	public int getMaxDepth(){
		return this.maxDepth;
	}
	
}
