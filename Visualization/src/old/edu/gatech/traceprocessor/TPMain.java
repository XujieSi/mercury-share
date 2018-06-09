package old.edu.gatech.traceprocessor;

import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.SFunctionFactory;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.SVariableFactory;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm1;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm2;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm3;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm4;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.ILP.ILPOffloadingAlgorithm5;
import edu.gatech.traceprocessor.utils.Utils;

public class TPMain {

	public static void main(String[] args){
		String tracePath = System.getProperty("mcc.path.trace", null);
		if(tracePath == null){
			Utils.printError("Enter path to trace file");
			System.exit(0);
		}

		String isTraceBinStr = System.getProperty("mcc.input.isBin","true");
		boolean isTraceBin = Boolean.parseBoolean(isTraceBinStr);
		String ifFixConStr = System.getProperty("mcc.fixConcur","true");
		boolean ifFixCon = Boolean.parseBoolean(ifFixConStr);
		String localMethodsPath = System.getProperty("mcc.path.localmethods", null);
		String networkModelPath = System.getProperty("mcc.path.networkModel", null);
		String coLocMethodsPath = System.getProperty("mcc.path.coLocMethods", null);
		String inlineStr = System.getProperty("mcc.inline","true");
		Configuration.inlineOpt = Boolean.parseBoolean(inlineStr);
		
		Configuration.offloadingLimit = Integer.parseInt(System.getProperty("mcc.offloadingLimit", "-1"));

		if((System.getProperty("mcc.verbose", "false")).equalsIgnoreCase("true"))
			Utils.enableVerboseLogging();

		int offloadingAlgo = Integer.parseInt(System.getProperty("mcc.algo", "3"));

		if(networkModelPath != null)
			Configuration.setNetworkModel(networkModelPath);

		SFunctionFactory sFunctionFactory = new SFunctionFactory();
		SVariableFactory sVariableFactory = new SVariableFactory();
		
		TraceProcessor tp;
		
		Utils.printConfig();
		
		switch(offloadingAlgo){
		case 1:
			tp = new TraceProcessor(tracePath,localMethodsPath,coLocMethodsPath,false,false,sFunctionFactory);
			tp.setIfBin(isTraceBin);
			tp.setIfFixCon(ifFixCon);
			tp.parse();
			(new ILPOffloadingAlgorithm1(tp.forest, tp.varTable, tp.coLocFunctions)).optimize();
			Utils.printLog("ILP stateless unidirectional algorithm terminated");
			break;
		case 2:
			tp = new TraceProcessor(tracePath,localMethodsPath,coLocMethodsPath,true,false,sFunctionFactory);
			tp.setIfBin(isTraceBin);
			tp.setIfFixCon(ifFixCon);		
			tp.parse();
			(new ILPOffloadingAlgorithm2(tp.forest, tp.varTable, tp.coLocFunctions)).optimize();
			Utils.printLog("ILP stateless bidirectional algorithm terminated");
			break;
		case 3:
			tp = new TraceProcessor(tracePath,localMethodsPath,coLocMethodsPath,false,true,sFunctionFactory);
			tp.setIfBin(isTraceBin);
			tp.setIfFixCon(ifFixCon);
			tp.parse();
			//(new ILPOffloadingAlgorithm3a(tp.forest, tp.varTable, tp.coLocFunctions)).optimize();
			(new ILPOffloadingAlgorithm3(tp.forest, tp.varTable, tp.coLocFunctions)).optimize();
			Utils.printLog("ILP stateful unidirectional algorithm terminated");
			break;
		case 4:
			tp = new TraceProcessor(tracePath,localMethodsPath,coLocMethodsPath,true,true,sFunctionFactory);
			tp.setIfBin(isTraceBin);
			tp.setIfFixCon(ifFixCon);
			tp.parse();
			//(new ILPOffloadingAlgorithm4a(tp.forest, tp.varTable, tp.coLocFunctions)).optimize();
			(new ILPOffloadingAlgorithm4(tp.forest, tp.varTable, tp.coLocFunctions)).optimize();
			Utils.printLog("ILP stateful bidirectional algorithm terminated");
			break;
		case 5:
			tp = new TraceProcessor(tracePath,localMethodsPath,coLocMethodsPath,false,false,sFunctionFactory);
			tp.setIfBin(isTraceBin);
			tp.setIfFixCon(ifFixCon);
			tp.parse();
			(new ILPOffloadingAlgorithm5(tp.forest, tp.varTable, tp.coLocFunctions)).optimize();
			Utils.printLog("ILP stateless parallel algorithm terminated");
			Utils.printLog("This algorithm has not been fully implemented!");
			break;
		default:
			Utils.printError("Offloading algorithm chosen does not exist");
		}
	}
}
