package edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gatech.traceprocessor.offloadingalgorithms.solver.BooleanVariable;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.SMethod;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.SVariable;
import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.Instruction;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Pair;

/*
 * Stateful bi-directional with piecewise linear network model
 */
public class ILPOffloadingAlgorithm4 extends ILPOffloadingAlgorithm {
	private Map<Data,Set<SVariable>> varMap;

	final int P = 3;
	final int W = 4;
	final int Q = 5;
	
	final int V = 0;
	final int T1 = 1;
	final int T2 = 2;
	
	public ILPOffloadingAlgorithm4(Program p) {
		super(p);
	}

	@Override
	public double optimize(Program p) {
		generateProgramVarMap();
		double time = generateConstraintsAndSolve();
		return time;
	}
	
	private void generateProgramVarMap(){
		this.varMap = new HashMap<Data,Set<SVariable>>();
		for(Data d : program.getData().values()){
			Set<SVariable> svSet = new HashSet<SVariable>();
			varMap.put(d, svSet);
			for(List<Instruction> accessors : d.groupAccessors()){
				SVariable sv = new SVariable(d,accessors.get(0),accessors.subList(1, accessors.size()));
				svSet.add(sv);
			}
		}
	}

	public double generateConstraintsAndSolve(){
		for(SMethod sMeth : methVarMap.values()){
			Method meth = sMeth.getMethod();
			double localRuntimeWeight = Configuration.getLocalExclusiveExecutionTime(meth);
			double remoteRuntimeWeight = Configuration.getRemoteExclusiveExecutionTime(meth);

			BooleanVariable li = addSolverVar(sMeth, "L", localRuntimeWeight, L);

			BooleanVariable ri = addSolverVar(sMeth, "R", remoteRuntimeWeight, R);

			BooleanVariable di = addSolverVar(sMeth, "D", 0, D);
			
			BooleanVariable pi = addSolverVar(sMeth, "P", 0, P);
			
			BooleanVariable wi = addSolverVar(sMeth, "W", 0, W);
			
			BooleanVariable qi = addSolverVar(sMeth, "Q", 2*Configuration.latency, Q);

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
		
			//Pi => Wi
			addImpliesConstraint(pi, wi, false);
			
			//Wi => Li
			addImpliesConstraint(wi, li, false);
			
			// Constraint that qi = di OR pi
			addORConstraint(qi, di, pi, false);

			//Constraints involving parent node in the call graph
			Method parentMeth = meth.getCaller();
			if(parentMeth != null){
				SMethod parentSMeth = methVarMap.get(parentMeth); 

				BooleanVariable pLi = addSolverVar(parentSMeth, "L", L);

				BooleanVariable pRi = addSolverVar(parentSMeth, "R", R);

				BooleanVariable pWi = addSolverVar(parentSMeth, "W", W);

				//Constraint that di = lj * ri where j is the parent node of i
				addANDConstraint(di, pLi, ri, false);

				//Constraint that wj => wi where j is the parent node of i
				addImpliesConstraint(pWi, wi, false);

				//Constraint that pi = rj * li where j is the parent node of i
				addANDConstraint(pi, pRi, li, false);
				
//				//Heuritistics about not offloading inside libraries
//				if(Configuration.isLibraryMethod(meth.methName())&&Configuration.isLibraryMethod(parentMeth.methName())){
//					this.addImpliesConstraint(pLi, li, true);
//					this.addImpliesConstraint(pRi, ri, true);
//				}
			}else{
				addImpliesConstraint(ri,di, false);
			}
		}

		for(Set<SVariable> svSet : varMap.values()){
			for(SVariable sv : svSet){
			double dataWeight = Configuration.getTransportTime(sv.getData());

			//Variable constraints to ensure that Vi is set to 1 if at least 
			//one writer and reader of Vi are on different partitions
			HashSet<Integer> varFuncIDs = new HashSet<Integer>();
			ArrayList<Pair<Double,BooleanVariable>> consL1 = new ArrayList<Pair<Double,BooleanVariable>>();
			ArrayList<Pair<Double,BooleanVariable>> consL2 = new ArrayList<Pair<Double,BooleanVariable>>();

			Method varWriter = sv.getWriter().getMethod();
			SMethod varSWriter = methVarMap.get(varWriter);
			varFuncIDs.add(varWriter.getMethodID());
			
			consL1.add(new Pair<Double, BooleanVariable>(1.0, varSWriter.getSolverVar(L)));
			consL2.add(new Pair<Double, BooleanVariable>(1.0, varSWriter.getSolverVar(R)));

			for(Instruction ri : sv.getReaders()){
				Method varReader = ri.getMethod();
				SMethod varSReader = methVarMap.get(varReader);
				if(varFuncIDs.add(varReader.getMethodID())){
					consL1.add(new Pair<Double, BooleanVariable>(1.0, varSReader.getSolverVar(L)));
					consL2.add(new Pair<Double, BooleanVariable>(1.0, varSReader.getSolverVar(R)));
				}
			}

			if(varFuncIDs.size() != 1){
				
				BooleanVariable vi = addSolverVar(sv, "V", dataWeight, V);

				BooleanVariable t1i = addSolverVar(sv, "T1_", 0, T1);

				BooleanVariable t2i = addSolverVar(sv, "T2_", 0, T2);

				consL1.add(new Pair<Double, BooleanVariable>((double) -varFuncIDs.size(), t1i));
				consL2.add(new Pair<Double, BooleanVariable>((double) -varFuncIDs.size(), t2i));
				BooleanConstraint cons1 = new BooleanConstraint(consL1, 0, varFuncIDs.size() - 1);
				BooleanConstraint cons2 = new BooleanConstraint(consL2, 0, varFuncIDs.size() - 1);
				constraints.add(cons1);
				constraints.add(cons2);

				{
					List<BooleanVariable> bList = new ArrayList<BooleanVariable>();
					bList.add(t1i);
					bList.add(t2i);
					bList.add(vi);
					addXORConstraint(bList, false);
				}
				
			}
			}
		}
		
		//Constraints for co-locating methods
		{
			for(Set<Method> SCC : coLocSCCs){
				BooleanVariable si = addSolverVar("S");
				ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
				for(Method sccMeth : SCC){
					SMethod sccSMeth = methVarMap.get(sccMeth);
					consL.add(new Pair<Double, BooleanVariable>(1.0, sccSMeth.getSolverVar(L)));
				}
				consL.add(new Pair<Double, BooleanVariable>(-1.0 * SCC.size(),si));
				BooleanConstraint cons = new BooleanConstraint(consL, 0, 0);
				constraints.add(cons);
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

	@Override
	protected void getMethsToBeOffloaded(double[] ILPSolution) {
		for(SMethod meth : methVarMap.values()){
			if(ILPSolution[meth.getSolverVar(R).id] == 1){
				toBeOffloaded.add(meth.getMethod());
			}
		}
	}

	@Override
	public boolean isStateful() {
		return true;
	}
}
