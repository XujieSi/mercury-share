package old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import old.edu.gatech.traceprocessor.Configuration;
import old.edu.gatech.traceprocessor.Function;
import old.edu.gatech.traceprocessor.VarEntry;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.BooleanVariable;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.SFunction;
import edu.gatech.traceprocessor.utils.Pair;

/*
 * Stateless uni-directional
 */
public class ILPOffloadingAlgorithm1 extends ILPOffloadingAlgorithm {

	final int L = 0;
	final int R = 1;
	final int D = 2;
	
	
	public ILPOffloadingAlgorithm1(Function forest, Set<VarEntry> programVarTable, Set<Set<Function>> coLocSCCs) {
		super(forest, programVarTable, coLocSCCs);
	}
	
	@Override
	public double optimize(Function root) {
//		generateDataStatsRecursive2(root);
//		Utils.printLog("CumulativeDataStatsGenerated");
		double time = generateConstraintsAndSolve();
		return time;
	}

	public double generateConstraintsAndSolve(){
				
	//	for(Function func : funcMap.keySet()){
		for(Function func : funcIDMap.values()){
		//	SFunction extFunc = funcMap.get(func);
			SFunction extFunc = (SFunction)func;
			double localRuntimeWeight = Configuration.getLocalRemainderExecutionTime(extFunc);
			double remoteRuntimeWeight = Configuration.getRemoteRemainderExecutionTime(extFunc);
			double dataWeight = Configuration.getTransportTime(extFunc);
			
			BooleanVariable li = addSolverVar(extFunc, "L", localRuntimeWeight, L);
			
			BooleanVariable ri = addSolverVar(extFunc, "R", remoteRuntimeWeight, R);
			
			BooleanVariable di = addSolverVar(extFunc, "D", dataWeight, D);
			
			//Constraint to pin this function to local device
			if(!func.isOffloadable()){
				addFixedValueConstraint(li, 1);
			}

			//Constraint that li xor ri
			{
				List<BooleanVariable> bList = new ArrayList<BooleanVariable>();
				bList.add(li);
				bList.add(ri);
				addXORConstraint(bList);
			}
			
			
			//Constraints involving parent node in the call graph
			Function parentFunc = func.getParent();
			if(parentFunc.getParent() != null){
			//	SFunction extParentFunc = funcMap.get(parentFunc);
			//	assert extParentFunc!=null;
				SFunction extParentFunc = (SFunction)parentFunc;

				BooleanVariable pLi = addSolverVar(extParentFunc, "L", L);
				
				BooleanVariable pRi = addSolverVar(extParentFunc, "R", R);
				
				//Constraint that rj => ri where j is the parent node of i
				addImpliesConstraint(pRi, ri);
				
				//Constraint that di = lj * ri where j is the parent node of i
				addANDConstraint(di, pLi, ri);
			}
		}
		
		//Constraints for co-locating methods
		{
			for(Set<Function> SCC : coLocSCCs){
				BooleanVariable si = addSolverVar("S");
				ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
				for(Function f : SCC){
					consL.add(new Pair<Double, BooleanVariable>(1.0, ((SFunction)f).getSolverVar(L)));
				}
				consL.add(new Pair<Double, BooleanVariable>(-1.0 * SCC.size(),si));
				ILPConstraint cons = new ILPConstraint(consL, 0, 0);
				constraints.add(cons);
			}
		}
		
		//Constraints for limiting the number of static offloaded methods
		{
			if(Configuration.offloadingLimit >= 0){
				ArrayList<Pair<Double,BooleanVariable>> consM = new ArrayList<Pair<Double,BooleanVariable>>();
				for(String fName : funcNameMap.keySet()){
					Set<Function> funcWithSameNames = funcNameMap.get(fName);
					
					BooleanVariable xi = addSolverVar("X");
					consM.add(new Pair<Double, BooleanVariable>(1.0, xi));
					
					ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
					for(Function f : funcWithSameNames){
						consL.add(new Pair<Double, BooleanVariable>(1.0, ((SFunction)f).getSolverVar(R)));
					}
					consL.add(new Pair<Double, BooleanVariable>(-1.0 * funcWithSameNames.size(),xi));
					ILPConstraint cons = new ILPConstraint(consL, -1.0 * funcWithSameNames.size() + 1 , 0);
					constraints.add(cons);
				}
				ILPConstraint cons = new ILPConstraint(consM, 0, Configuration.offloadingLimit);
				constraints.add(cons);
			}
		}
		

		double objValue = runSolver(1);
		return objValue;
	}
	
	protected void getFuncsToBeOffloaded(double[] ILPSolution){
		//for(Function func : funcMap.keySet()){
		for(Function func : funcIDMap.values()){
		//	SFunction extFunc = funcMap.get(func);
			SFunction extFunc = (SFunction)func;
			if(ILPSolution[extFunc.getSolverVar(R).id] == 1){
				toBeOffloaded.add(func);
			}
		}
	}

}
