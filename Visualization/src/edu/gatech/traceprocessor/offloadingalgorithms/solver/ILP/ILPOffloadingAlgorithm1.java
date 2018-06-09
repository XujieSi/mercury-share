package edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gatech.traceprocessor.offloadingalgorithms.solver.BooleanVariable;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.SMethod;
import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Pair;

/*
 * Stateless uni-directional
 */
public class ILPOffloadingAlgorithm1 extends ILPOffloadingAlgorithm {

	public ILPOffloadingAlgorithm1(Program p) {
		super(p);
	}
	
	@Override
	public double optimize(Program p) {
		double time = generateConstraintsAndSolve();
		return time;
	}

	public double generateConstraintsAndSolve(){
				
		for(SMethod m : methVarMap.values()){
			Method meth = m.getMethod();
			double localRuntimeWeight = Configuration.getLocalExclusiveExecutionTime(meth);
			double remoteRuntimeWeight = Configuration.getRemoteExclusiveExecutionTime(meth);
			double dataWeight = Configuration.getTransportTime(program, meth);
			
			BooleanVariable li = addSolverVar(m, "L", localRuntimeWeight, L);
			
			BooleanVariable ri = addSolverVar(m, "R", remoteRuntimeWeight, R);
			
			BooleanVariable di = addSolverVar(m, "D", dataWeight, D);
			
			//Constraint to pin this function to local device
			if(isMethodPinned(meth)){
				addFixedValueConstraint(li, 1, false);
			}

			//Constraint that li xor ri
			{
				List<BooleanVariable> bList = new ArrayList<BooleanVariable>();
				bList.add(li);
				bList.add(ri);
				addXORConstraint(bList, false);
			}
			
			
			//Constraints involving parent node in the call graph
			Method parentMeth = meth.getCaller();
			if(parentMeth != null){
				SMethod parentSMeth = methVarMap.get(parentMeth);

				BooleanVariable pLi = addSolverVar(parentSMeth, "L", L);

				BooleanVariable pRi = addSolverVar(parentSMeth, "R", R);

				//Constraint that rj => ri where j is the parent node of i
				addImpliesConstraint(pRi, ri, false);

				//Constraint that di = lj * ri where j is the parent node of i
				addANDConstraint(di, pLi, ri, false);
			}else{
				addImpliesConstraint(ri,di, false);
			}
		}
		
		//Constraints for co-locating methods
		{
			for(Set<Method> SCC : program.getColocSet()){
				BooleanVariable si = addSolverVar("S");
				ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
				for(Method sccMeth : SCC){
					SMethod sccSMeth = methVarMap.get(sccMeth);
					consL.add(new Pair<Double, BooleanVariable>(1.0, sccSMeth.getSolverVar(L)));
				}
				consL.add(new Pair<Double, BooleanVariable>(-1.0 * SCC.size(),si));
				BooleanConstraint cons = new BooleanConstraint(consL, 0, 0);
				constraints.add(cons);
				if(SCC.size()>1){
					Method commonAnces = program.findCommonAncestor(SCC);
					if(commonAnces == null){
						for(Method sccMeth : SCC){
							SMethod sccSMeth = methVarMap.get(sccMeth);
							this.addFixedValueConstraint(sccSMeth.getSolverVar(L), 1, false);
						}
					}else{
						for(Method sccMeth : SCC){
							SMethod sccSMeth = methVarMap.get(sccMeth);
							Method parent = sccMeth.getCaller();
							while(parent != null && parent!=commonAnces.getCaller()){
								SMethod sParent = methVarMap.get(parent);
								this.addImpliesConstraint(sccSMeth.getSolverVar(R), sParent.getSolverVar(R), false);
								parent = parent.getCaller();
							}
						}
					}
				}
			}
		}
		
		//Constraints for limiting the number of static offloaded methods
		{
			if(Configuration.offloadingLimit >= 0){
				ArrayList<Pair<Double,BooleanVariable>> consM = new ArrayList<Pair<Double,BooleanVariable>>();
				for(Map.Entry<String, Set<Method>> entry : methNameMap.entrySet()){
					Set<Method> methWithSameName = entry.getValue();
					
					BooleanVariable xi = addSolverVar("X");
					consM.add(new Pair<Double, BooleanVariable>(1.0, xi));
					
					ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
					for(Method tempM : methWithSameName){
						SMethod tempSM = methVarMap.get(tempM);
						consL.add(new Pair<Double, BooleanVariable>(1.0, tempSM.getSolverVar(R)));
					}
					consL.add(new Pair<Double, BooleanVariable>(-1.0 * methWithSameName.size(),xi));
					BooleanConstraint cons = new BooleanConstraint(consL, -1.0 * methWithSameName.size() + 1 , 0);
					constraints.add(cons);
				}
				BooleanConstraint cons = new BooleanConstraint(consM, 0, Configuration.offloadingLimit);
				constraints.add(cons);
			}
		}
		

		double objValue = runSolver(1);
		return objValue;
	}
	
	protected void getMethsToBeOffloaded(double[] ILPSolution){
		for(SMethod meth : methVarMap.values()){
			if(ILPSolution[meth.getSolverVar(R).id] == 1){
				toBeOffloaded.add(meth.getMethod());
			}
		}
	}

	@Override
	public boolean isStateful() {
		return false;
	}

}
