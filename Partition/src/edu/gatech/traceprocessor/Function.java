package edu.gatech.traceprocessor;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import edu.gatech.traceprocessor.utils.Utils;

public abstract class Function {
	private static int MID;
	//properties
	protected String mName;
	protected boolean mIsNative;
	protected long mStartTime = 0;
	protected long mExeTime = 0;
	protected long mChildrenTime = 0;
	
	protected long mStartCount = 0;
	protected long mEndCount = 0;
//	protected int localType;
	protected boolean offloadable;
	protected int mId;
	protected int threadID;
	
	protected long entryLineNum;
	protected long exitLineNum;
	
	/**
	 * Input and output are for the data directed accessed in this function. Note if the data is only read and written
	 * in this function, it won't be put into input or ouput set
	 */
	protected Set<VarEntry> mInput; 
	protected Set<VarEntry> mOutput;
	/**
	 * The data used for ILP solving
	 */
	protected Set<VarEntry> mSubtreeInput; 
	protected Set<VarEntry> mSubtreeOutput;
	protected Set<Function> ancestors;
	
	//call structure
	protected Function mParent;
	protected Vector<Function> mChildren;	
	
	public Function(Function parent, String name, boolean isNative, long startTime, int threadId){
		mName = name;
		mId = MID++;
		mIsNative = isNative;
		mStartTime = startTime;
		mChildrenTime = 0;

		mInput = new HashSet<VarEntry>();
		mOutput = new HashSet<VarEntry>();
		mSubtreeInput = new HashSet<VarEntry>();
		mSubtreeOutput = new HashSet<VarEntry>();
		offloadable = true;
		this.threadID = threadId;
		
		mParent = parent;
		mChildren = new Vector<Function>();
	//	ancestors = new SortedList<Function>(fcomparator);
		ancestors = new HashSet<Function>();
		ancestors.add(this);
		while(parent != null){
			ancestors.add(parent);
			parent = parent.mParent;
		}
	}
	
	public int getID(){
		return this.mId;
	}
	
	@Override
	public boolean equals(Object that){
		if(that == this)
			return true;
		if(that == null)
			return false;
		
		if(!(that instanceof Function))
			return false;
		
		if(((Function) that).mId == this.mId)
			return true;

		return false;
	}
	
	@Override
	public int hashCode(){
		return mId;
	}
		
	public boolean isOffloadable(){
		return offloadable;
	}
	
	public Function getParent(){
		return mParent;
	}
	
	public Vector<Function> getChildren(){
		return mChildren;
	}
	
	public Set<VarEntry> getInput(){
		return mInput;
	}
	
	public Set<VarEntry> getOutput(){
		return mOutput;
	}
	
	public Set<VarEntry> getSubtreeInput(){
		return Collections.unmodifiableSet(mSubtreeInput);
	}
	public Set<VarEntry> getSubtreeOutput(){
		return Collections.unmodifiableSet(mSubtreeOutput);
	}
	
	public Set<Function> getAncestors(){
		return ancestors;
	}
	
	public void setParent(Function f){
		if(this.equals(f))
			throw new RuntimeException("Parent link to itself!");
		this.mParent = f;
	}
	
	public String getName(){
		return mName;
	}
	
	public boolean isNative() {
		return mIsNative;
	}

	public int getThreadID() {
		return threadID;
	}

	public void setThreadID(int threadID) {
		this.threadID = threadID;
	}
	
	public int getInputSize(){
		int size = 0;
		for(VarEntry v : mInput){
			size += v.getSize();
		}
		return size;
	}

	public int getOutputSize(){
		int size = 0;
		for(VarEntry v : mOutput){
			size += v.getSize();
		}
		return size;
	}
	
	public int getSubtreeInputSize(){
		int size = 0;
		for(VarEntry v : mSubtreeInput){
			size += v.getSize();
		}
		return size;
	}

	public int getSubtreeOutputSize(){
		int size = 0;
		for(VarEntry v : mSubtreeOutput){
			size += v.getSize();
		}
		return size;
	}
	
	public void setNonOffloadable(/*int type*/){
		offloadable = false;
	//	localType = type;
	}
	
	public void addChild(Function f){
		mChildren.add(f);
	}
	
	
	public boolean addInput(VarEntry var){
//		if(var.entry.getWriter() == this)
//			return false;
		boolean ret = false;
		if(mInput.add(var)){
			ret = true;
			addSubtreeInput(var);
		}
		return ret;
	}
	
	public boolean addOutput(VarEntry var){
//		if(var.getEntry().getReaders().size()==0)
//			return false;
//		if(var.getEntry().getReaders().size()==1){
//			if(var.getEntry().getReaders().iterator().next() == this)
//				return false;
//		}
		boolean ret = false;
		if(mOutput.add(var)){
			ret = true;
		}
		addSubtreeOutput(var);
		return ret;
	}
	
	public void setStartTime(long startTime){
		mStartTime = startTime;
	}
	
	public void setEndTime(long endTime){
		if(endTime < mStartTime){
			mExeTime = 0;
		}
		else
			mExeTime = endTime - mStartTime;
	}
	
	public long getStartTime(){
		return mStartTime;
	}
	
	public long getEndTime(){
		return mStartTime + mExeTime;
	}
	
//	public long getRemoteExecutionTime(){
//		return mExeTime;
//	}
	
