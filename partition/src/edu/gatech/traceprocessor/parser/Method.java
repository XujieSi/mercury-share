package edu.gatech.traceprocessor.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gatech.traceprocessor.utils.Pair;

public class Method implements Comparable<Method> {
	public static final int STATELESS = 0;
	public static final int STATEFULL = 1;
	public static final int PINNED = 2;
	public static final int UNPIN_TREE = 3;
	public static final int SPEC_PINNED = 21;//an ugly hack for the new colocation mechanism. The new colocation
	//system allow to colocate all native methods in certain java methods except for the pinned methods defined in the 
	//configuration file. This is to create an exception.
	public static final int UNDEF = -1;
	public static int fakeAddr = 0;
	
	Program program;
	List<Instruction> instructions;
	MethodEntry entry;
	MethodExit exit;
	List<Method> callees;
	Method caller;
	List<Method> ancesters;
	Set<Method> reachableMethods;
	int pinType;
	long exclusiveTime;
	long inclusiveTime;
	
	Map<DataKey,Integer> jniArrayMap;
	
	List<Pair<Long,Integer>> params;
	List<Pair<Long,Integer>> ret;
	
	boolean isInstructionOrdered;
	
	public Method(Method caller,MethodEntry entry, Program program){
		this.entry = entry;
		this.instructions = new LinkedList<Instruction>();
		this.callees = new ArrayList<Method>();
		this.caller = caller;
		this.program = program;
		this.exclusiveTime = -1;
		this.inclusiveTime = -1;
		params = new ArrayList<Pair<Long,Integer>>();
		ret = new ArrayList<Pair<Long,Integer>>();
		jniArrayMap = new HashMap<DataKey,Integer>();
		this.isInstructionOrdered = true;
	}
	
	public void addCallee(Method callee){
		this.callees.add(callee);
	}
	
	public void destory(){
		instructions.clear();
		ancesters = null;
		reachableMethods = null;
	}
	
	public void addInstruction(Instruction inst){
		this.instructions.add(inst);
	}
	
	public long getAddr(){
		return entry.addr;
	}
	
	public int indexOf(Instruction i){
		return instructions.indexOf(i);
	}
	
	public void inline(Method m){
		int mi = this.callees.indexOf(m);
		if(mi != -1){
			this.callees.remove(mi);
			this.callees.addAll(mi, m.callees);
			for(Method c : m.callees)
				c.setCaller(this);
//			int ei = this.indexOf(m.entry);
//			int exi = this.indexOf(m.exit);
//			if(exi != ei + 1)
//				throw new RuntimeException("Entry and exit of the same method should be together!");
//			this.instructions.remove(m.exit);
//			this.instructions.remove(m.entry);
//			this.instructions.addAll(m.instructions);
			this.removeInstruction(m.exit);
			int loc = this.removeInstruction(m.entry);
			this.insertInstructions(loc, m.instructions);
			for(Instruction i : m.instructions){
				i.setMethod(this);
			}
			//Mark exclusive time as need to update
			this.exclusiveTime = -1;
			//mark cache as invalid
			this.resetReachableMethods();
			if(!m.isInstructionOrdered)
				this.isInstructionOrdered = false;
		}
	}
	
	/**
	 * Quick inlining: simply append the instructions of inlined methods to the tail of the instruction
	 * set. The entry and exit of the inlined method is not removed.
	 * @param m
	 */
	public void inlineQuick(Method m){
		int mi = this.callees.indexOf(m);
		if(mi != -1){
			this.isInstructionOrdered = false;
			this.callees.remove(mi);
			this.callees.addAll(mi, m.callees);
			for(Method c : m.callees)
				c.setCaller(this);
			this.instructions.addAll(m.instructions);
			for(Instruction i : m.instructions){
				i.setMethod(this);
			}
			//Mark exclusive time as need to update
			this.exclusiveTime = -1;
			//mark cache as invalid
			this.resetReachableMethods();
		}	
	}
	
	public void inlineBatchQuick(Set<Method> meths){
		if(meths.size() == 0){
			return;
		}
		List<Method> nCallees = new ArrayList<Method>();
		for(Method c : this.callees){
			if(meths.contains(c)){
				nCallees.addAll(c.callees);
				for(Method gc : c.callees)
					gc.setCaller(this);
				this.instructions.addAll(c.instructions);
				for(Instruction i : c.instructions){
					i.setMethod(this);
				}
				c.instructions = null;
			}else
				nCallees.add(c);
		}
		this.isInstructionOrdered = false;
		//Mark exclusive time as need to update
		this.exclusiveTime = -1;
		//mark cache as invalid
		this.resetReachableMethods();			
		this.callees = nCallees;
	}
	
	private int removeInstruction(Instruction i){
		int idx = instructions.indexOf(i);
		if(idx > 0)
			this.instructions.remove(i);
		return idx;
	}
	
	private void insertInstructions(int idx, Collection<Instruction> ins){
		this.instructions.addAll(idx, ins);
	}
	
	public void setCaller(Method m){
		this.caller = m;
		//mark cache as invalid
		this.resetAncestors();
	}
	
	public void setExit(MethodExit exit){
		if(this.exit != null)
			throw new RuntimeException(this+" already is closed.");
		if(exit.getThreadID() != entry.getThreadID() || exit.addr != entry.addr){
			throw new RuntimeException(exit + " doest not match " + entry);
		}
		this.exit = exit;
	}
	
	public List<Method> getAncestorAndSelf(){
		List<Method> ancesters = this.getAncestors();
		List<Method> ret = new ArrayList<Method>(ancesters);
		ret.add(0, this);
		return ret;
	}
	
