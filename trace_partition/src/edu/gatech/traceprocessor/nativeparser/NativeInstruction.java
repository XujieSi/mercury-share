package edu.gatech.traceprocessor.nativeparser;


/**
 * A light weight class representing instructions in native trace
 * @author xin
 *
 */
public class NativeInstruction implements Comparable<NativeInstruction>{
	final static int ENTRANCE = 0;
	final static int EXIT = 1;
	final static int WRITE = 2;
	final static int READ = 3;
	final static int SYS_CALL = 4;
	int lineNum;
	int type;
	int threadId;
	long data1;
	long data2;
	long data3;
	NativeMethod meth;

	public NativeInstruction(int lineNum, int type, int threadId, long data1,
			long data2, long data3, NativeMethod meth) {
		super();
		this.lineNum = lineNum;
		this.type = type;
		this.threadId = threadId;
		this.data1 = data1;
		this.data2 = data2;
		this.data3 = data3;
		this.meth = meth;
	}

	@Override
	public int compareTo(NativeInstruction o) {
		long ret = this.lineNum - o.lineNum ;
		if(ret > 0)
			return 1;
		if(ret == 0)
			return 0;
			return -1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (lineNum ^ (lineNum >>> 32));
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
		NativeInstruction other = (NativeInstruction) obj;
		if (lineNum != other.lineNum)
			return false;
		return true;
	}

	public int getLineNum() {
		return this.lineNum;
	} 	
	
}