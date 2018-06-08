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
import edu.gatech.traceprocessor.utils.Utils;

/*
 * Stateless parallel
 */
public class ILPOffloadingAlgorithm5 extends ILPOffloadingAlgorithm {
	
	final int L = 0;
	final int R = 1;
	final int D = 2;

	public ILPOffloadingAlgorithm5(Function forest, Set<VarEntry> programVarTable, Set<Set<Function>> coLocSCCs) {
		super(forest, programVarTable, coLocSCCs);
	}
	
	@Override
	public double optimize(Function root) {
//		generateDataStatsRecursive2(root);
		generateParallelizationInfo();
		Utils.printLog("CumulativeDataStatsGenerated");
		double time = generateConstraintsAndSolve();
		return time;
	}

	public double generateConstraintsAndSolve(){
	//	int numVars = temporalFuncList.size() * 3;
	//	double[] objCoeffs = new double[numVars];

		for(Function func : temporalFuncList){
		//	SFunction extFunc = funcMap.get(func);
			SFunction extFunc = (SFunction)func;
			double localRuntimeWeight = Configuration.getLocalRemainderExecutionTime(extFunc);
			double remoteRuntimeWeight = Configuration.getRemoteRemainderExecutionTime(extFunc);
			double dataWeight = Configuration.getTransportTime(extFunc);
			
			double remoteTime = dataWeight + remoteRuntimeWeight; 
			double delayWeight = 0;
			
			if(extFunc.getEarliestNonSubTreeReader() != null){
				long readDelay = extFunc.getEarliestNonSubTreeReader().getStartTime() - func.getEndTime();

				if(readDelay < remoteTime){
					delayWeight = remoteTime - readDelay;
				}
			}
			
			double parallelDataWeight = dataWeight - delayWeight;

			BooleanVariable li = addSolverVar(extFunc, "L", -localRuntimeWeight, L);
			
			BooleanVariable ri = addSolverVar(extFunc, "R", remoteRuntimeWeight, R);

			BooleanVariable di = addSolverVar(extFunc, "D", parallelDataWeight, D);

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
			
			//Constraint that ri => lj1 ^ lj2 ^.. ^ ljn where jk follows i before i can return
			ArrayList<Pair<Double,BooleanVariable>> consL1 = new ArrayList<Pair<Double,BooleanVariable>>();
			for(int i = extFunc.getLastSubTreeNodeID() + 1; i < extFunc.getEarliestNonOverlappingNodeID(); i++){
					SFunction extOverlappingFunc = funcIDMap.get(i);

					BooleanVariable tLi = addSolverVar(extOverlappingFunc, "L", L);
										
					consL1.add(new Pair<Double, BooleanVariable>(1.0, tLi));
					
					//Constraint that ri => lj where j follows i before i can return
					//addImpliesConstraint(ri, tLi);
			}
			int numFollowers = consL1.size();
			if(numFollowers != 0){
				consL1.add(new Pair<Double, BooleanVariable>((double) -numFollowers, ri));
				ILPConstraint cons7 = new ILPConstraint(consL1, 0, numFollowers);
				constraints.add(cons7);
			}
		}
		
		double objValue = runSolver(2);
		return objValue;
	}

	@Override
	protected void getFuncsToBeOffloaded(double[] ILPSolution) {
	//	for(Function func : funcMap.keySet()){
	//		SFunction extFunc = funcMap.get(func);
		for(Function func : funcIDMap.values()){
			SFunction extFunc = (SFunction)func;
			if(ILPSolution[extFunc.getSolverVar(R).id] == 1){
				toBeOffloaded.add(func);
			}
		}
	}
}
