package edu.gatech.traceprocessor.parser;

public class DataKey{
	long address;
	int index;
	
	public DataKey(long address, int index){
		this.address = address;
		this.index = index;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (address ^ (address >>> 32));
		result = prime * result + (int) (index ^ (index >>> 32));
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
		DataKey other = (DataKey) obj;
		if (address != other.address)
			return false;
		if (index != other.index)
			return false;
		return true;
	}
	
}