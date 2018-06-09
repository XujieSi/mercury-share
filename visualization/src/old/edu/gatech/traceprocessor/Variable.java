package old.edu.gatech.traceprocessor;

public abstract class Variable {
	VarEntry entry;
	String offset;
	
	public Variable(Variable v){
	//	value = v.value;
	//	size = v.size;
		entry = v.entry;
	}
	
	public Variable(VarEntry copy){
	//	value = copy.getValue();
	//	size = copy.getSize();
		entry = copy;
	}
	

/*	public String getValue() {
		return value;
	}

	public int getSize() {
		return size;
	}
*/
	public VarEntry getEntry() {
		return entry;
	}

	public void setOffset(String off){
		offset = off;
	}
	
	@Override
	public boolean equals(Object that){
		if(that == this)
			return true;
		if(that == null)
			return false;
		
		if(!(that instanceof Variable))
			return false;
		
		Variable z = (Variable) that;
		return (this.getEntry().equals(z.getEntry()));
	}
	
	@Override
	public int hashCode(){
		return entry.getVid();
	}

	public String toString(){
		return ""+entry.getVid();
	}
	
}
