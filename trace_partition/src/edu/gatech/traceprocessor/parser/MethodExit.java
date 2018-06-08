package edu.gatech.traceprocessor.parser;

public class MethodExit extends Instruction {
	public long addr;
	public long time;
	public long count;
	//The method we're returning from
	public Method curMeth;
	
	
	public MethodExit(int lineNum, int threadID, Method method,long addr, long time, long count) {
		super(lineNum, threadID, method);
		this.addr = addr;
		this.time = time;
		this.count = count;
	}


	@Override
	public String toPlainFormat() {
		return this.getThreadID()+"</fun a="+addr+" t="+time+" c="+count+">";
	}

}
