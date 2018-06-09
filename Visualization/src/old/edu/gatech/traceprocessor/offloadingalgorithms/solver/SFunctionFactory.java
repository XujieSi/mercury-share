package old.edu.gatech.traceprocessor.offloadingalgorithms.solver;

import old.edu.gatech.traceprocessor.Function;
import old.edu.gatech.traceprocessor.FunctionFactory;

public class SFunctionFactory extends FunctionFactory{

	@Override
	public Function create(Function parent, String name, boolean isNative,
			long startTime, int threadId) {
		return new SFunction(parent, name, isNative, startTime, threadId);
	}

}
