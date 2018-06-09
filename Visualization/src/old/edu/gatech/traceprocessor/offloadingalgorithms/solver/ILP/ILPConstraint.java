package old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP;

import java.util.List;

import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.BooleanVariable;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.Constraint;
import edu.gatech.traceprocessor.utils.Pair;

public class ILPConstraint extends Constraint {
	List<Pair<Double,BooleanVariable>> variables; //Pair of coefficient and BooleanVariable
	double lb;
	double ub;
	
	public ILPConstraint(List<Pair<Double,BooleanVariable>> variables, double lb, double ub){
		this.variables = variables;
		this.lb = lb;
		this.ub = ub;
		for(Pair<Double,BooleanVariable> p :variables)
			if(p.getSecond() == null)
				throw new RuntimeException("Null boolean variables");
	}
}
