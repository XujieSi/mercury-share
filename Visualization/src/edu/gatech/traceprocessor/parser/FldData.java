package edu.gatech.traceprocessor.parser;

public class FldData extends Data {
	public final static int ALL_FIELDS = -37;
	private int offset;
	private ObjData parent;
	
	public FldData(long addr, int size, int offset, ObjData parent) {
		super(addr, size, parent.index);
		this.offset = offset;
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "FldData [offset=" + offset + ", parent=" + parent + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + offset;
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FldData other = (FldData) obj;
		if (offset != other.offset)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

}