	public long getExecutionTime(){
		return mExeTime;
	}
	
	
	public double updateRemainderTime(){
		mChildrenTime = 0;
		for(int i = 0; i < mChildren.size(); i++){
			mChildrenTime += mChildren.get(i).updateRemainderTime();
		}
		if(mExeTime < mChildrenTime){
			this.mExeTime = mChildrenTime;
			Utils.printLog("Total execution time of " + mName+"(ST:" + mStartTime +") less than sum of children execution times!" );
		}
//		return this.getRemoteExecutionTime();
		return this.getExecutionTime();
	}
	
//	public long getRemoteRemainderExecutionTime(){
//		return mExeTime - mChildrenTime;
//	}
		
	public long getRemainderExecutionTime(){
		return mExeTime - mChildrenTime;
	}
	
	public abstract boolean addSubtreeInput(VarEntry v);
	public abstract boolean addSubtreeOutput(VarEntry v);
	public abstract boolean removeSubtreeInput(VarEntry v);
	public abstract boolean removeSubtreeOutput(VarEntry v);
	
	public abstract boolean inlineChild(Function f);

	
	public boolean removeSubtreeInputAll(Collection<VarEntry> c){
		boolean ret = false;
		for(VarEntry v : c)
			ret |= this.removeSubtreeInput(v);
		return ret;
	}

	public boolean removeSubtreeOutputAll(Collection<VarEntry> c){
		boolean ret = false;
		for(VarEntry v : c)
			ret |= this.removeSubtreeOutput(v);
		return ret;
	}
	
	public long getStartCount() {
		return mStartCount;
	}

	public void setStartCount(long mStartCount) {
		this.mStartCount = mStartCount;
	}

	public long getEndCount() {
		return mEndCount;
	}

	public void setEndCount(long mEndCount) {
		this.mEndCount = mEndCount;
	}

	@Override
	public String toString() {
		return "Function [mName=" + mName + ", mIsNative=" + mIsNative + ", mStartTime=" + mStartTime + ", mExeTime=" + mExeTime + ", mChildrenTime=" + mChildrenTime + ", mStartCount=" + mStartCount + ", mEndCount=" + mEndCount + ", offloadable=" + offloadable + ", mId=" + mId + ", threadID=" + threadID + "]";
	}

	public void printStatistics(PrintStream out){
		Map<String,Integer> funcCountMap = new HashMap<String,Integer>();
		Map<String,Long> funcTimeMap = new HashMap<String,Long>();
		Map<String,Long> maxTimeMap = new HashMap<String,Long>();
		Map<String,Integer> maxInputMap = new HashMap<String,Integer>();
		Map<String,Integer> maxOutputMap = new HashMap<String,Integer>();
		discoverStat(this,funcCountMap,funcTimeMap,maxTimeMap,maxInputMap,maxOutputMap);
		SortedMap<Integer,Set<String>> countMap = new TreeMap<Integer,Set<String>>(); 
		for(Map.Entry<String, Integer> entry : funcCountMap.entrySet()){
			Set<String> curSet = countMap.get(entry.getValue());
			if(curSet == null){
				curSet = new HashSet<String>();
				countMap.put(entry.getValue(), curSet);
			}
			curSet.add(entry.getKey());
		}
		out.println("name\t\t\t\t\t\t\t\t\t\tcount\ttime\tmaxtime\tmax input\tmax output");
		for(Map.Entry<Integer, Set<String>> entry : countMap.entrySet()){
			for(String name : entry.getValue()){
				out.println(name+"\t\t\t\t\t\t\t\t\t\t"+entry.getKey()+"\t"+funcTimeMap.get(name)+"\t"+maxTimeMap.get(name)
						+"\t"+maxInputMap.get(name)+"\t"+maxOutputMap.get(name));
			}
		}
	}
	
	private void discoverStat(Function f, Map<String,Integer> funcCountMap, Map<String,Long> funcTimeMap, Map<String,Long> maxTimeMap,
			Map<String,Integer> maxInputMap, Map<String,Integer> maxOutputMap){
		String fname = f.getName();
		Integer count = funcCountMap.get(fname);
		if(count == null){
			count = 0;
		}
		funcCountMap.put(fname, count+1);
		Long time = funcTimeMap.get(fname);
		if(time == null)
			time = 0L;
		funcTimeMap.put(fname, time+f.getExecutionTime());
		Long maxTime = maxTimeMap.get(fname);
		if(maxTime == null)
			maxTime = 0L;
		if(f.getExecutionTime() >= maxTime)
			maxTimeMap.put(fname, f.getExecutionTime());
		Integer maxInput = maxInputMap.get(fname);
		if(maxInput == null)
			maxInput = 0;
		if(f.getInputSize() >= maxInput)
			maxInputMap.put(fname, f.getInputSize());
		Integer maxOutput = maxOutputMap.get(fname);
		if(maxOutput == null)
			maxOutput = 0;
		if(f.getOutputSize() >= maxOutput)
			maxOutputMap.put(fname, f.getOutputSize());
		for(Function c: f.getChildren())
			discoverStat(c,funcCountMap,funcTimeMap,maxTimeMap,maxInputMap,maxOutputMap);
	}
	
	public Function getThread(){
		Function thread = this;
		while(!thread.mName.equals(TraceProcessor.ROOT)&&thread.mParent!=null)
			thread = thread.mParent;
		if(thread.mName == TraceProcessor.FORESTROOT)
			return null;
		return thread;
	}
	
	public long getEntryLineNum() {
		return entryLineNum;
	}

	public void setEntryLineNum(long entryLineNum) {
		this.entryLineNum = entryLineNum;
	}

	public long getExitLineNum() {
		return exitLineNum;
	}

	public void setExitLineNum(long exitLineNum) {
		this.exitLineNum = exitLineNum;
	}
}

