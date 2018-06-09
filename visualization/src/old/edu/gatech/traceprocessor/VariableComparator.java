package old.edu.gatech.traceprocessor;

import java.util.Comparator;

public class VariableComparator implements Comparator<Variable>{
	@Override
    public int compare(Variable x, Variable y){
	/*	if(x.value.compareTo(y.value) != 0)
			return x.value.compareTo(y.value);
		if(x.writer.mId != y.writer.mId)
			return x.writer.mId - y.writer.mId;
		return x.reader.mId - y.reader.mId;
	*/
		if(!(x.entry.equals(y.entry)))
			return x.getEntry().getValue().compareTo(y.getEntry().getValue());
		
		return 0;
	}
    
}