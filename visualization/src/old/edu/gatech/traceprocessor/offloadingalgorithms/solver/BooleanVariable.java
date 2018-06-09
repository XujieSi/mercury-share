package old.edu.gatech.traceprocessor.offloadingalgorithms.solver;

public class BooleanVariable {
	//id starts from 0 for ILP and 1 for SAT
	public int id;
	public String name;
	
	public BooleanVariable(int id, String name){
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String toString(){
		return String.valueOf(id);
	}
	
	@Override
	public boolean equals(Object y){
		if(y == this)
			return true;
		if(y == null)
			return false;
		
		if(!(y instanceof BooleanVariable))
			return false;
		
		BooleanVariable z = (BooleanVariable) y;
		
		if(z.id != this.id)
			return false;		
		
		if(z.name == null){
			if(this.name != null)
				return false;
		}else{
			if(this.name == null)
				return false;
			else{
				if(!this.name.equalsIgnoreCase(z.name))
					return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode(){
		return id;
	}
}
