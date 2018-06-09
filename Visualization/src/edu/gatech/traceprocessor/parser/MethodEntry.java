package edu.gatech.traceprocessor.parser;

public class MethodEntry extends Instruction {
	public String name;
	public long addr;
	public long count;
	public long time;
	public boolean isNative;
	//The method we're calling
	public Method curMeth;
	
	
	
	public MethodEntry(int lineNum, int threadID, Method method, String name, long addr, long count, long startTime, boolean isNative) {
		super(lineNum, threadID, method);
		this.name = name;
		this.addr = addr;
		this.count = count;
		this.time = startTime;
		this.isNative = isNative;
	}

	@Override
	public String toPlainFormat() {
		return this.getThreadID()+"<fun n="+name+" a="+addr+" nt="+(isNative?1:0)+" t="+time+" c="+count+">";
	}

}
