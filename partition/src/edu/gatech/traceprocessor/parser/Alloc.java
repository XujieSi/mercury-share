package edu.gatech.traceprocessor.parser;

public class Alloc extends Instruction {
	long addr;
	int objSize;
	Data d;

	public Alloc(int lineNum, int threadID, Method method, long addr, int objSize, String _typeName) {
		super(lineNum, threadID, method);
		this.addr = addr;
		this.objSize = objSize;
		Program p = this.getProgram();
		int index = p.newAddr(addr);
		this.d = new ObjData(addr, objSize, index, _typeName);
		p.addrToData.put(p.getDataKey(addr), d);
	}
	
	public Data getData(){
		return d;
	}
	
	@Override
	public String toPlainFormat() {
		return this.getThreadID()+"<a v="+addr+" s="+objSize+"/>";
	}

}
