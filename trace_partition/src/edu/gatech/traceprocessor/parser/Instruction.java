package edu.gatech.traceprocessor.parser;

import java.util.ArrayList;
import java.util.List;

public abstract class Instruction implements Comparable<Instruction> {
	public int lineNum;
	public int threadID;
	private Method method;

	public Instruction(int lineNum, int threadID, Method method) {
		super();
		this.lineNum = lineNum;
		this.threadID = threadID;
		this.setMethod(method);
	}

	public int getLineNum(){
		return lineNum;
	}
	
	public int getThreadID() {
		return threadID;
	}
	
	public Program getProgram(){
		return getMethod().program;
	}

	public abstract String toPlainFormat();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lineNum;
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
		Instruction other = (Instruction) obj;
		if (lineNum != other.lineNum)
			return false;
		return true;
	}

	public Method getMethod() {
		return method;
	}

	public List<Method> getEncloseMethods(){
		List<Method> encloseMethods = new ArrayList<Method>();
		encloseMethods.add(method);
		encloseMethods.addAll(method.getAncestors());
		return encloseMethods;
	}
	
	public void setMethod(Method method) {
		this.method = method;
	}
	
	@Override
	public int compareTo(Instruction o) {
		return this.lineNum-o.lineNum;
	}

	public String toString(){
		return this.toPlainFormat();
	}
	
}