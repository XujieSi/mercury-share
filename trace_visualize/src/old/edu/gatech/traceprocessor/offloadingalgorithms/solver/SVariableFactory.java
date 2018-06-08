package old.edu.gatech.traceprocessor.offloadingalgorithms.solver;

import old.edu.gatech.traceprocessor.VarEntry;
import old.edu.gatech.traceprocessor.Variable;
import old.edu.gatech.traceprocessor.VariableFactory;

public class SVariableFactory extends VariableFactory {

	@Override
	public Variable create(Variable v) {
		return new SVariable((SVariable) v);
	}

	@Override
	public Variable create(VarEntry v) {
		return new SVariable(v);
	}

}
