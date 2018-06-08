package old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import old.edu.gatech.traceprocessor.Function;
import old.edu.gatech.traceprocessor.VarEntry;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.BooleanVariable;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.Constraint;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.OffloadingAlgorithm;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.SFunction;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.SVariable;
import edu.gatech.traceprocessor.utils.Pair;
import edu.gatech.traceprocessor.utils.Utils;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public abstract class ILPOffloadingAlgorithm extends OffloadingAlgorithm {

	//List<Pair<Double,BooleanVariable>> objFunction; //Pair of coefficient and BooleanVariable
	Map<BooleanVariable,Double> objFunction; //Pair of coefficient and BooleanVariable
	static int ID = 0;
	boolean useCplex = false;

	public ILPOffloadingAlgorithm(Function forest, Set<VarEntry> programVarTable, Set<Set<Function>> coLocSCCs) {
		super(forest, programVarTable, coLocSCCs);
		//objFunction = new ArrayList<Pair<Double,BooleanVariable>>();
		objFunction = new HashMap<BooleanVariable, Double>();
	}

	protected BooleanVariable addSolverVar(String varName){
		Utils.printLog("New ILP variable created without check if the variable already exists");
		BooleanVariable b = new BooleanVariable(ID, varName + ID);
		varDictionary.put(ID, b);
	//	objFunction.add(new Pair<Double, BooleanVariable>(0.0, b));
		objFunction.put(b, 0.0);
		ID++;
		return b;
	}
	
	protected BooleanVariable addSolverVar(SFunction extFunc, String varName, double cost, int idx){
		BooleanVariable b = extFunc.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
			varDictionary.put(ID, b);
			extFunc.addSolverVar(idx,b);
			ID++;
		}
		//objFunction.add(new Pair<Double, BooleanVariable>(cost, b));
		objFunction.put(b, cost);
		return b;
	}
	
	protected BooleanVariable addSolverVar(SFunction extFunc, String varName, int idx){
		BooleanVariable b = extFunc.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
			varDictionary.put(ID, b);
			extFunc.addSolverVar(idx,b);
			ID++;
		}
		//objFunction.add(new Pair<Double, BooleanVariable>(0.0, b));
		//objFunction.put(b, 0.0);
		return b;
	}
	
	protected BooleanVariable addSolverVar(SVariable extVar, String varName, double cost, int idx){
		BooleanVariable b = extVar.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
			varDictionary.put(ID, b);
			extVar.addSolverVar(idx,b);
			ID++;
		}
		//objFunction.add(new Pair<Double, BooleanVariable>(cost, b));
		objFunction.put(b, cost);
		return b;
	}
	
	protected BooleanVariable addSolverVar(SVariable extVar, String varName, int idx){
		BooleanVariable b = extVar.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
			varDictionary.put(ID, b);
			extVar.addSolverVar(idx,b);
			ID++;
		}
		//objFunction.add(new Pair<Double, BooleanVariable>(0.0, b));
		//objFunction.put(b, 0.0);
		return b;
	}
	
	protected void addFixedValueConstraint(BooleanVariable b, double fixedVal){
		if(b == null) return;
		ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
		consL.add(new Pair<Double, BooleanVariable>(1.0, b));
		ILPConstraint cons = new ILPConstraint(consL, fixedVal, fixedVal);
		constraints.add(cons);
	}

	//b1 => b2
	protected void addImpliesConstraint(BooleanVariable b1, BooleanVariable b2){
		ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
		consL.add(new Pair<Double, BooleanVariable>(-1.0, b1));
		consL.add(new Pair<Double, BooleanVariable>(1.0, b2));
		ILPConstraint cons = new ILPConstraint(consL, 0, 1);
		constraints.add(cons);
	}
	
	//b3 = b1 * b2
	protected void addANDConstraint(BooleanVariable b3, BooleanVariable b1, BooleanVariable b2){
		ArrayList<Pair<Double,BooleanVariable>> consL1 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL1.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL1.add(new Pair<Double, BooleanVariable>(1.0, b1));
		ILPConstraint cons1 = new ILPConstraint(consL1, 0, 1);
		constraints.add(cons1);

		ArrayList<Pair<Double,BooleanVariable>> consL2 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL2.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL2.add(new Pair<Double, BooleanVariable>(1.0, b2));
		ILPConstraint cons2 = new ILPConstraint(consL2, 0, 1);
		constraints.add(cons2);

		ArrayList<Pair<Double,BooleanVariable>> consL3 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL3.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b1));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b2));
		ILPConstraint cons3 = new ILPConstraint(consL3, 0, 1);
		constraints.add(cons3);
	}
	
	//b3 = b1 OR b2
	protected void addORConstraint(BooleanVariable b3, BooleanVariable b1, BooleanVariable b2){
		ArrayList<Pair<Double,BooleanVariable>> consL1 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL1.add(new Pair<Double, BooleanVariable>(1.0, b3));
		consL1.add(new Pair<Double, BooleanVariable>(-1.0, b1));
		ILPConstraint cons1 = new ILPConstraint(consL1, 0, 1);
		constraints.add(cons1);

		ArrayList<Pair<Double,BooleanVariable>> consL2 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL2.add(new Pair<Double, BooleanVariable>(1.0, b3));
		consL2.add(new Pair<Double, BooleanVariable>(-1.0, b2));
		ILPConstraint cons2 = new ILPConstraint(consL2, 0, 1);
		constraints.add(cons2);

		ArrayList<Pair<Double,BooleanVariable>> consL3 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL3.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b1));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b2));
		ILPConstraint cons3 = new ILPConstraint(consL3, 0, 1);
		constraints.add(cons3);
	}
	
	protected void addXORConstraint(List<BooleanVariable> bList){
		if(bList == null) return;
		if(bList.size() == 0) return;
		ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
		for(BooleanVariable b : bList){
			consL.add(new Pair<Double, BooleanVariable>(1.0, b));
		}
		ILPConstraint cons = new ILPConstraint(consL, 1, 1);
		constraints.add(cons);
	}
	
	protected double[] getObjFunctionCoefficients(){
		double[] objCoeffs = new double[objFunction.size()];
	//	for(Pair<Double,BooleanVariable> p : objFunction){
	//		objCoeffs[p.getSecond().id] = p.getFirst(); 
	//	}
		for(BooleanVariable b : objFunction.keySet()){
			objCoeffs[b.id] = objFunction.get(b); 
		}
		return objCoeffs;
	}
	
	/*
	 * The parameter choice dictates the form of the objective function.
	 * If choice = 1; obj function has form: min(linear expression)
	 * If choice = 2; obj function has form: min(abs(linear expression))
	 */
	protected double runSolver(int choice){
		if(choice != 1 && choice != 2){
			Utils.printError("Choose between values 1 and 2 for selecting the objective function");
			return -1;
		}
		
		Utils.printLogWithTime("ILP constraints generated. Num Variables:" + varDictionary.size() + ", Num Constraints:" + constraints.size());

		double sol[] = new double[ID];
		if(this.useCplex){
			Utils.printLogWithTime("Solve the problem with CPLEX.");
			double objValue = this.runWithCplex(choice, sol, null);
			for(double d : sol)
				if(d != 1.0 && d != 0)
					throw new RuntimeException("The solution is not an ILP solution.");
			Utils.printLogWithTime("Display offloading scheme:");
			this.getFuncsToBeOffloaded(sol);

			return objValue;
		}else{
			Utils.printLogWithTime("Solve the problem with Gurobi.");
			double objValue = runWithGurobi(sol);
			this.getFuncsToBeOffloaded(sol);
			for(double d : sol)
				if(d != 1.0 && d != 0)
					throw new RuntimeException("The solution is not an ILP solution.");
//			Utils.printLogWithTime("Let us use cplex to check whether the result of Gurobi makes sense.");
//			double newSol[] = new double[ID];
//			this.runWithCplex(choice, newSol, sol);
			return objValue;
		}
	}
	
	private double runWithGurobi(double[] sol){
		try {
			GRBEnv env = new GRBEnv("mcc_gurobi.log");
			env.set(GRB.DoubleParam.NodefileStart, 40);
			GRBModel model = new GRBModel(env);
			GRBVar[] x = model.addVars(ID, GRB.BINARY);
			model.update();
			GRBLinExpr expr = new GRBLinExpr();
			expr.addTerms(this.getObjFunctionCoefficients(), x);
			model.setObjective(expr, GRB.MINIMIZE);
			int i = 0;
			for(Constraint c : constraints){
				i++;
				ILPConstraint cons = (ILPConstraint) c;
				expr = new GRBLinExpr();
				for(Pair<Double, BooleanVariable> p : cons.variables){
					expr.addTerm(p.getFirst(), x[p.getSecond().id]);
				}
				model.addRange(expr, cons.lb, cons.ub, "c"+i);
			}
			model.optimize();
			for(int j = 0 ; j < x.length; j++)
				sol[j] = x[j].get(GRB.DoubleAttr.X);
		    return model.get(GRB.DoubleAttr.ObjVal);
		} catch (GRBException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private double runWithCplex(int choice,double[] sol, double startSol[]){
		double[] lb = new double[constraints.size()];
		double[] ub = new double[constraints.size()];
		double[][] valH = new double[constraints.size()][];
		int[][] indH = new int[constraints.size()][];

		int idx = 0;
		for(Constraint c : constraints){
			ILPConstraint cons = (ILPConstraint) c;
			lb[idx] = cons.lb;
			ub[idx] = cons.ub;
			valH[idx] = new double[cons.variables.size()];
			indH[idx] = new int[cons.variables.size()];
			int idx2 = 0;
			for(Pair<Double, BooleanVariable> p : cons.variables){
				valH[idx][idx2] = p.getFirst();
				indH[idx][idx2] = p.getSecond().id;
				idx2++;
			}
			idx++;
		}
		try {
			IloCplex cplex = new IloCplex();
			IloLPMatrix lp = cplex.addLPMatrix();

			// Add empty corresponding to new variables columns to lp;
			//Can use ID to get the number of variables since ID starts from zero,
			//and is incremented everytime a new boolean variable is created. 
			IloNumVar[] x = cplex.numVarArray(cplex.columnArray(lp, ID),
					0,1,IloNumVarType.Bool);
			lp.addRows(lb, ub, indH, valH);

			if(choice == 1)
				cplex.addMinimize(cplex.scalProd(x, getObjFunctionCoefficients()));
			else if(choice == 2)
				cplex.addMinimize(cplex.abs(cplex.scalProd(x, getObjFunctionCoefficients())));

			cplex.setOut(System.out);
			
			if(startSol != null)
				cplex.addMIPStart(x, startSol);

			cplex.solve();
			cplex.writeSolution("solution.sol");
			Utils.printLogWithTime("ILP solver terminated with status: "+cplex.getStatus());
			Utils.printLog(cplex.getCplexStatus().toString());

			//Code to debug cplex solution when model declared as infeasible	
			/*	{
				if (cplex.solve())
				{
					Utils.printLog("Model Feasible");
					Utils.printLog("Solution status = " + cplex.getStatus());
					Utils.printLog("Solution value  = " + cplex.getObjValue());
					double[] X = cplex.getValues(lp);
					//    for (int j = 0; j < X.length; ++j)
					//    Utils.printLog("Variable Name:"+lp.getNumVar(j).getName()+"; Value = " + X[j]);
				}
				else
				{
					Utils.printLog("Solution status = " + cplex.getStatus());
					Utils.printLog("Model Infeasible, Calling CONFLICT REFINER");
					IloRange[] rng = lp.getRanges();
					int numVars2 = 0;

					//calculate the number of non-boolean variables
					for (int c1 = 0; c1 < lp.getNumVars().length; c1++)
						if (lp.getNumVar(c1).getType() != IloNumVarType.Bool)
							numVars2++;
					//find the number of SOSs in the model
					int numSOS = cplex.getNSOSs();
					Utils.printLog("Number of SOSs=" + numSOS);

					int numConstraints = rng.length+2*numVars2+numSOS;
					IloConstraint[] constraints = new IloConstraint[numConstraints];
					for (int c1 = 0; c1 < rng.length; c1++)
					{
						constraints[c1] = rng[c1];
					}
					int numVarCounter = 0;
					//add variable bounds to the constraints array
					for(int c1=0;c1<lp.getNumVars().length;c1++)
					{
						if (lp.getNumVar(c1).getType() != IloNumVarType.Bool)
						{
							constraints[rng.length + 2*numVarCounter] = cplex.addLe(lp.getNumVar(c1).getLB(), lp.getNumVar(c1));
							constraints[rng.length + 2 * numVarCounter].setName(lp.getNumVar(c1).toString() + "_LB");
							constraints[rng.length + 2*numVarCounter + 1] = cplex.addGe(lp.getNumVar(c1).getUB(), lp.getNumVar(c1));
							constraints[rng.length + 2 * numVarCounter + 1].setName(lp.getNumVar(c1).toString() + "_UB");
							numVarCounter++;
						}
					}
					//add SOSs to the constraints array
					if (numSOS > 0)
					{
						int s1Counter = 0;
						Iterator s1 = cplex.SOS1iterator();
						while (s1.hasNext())
						{
							IloSOS1 cur = (IloSOS1)s1.next();
							Utils.printLog(cur.toString());
							constraints[rng.length + numVars2 * 2 + s1Counter] = (IloConstraint)cur;
							s1Counter++;
						}
						int s2Counter = 0;
						Iterator s2 = cplex.SOS2iterator();
						while (s2.hasNext())
						{
							IloSOS2 cur = (IloSOS2)s2.next();
							Utils.printLog(cur.toString());
							constraints[rng.length + numVars2 * 2 + s1Counter + s2Counter] = (IloConstraint)cur;
							s2Counter++;
						}
					}
					double[] prefs = new double[constraints.length];
					for (int c1 = 0; c1 < constraints.length; c1++)
					{
						Utils.printLog(constraints[c1].toString());
						prefs[c1] = 1.0;//change it per your requirements
					}
					if (cplex.refineConflict(constraints, prefs))
					{
						Utils.printLog("Conflict Refinement process finished: Printing Conflicts");
						IloCplex.ConflictStatus[] conflict = cplex.getConflict(constraints);
						int numConConflicts = 0;
						int numBoundConflicts = 0;
						int numSOSConflicts = 0;
						for (int c2 = 0; c2 < constraints.length; c2++)
						{
							if (conflict[c2] == IloCplex.ConflictStatus.Member)
							{
								Utils.printLog("  Proved  : " + constraints[c2]);
								if (c2 < rng.length)
									numConConflicts++;
								else if (c2 < rng.length + 2*numVars2)
									numBoundConflicts++;
								else
									numSOSConflicts++;

							}
							else if (conflict[c2] == IloCplex.ConflictStatus.PossibleMember)
							{
								Utils.printLog("  Possible  : " + constraints[c2]);
								if (c2 < rng.length)
									numConConflicts++;
								else if (c2 < rng.length + 2*numVars2)
									numBoundConflicts++;
								else
									numSOSConflicts++;
							}
						}
						Utils.printLog("Conflict Summary:");
						Utils.printLog("  Constraint conflicts     = " + numConConflicts);
						Utils.printLog("  Variable Bound conflicts = " + numBoundConflicts);
						Utils.printLog("  SOS conflicts            = " + numSOSConflicts);
					}
					else
					{
						Utils.printLog("Conflict could not be refined"); 
					}
					Utils.printLog("Calling FEASOPT");
					// cplex.SetParam(Cplex.IntParam.FeasOptMode, 0);//change per feasopt requirements
					// Relax contraints only, modify if variable bound relaxation is required 
					double[] lb_pref = new double[rng.length];
					double[] ub_pref = new double[rng.length];
					for (int c1 = 0; c1 < rng.length; c1++)
					{
						lb_pref[c1] = 1.0;//change it per your requirements
						ub_pref[c1] = 1.0;//change it per your requirements
					}
					if (cplex.feasOpt(rng, lb_pref, ub_pref))
					{
						Utils.printLog("Finished Feasopt");
						double[] infeas = cplex.getInfeasibilities(rng);
						//Print bound changes
						Utils.printLog("Suggested Bound changes:"); 
						for (int c3 = 0; c3 < infeas.length; c3++)
							if(infeas[c3]!=0)
								Utils.printLog("  "+rng[c3] + " : Change=" + infeas[c3]);
						Utils.printLog("Relaxed Model's obj value=" + cplex.getObjValue());
						Utils.printLog("Relaxed Model's solution status:" + cplex.getCplexStatus());
						double[] X = cplex.getValues(lp);
						for (int j = 0; j < X.length; ++j)
							Utils.printLog("Relaxed Model's Variable Name:"+lp.getNumVar(j).getName()+"; Value = " + X[j]);
					}
					else
					{
						Utils.printLog("FeasOpt failed- Could not repair infeasibilities");
					}
				}
				cplex.end();
			}*/


			double[] localSol = cplex.getValues(lp);
			for(int i = 0 ; i < localSol.length; i ++)
				sol[i] = localSol[i];
			//		    getFuncsToBeOffloaded(sol);
			double objVal = cplex.getObjValue();
			cplex.end();
			return objVal;
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	protected abstract void getFuncsToBeOffloaded(double[] ILPSolution);
	protected abstract double generateConstraintsAndSolve();
}
