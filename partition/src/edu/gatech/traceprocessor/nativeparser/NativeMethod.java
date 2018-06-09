package edu.gatech.traceprocessor.nativeparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class NativeMethod {
	boolean isJni; // true: java->native. false: native->java
	TreeSet<NativeInstruction> body;
	NativeMethod parent;
	int tid;
	int javaIC;// the count of java instructions when entering this native method
	long sIC; // the count of native instructions at entrance
	long eIC; // the count of native instructions at exit
	int methId;
	Set<Integer> sysCalls;
	List<NativeMethod> callees;
	NativeInstruction entrance;
	NativeInstruction exit;
	
	public NativeMethod(boolean isJni, NativeMethod parent, int tid, int javaIC, long sIC, int mid){
		this.isJni = isJni;
		this.tid = tid;
		this.javaIC = javaIC;
		this.sIC = sIC;
		this.eIC = -1;
		this.body = new TreeSet<NativeInstruction>();
		this.sysCalls = new HashSet<Integer>();
		this.callees = new ArrayList<NativeMethod>();
		this.methId = mid;
	}
	
	public boolean isClosed(){
		return eIC > sIC;
	}
	
	public void close(long eIC){
		this.eIC = eIC;
	}
	
	public void addInstruction(NativeInstruction instr){
		this.body.add(instr);
	}
	
	public void addCalless(NativeMethod method){
		this.callees.add(method);
	}
	
	public void addSyscall(int num){
		this.sysCalls.add(num);
	}

	public boolean isRoot(){
		return parent == null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (eIC ^ (eIC >>> 32));
		result = prime * result + (isJni ? 1231 : 1237);
		result = prime * result + javaIC;
		result = prime * result + methId;
		result = prime * result + (int) (sIC ^ (sIC >>> 32));
		result = prime * result + tid;
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
		NativeMethod other = (NativeMethod) obj;
		if (eIC != other.eIC)
			return false;
		if (isJni != other.isJni)
			return false;
		if (javaIC != other.javaIC)
			return false;
		if (methId != other.methId)
			return false;
		if (sIC != other.sIC)
			return false;
		if (tid != other.tid)
			return false;
		return true;
	}
		
}