	public List<Method> getAncestors(){
		if(ancesters != null)
			return ancesters;
		ancesters = new ArrayList<Method>();
		Method parent = this.getCaller();
		while(parent != null){
			ancesters.add(parent);
			parent = parent.getCaller();
		}
		return ancesters;
	}
	
	public void resetAncestors(){
		this.resetAncestorsRecursively(this);
	}
	
	private void resetAncestorsRecursively(Method m){
		m.ancesters = null;
		for(Method c : m.getCallees())
			this.resetAncestorsRecursively(c);
	}
	
	public Set<Method> getReachableMethods(){
		if(this.reachableMethods != null)
			return reachableMethods;
		reachableMethods = new HashSet<Method>();
		reachableMethods.add(this);
		for(Method c : this.callees)
			reachableMethods.addAll(c.getReachableMethods());
		return reachableMethods;
	}
	
	public void resetReachableMethods(){
		this.resetReachableMethodsRecursively(this);
	}
	
	private void resetReachableMethodsRecursively(Method m){
		while(m!=null){
			m.reachableMethods = null;
			m = m.getCaller();
		}
	}
	
	public int getThreadID(){
		return entry.getThreadID();
	}
	
	public int getMethodID(){
		return entry.getLineNum();
	}
	
	public Instruction getEntry(){
		return entry;
	}
	
	public Instruction getExit(){
		return exit;
	}
	
	public void setStartTime(long time){
		entry.time = time;
	}
	
	public void setEndTime(long time){
		exit.time = time;
	}
	
	public long getStartTime(){
		return entry.time;
	}
	
	public long getEndTime(){
		return exit.time;
	}
	
	public long getStartCount(){
		return entry.count;
	}
	
	public long getEndCount(){
		return exit.count;
	}
	
	public String methName(){
		return entry.name;
	}
	
	public boolean isNative(){
		return entry.isNative;
	}
	
	public int id(){
		return entry.getLineNum();
	}

	public int getMethodLine(){
		return entry.getLineNum();
	}
	
	public boolean isClosed(){
		return this.exit != null;
	}
	
	public long getExclusiveTime(){
		if(this.exclusiveTime < 0){
			long inclusiveTime = this.getInclusiveTime();
			long childrenTime = 0;
			for(Method c : this.callees)
				childrenTime += c.getInclusiveTime();
			this.exclusiveTime = inclusiveTime - childrenTime;
		}
		return this.exclusiveTime;
	}
	
	public List<Method> getCallees(){
		return Collections.unmodifiableList(callees);
	}
	
	public Method getCaller(){
		return caller;
	}
	
	public long getChildrenTime(){
		return this.getInclusiveTime() - this.getExclusiveTime();
	} 
	
	public long getInclusiveTime(){
		if(this.inclusiveTime < 0)
			this.inclusiveTime = this.getEndTime() - this.getStartTime();
		return this.inclusiveTime;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entry == null) ? 0 : entry.hashCode());
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
		Method other = (Method) obj;
		if (entry == null) {
			if (other.entry != null)
				return false;
		} else if (!entry.equals(other.entry))
			return false;
		return true;
	}
	
	public static long genFakeAddr(){
		fakeAddr--;
		return fakeAddr;
	}

	public List<Instruction> getInstructions() {
		return instructions;
	}

	@Override
	public String toString() {
		return "Method[name = "+entry.name+", threadID = "+this.getThreadID()+", startTime = "+entry.time
				+", startCount = "+entry.count+(this.exit==null?"]":", execTime = "+this.getInclusiveTime()+", method lineNum = "+this.entry.getLineNum()+"]");
	}

	@Override
	public int compareTo(Method o) {
		return this.entry.lineNum - o.entry.lineNum;
	}
	
	public void visitPreOrder(InstructionVisitor v){
		if(!this.isInstructionOrdered)
			throw new RuntimeException("InlineQuick is invoked. The instructions are no loger well formated!");
		for(Instruction i : instructions){
			v.visit(i);
			if(i instanceof MethodEntry){
				MethodEntry me = (MethodEntry)i;
				me.curMeth.visitPreOrder(v);
			}
		}
	}

	public void removeMethod(Method c) {
		int mi = this.callees.indexOf(c);
		if(mi != -1){
			this.callees.remove(mi);
			this.removeInstruction(c.exit);
			this.removeInstruction(c.entry);
			this.exclusiveTime = -1;
			//mark cache as invalid
			this.resetReachableMethods();
		}
	}
	
	public void addParam(long p){
		DataKey dk = program.getDataKey(p);
		if(dk == null)
			params.add(new Pair<Long,Integer>(p,0));
		else
			params.add(new Pair<Long,Integer>(p,dk.index));	
	}
	
	public void addRet(long r){
		DataKey dk = program.getDataKey(r);
		if(dk == null)
			ret.add(new Pair<Long,Integer>(r,0));
		else
			ret.add(new Pair<Long,Integer>(r,dk.index));	
	}
	
	public void increaseHoldCount(DataKey dk){
		Integer count = jniArrayMap.get(dk);
		if(count == null)
			count = 0;
		jniArrayMap.put(dk, count+1);
	}
	
	public void decreaseHoldCount(DataKey dk){
		Integer count = jniArrayMap.get(dk);
		if(count <=0 )
			throw new RuntimeException("Cannot decrease the count further.");
		jniArrayMap.put(dk, count-1);
	}
	
	public void checkHeldArrays(){
		for(Map.Entry<DataKey, Integer> entry : this.jniArrayMap.entrySet())
			if(entry.getValue() != 0)
				throw new RuntimeException("This array hasn't been released yet: "+entry);
	}

	public List<Pair<Long, Integer>> getParams() {
		return params;
	}

	public List<Pair<Long, Integer>> getRet() {
		return ret;
	}
}
