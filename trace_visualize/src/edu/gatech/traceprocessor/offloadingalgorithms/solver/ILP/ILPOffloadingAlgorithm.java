package edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.gatech.traceprocessor.offloadingalgorithms.solver.BooleanVariable;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.Constraint;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.OffloadingAlgorithm;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.SMethod;
import edu.gatech.traceprocessor.offloadingalgorithms.solver.SVariable;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Pair;
import edu.gatech.traceprocessor.utils.Utils;
import gurobi.GRB;
import gurobi.GRBConstr;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

public abstract class ILPOffloadingAlgorithm extends OffloadingAlgorithm {

	Map<BooleanVariable,Double> objFunction; //Pair of coefficient and BooleanVariable
	static int ID = 0;
	double intFeasTol = 0.0;
	protected final int L = 0;
	protected final int R = 1;
	protected final int D = 2;

	public ILPOffloadingAlgorithm(Program p) {
		super(p);
		objFunction = new HashMap<BooleanVariable, Double>();
	}

	protected BooleanVariable addSolverVar(String varName){
		Utils.printLog("New ILP variable created without check if the variable already exists");
		BooleanVariable b = new BooleanVariable(ID, varName + ID);
//		varDictionary.put(ID, b);
		objFunction.put(b, 0.0);
		ID++;
		return b;
	}
	
