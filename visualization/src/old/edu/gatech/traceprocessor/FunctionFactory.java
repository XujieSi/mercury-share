package old.edu.gatech.traceprocessor;

public abstract class FunctionFactory {
	
	public abstract Function create(Function parent, String name, boolean isNative, long startTime, int threadId);

}
