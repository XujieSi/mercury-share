package old.edu.gatech.traceprocessor;

import java.util.Comparator;

public class FunctionComparator implements Comparator<Function>{
	@Override
    public int compare(Function x, Function y){
		
		return x.mId - y.mId;
	}
    
}