	protected BooleanVariable addSolverVar(SMethod extMeth, String varName, double cost, int idx){
		BooleanVariable b = extMeth.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
//			varDictionary.put(ID, b);
			extMeth.addSolverVar(idx,b);
			ID++;
		}
		objFunction.put(b, cost);
		return b;
	}
	
	protected BooleanVariable addSolverVar(SMethod extMeth, String varName, int idx){
		BooleanVariable b = extMeth.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
//			varDictionary.put(ID, b);
			extMeth.addSolverVar(idx,b);
			ID++;
		}
		return b;
	}
	
	protected BooleanVariable addSolverVar(SVariable extVar, String varName, double cost, int idx){
		BooleanVariable b = extVar.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
//			varDictionary.put(ID, b);
			extVar.addSolverVar(idx,b);
			ID++;
		}
		objFunction.put(b, cost);
		return b;
	}
	
	protected BooleanVariable addSolverVar(SVariable extVar, String varName, int idx){
		BooleanVariable b = extVar.getSolverVar(idx);
		if(b == null){
			b = new BooleanVariable(ID, varName + ID);
//			varDictionary.put(ID, b);
			extVar.addSolverVar(idx,b);
			ID++;
		}
		return b;
	}
	
	protected void addFixedValueConstraint(BooleanVariable b, double fixedVal, boolean isHeuristic){
		if(b == null) return;
		ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
		consL.add(new Pair<Double, BooleanVariable>(1.0, b));
		BooleanConstraint cons = new BooleanConstraint(consL, fixedVal, fixedVal);
		if(!isHeuristic)
			constraints.add(cons);
		else
			heuristics.add(cons);
	}

	//b1 => b2
	protected void addImpliesConstraint(BooleanVariable b1, BooleanVariable b2, boolean isHeuristic){
		ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
		consL.add(new Pair<Double, BooleanVariable>(-1.0, b1));
		consL.add(new Pair<Double, BooleanVariable>(1.0, b2));
		BooleanConstraint cons = new BooleanConstraint(consL, 0, 1);
		if(!isHeuristic)
			constraints.add(cons);
		else
			heuristics.add(cons);
	}
	
	//b3 = b1 * b2
	protected void addANDConstraint(BooleanVariable b3, BooleanVariable b1, BooleanVariable b2, boolean isHeuristic){
		ArrayList<Pair<Double,BooleanVariable>> consL1 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL1.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL1.add(new Pair<Double, BooleanVariable>(1.0, b1));
		BooleanConstraint cons1 = new BooleanConstraint(consL1, 0, 1);
		if(!isHeuristic)
			constraints.add(cons1);
		else
			heuristics.add(cons1);

		ArrayList<Pair<Double,BooleanVariable>> consL2 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL2.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL2.add(new Pair<Double, BooleanVariable>(1.0, b2));
		BooleanConstraint cons2 = new BooleanConstraint(consL2, 0, 1);
		if(!isHeuristic)
			constraints.add(cons2);
		else
			heuristics.add(cons2);

		ArrayList<Pair<Double,BooleanVariable>> consL3 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL3.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b1));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b2));
		BooleanConstraint cons3 = new BooleanConstraint(consL3, 0, 1);
		if(!isHeuristic)
			constraints.add(cons3);
		else
			heuristics.add(cons3);
	}
	
	//b3 = b1 OR b2
	protected void addORConstraint(BooleanVariable b3, BooleanVariable b1, BooleanVariable b2, boolean isHeuristic){
		ArrayList<Pair<Double,BooleanVariable>> consL1 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL1.add(new Pair<Double, BooleanVariable>(1.0, b3));
		consL1.add(new Pair<Double, BooleanVariable>(-1.0, b1));
		BooleanConstraint cons1 = new BooleanConstraint(consL1, 0, 1);
		if(!isHeuristic)
			constraints.add(cons1);
		else
			heuristics.add(cons1);

		ArrayList<Pair<Double,BooleanVariable>> consL2 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL2.add(new Pair<Double, BooleanVariable>(1.0, b3));
		consL2.add(new Pair<Double, BooleanVariable>(-1.0, b2));
		BooleanConstraint cons2 = new BooleanConstraint(consL2, 0, 1);
		if(!isHeuristic)
			constraints.add(cons2);
		else
			heuristics.add(cons2);

		ArrayList<Pair<Double,BooleanVariable>> consL3 = new ArrayList<Pair<Double,BooleanVariable>>();
		consL3.add(new Pair<Double, BooleanVariable>(-1.0, b3));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b1));
		consL3.add(new Pair<Double, BooleanVariable>(1.0, b2));
		BooleanConstraint cons3 = new BooleanConstraint(consL3, 0, 1);
		if(!isHeuristic)
			constraints.add(cons3);
		else
			heuristics.add(cons3);
	}
	
	protected void addXORConstraint(List<BooleanVariable> bList, boolean isHeuristic){
		if(bList == null) return;
		if(bList.size() == 0) return;
		ArrayList<Pair<Double,BooleanVariable>> consL = new ArrayList<Pair<Double,BooleanVariable>>();
		for(BooleanVariable b : bList){
			consL.add(new Pair<Double, BooleanVariable>(1.0, b));
		}
		BooleanConstraint cons = new BooleanConstraint(consL, 1, 1);
		if(!isHeuristic)
			constraints.add(cons);
		else
			heuristics.add(cons);
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
		
		Utils.printLogWithTime("ILP constraints generated. Num Variables:" + ID + ", Num Constraints:" + constraints.size()+", Num Heuristics:"+heuristics.size());

		double sol[] = new double[ID];
			Utils.printLogWithTime("Solve the problem with Gurobi.");
			double objValue = runWithGurobi(sol);
			this.getMethsToBeOffloaded(sol);
			for(int i = 0; i < sol.length; i++){
				if(sol[i] != 1.0 && sol[i] != 0.0){
					System.out.println("Not exactly an integer solution: "+sol[i]);
					if(Math.abs(sol[i]-1) <= this.intFeasTol)
						sol[i] = 1;
					else if(Math.abs(sol[i]-0) <= this.intFeasTol)
						sol[i] = 0;
					else
						throw new RuntimeException("Unacceptable soultion: "+sol[i]);
				}
			}
			return objValue;
	}
	
	private double runWithGurobi(double[] sol){
		try {
			GRBEnv env = new GRBEnv("mcc_gurobi.log");
//			env.set(GRB.DoubleParam.NodefileStart, 40);
			this.intFeasTol = env.get(GRB.DoubleParam.IntFeasTol);
			GRBModel model = new GRBModel(env);
			GRBVar[] x = model.addVars(ID, GRB.BINARY);
			model.update();
			GRBLinExpr expr = new GRBLinExpr();
			expr.addTerms(this.getObjFunctionCoefficients(), x);
			model.setObjective(expr, GRB.MINIMIZE);
			int i = 0;
			for(Constraint c : constraints){
				i++;
				BooleanConstraint cons = (BooleanConstraint) c;
				expr = new GRBLinExpr();
				for(Pair<Double, BooleanVariable> p : cons.variables){
					expr.addTerm(p.getFirst(), x[p.getSecond().id]);
				}
				model.addRange(expr, cons.lb, cons.ub, "c"+i);
			}
//			List<GRBConstr> grbHeuristics = new ArrayList<GRBConstr>();
//			int hi = 0;
//			for(Constraint h : heuristics){
//				hi++;
//				BooleanConstraint cons = (BooleanConstraint) h;
//				expr = new GRBLinExpr();
//				for(Pair<Double, BooleanVariable> p : cons.variables){
//					expr.addTerm(p.getFirst(), x[p.getSecond().id]);
//				}
//				grbHeuristics.add(model.addRange(expr, cons.lb, cons.ub, "h"+hi));
//			}
			//release the memory
			constraints.clear();
//			heuristics.clear();
			program.destory();
			System.gc();
//			Utils.printLogWithTime("Start optimizing with heuritics.");
			model.optimize();
//			for(GRBConstr heuri : grbHeuristics)
//				model.remove(heuri);
//			Utils.printLogWithTime("Start optimizing without heuritics.");
//			model.optimize();
			double solution[] = model.get(GRB.DoubleAttr.X, x);
			for(int j = 0; j < solution.length; j++)
				sol[j] = solution[j];
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
			BooleanConstraint cons = (BooleanConstraint) c;
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
		//release the memory
		constraints = null;
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

			double[] localSol = cplex.getValues(lp);
			for(int i = 0 ; i < localSol.length; i ++)
				sol[i] = localSol[i];
			double objVal = cplex.getObjValue();
			cplex.end();
			return objVal;
		} catch (IloException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	protected abstract void getMethsToBeOffloaded(double[] ILPSolution);
	protected abstract double generateConstraintsAndSolve();
}
