package edu.gatech.traceprocessor.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * There are following kinds of data:
 * 1. Register or static fields
 * 2. Object field access. Need to provide two kinds of granularity: object and field
 * 3. Array access. Need to handle the whole array access, now sharing the same class with Object
 * @author xin
 */
public class Data {
	public final static int REG = 0;
	public final static int FIELD = 1;
	public final static int STATIC = 2;
	public final static int ARRAY = 3;
	
	long addr;
	int size;
	//For object type, the address might be reused
	int index;

	List<Write> writers;
	List<Read> readers;
	List<Instruction> accessors;
	boolean isThreadShared;
	int lastThreadId = -1;
	
	public Data(long addr, int size, int index) {
		super();
		this.addr = addr;
		this.size = size;
		this.writers = new ArrayList<Write>();
		this.readers = new ArrayList<Read>();
		this.isThreadShared = false;
		accessors = new ArrayList<Instruction>();
		this.index = index;
	}
	
	public void addReader(Read r){
		readers.add(r);
		this.addAccessor(r);
	}
	
	public void addWriter(Write w){
		writers.add(w);
		this.addAccessor(w);
	}
	
	public Write getLastWrite(){
		if(this.writers.size() > 0)
			return writers.get(writers.size()-1);
		return null;
	}
	
	private void addAccessor(Instruction i){
		if(!this.isThreadShared)
			if(this.accessors.size() == 0)
				lastThreadId = i.getThreadID();
			else{
				if(i.getThreadID() != this.lastThreadId)
					this.isThreadShared = true;
			}
		this.accessors.add(i);
	}
	
	public boolean isThreadShared(){
		return this.isThreadShared;
	}
	
	public List<Write> getWriters(){
		return Collections.unmodifiableList(writers);
	}
	
	public List<Read> getReaders(){
		return Collections.unmodifiableList(readers);
	}
	
	public List<Instruction> getAccessors(){
		return Collections.unmodifiableList(accessors);
	}
	
	public List<List<Instruction>> groupAccessors(){
		List<List<Instruction>> ret = new ArrayList<List<Instruction>>();
		Write lw = null;
		for(Instruction i : accessors){
			if(i instanceof Write){
				lw  = (Write)i;
				List<Instruction> curSeg = new ArrayList<Instruction>();
				curSeg.add(lw);
				ret.add(curSeg);
			}else if (i instanceof Read){
				if(lw == null)
					continue;
				List<Instruction> curSet = ret.get(ret.size() - 1);
				curSet.add(i);
			}else
				throw new RuntimeException("Unknown instruction " + i);
		}
		return ret;
	}

	public long getAddr() {
		return addr;
	}

	public int getSize() {
		return size;
	}
	
	public int getIndex(){
		return index;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (addr ^ (addr >>> 32));
		result = prime * result + index;
		result = prime * result + size;
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
		Data other = (Data) obj;
		if (addr != other.addr)
			return false;
		if (index != other.index)
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Data [addr=" + addr + ", size=" + size + ", index=" + index + "]";
	}
	

	
}